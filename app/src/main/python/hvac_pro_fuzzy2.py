# -*- coding: utf-8 -*-
"""
HVAC Fuzzy Diagnostic (sin alta, sin subenfriamiento) - v4 (con altitud + fans OK)
Chaquopy-ready (Android):
- NO usa ipywidgets / Jupyter
- Lee entradas desde ClaseDatosPythonJava (Java static fields)
- Escribe resultados de vuelta a ClaseDatosPythonJava:
    diagnostico (str), nivel_carga (int), tsat_evap_c (float), sh_c (float)
"""

import numpy as np
import skfuzzy as fuzz
from skfuzzy import control as ctrl
from dataclasses import dataclass, field
from typing import Dict, Any, List, Tuple, Optional

# Chaquopy Java bridge
from java import jclass


# ============================================================
# Tablas P-T (aproximadas) succión psig -> Tsat °C
# ============================================================
PT_TABLES = {
    "R410A": [
        (60,  -13.0), (70,  -10.0), (80,   -6.5), (90,   -3.0),
        (100,  -0.5), (110,   2.0), (120,   4.5), (130,   7.0),
        (140,   9.5), (150,  12.0), (160,  14.0), (170,  16.0),
    ],
    "R22": [
        (30,  -11.0), (40,   -6.5), (50,   -1.5), (58,    0.0),
        (65,    3.0), (70,    5.0), (75,    7.0), (80,    9.0),
        (85,   11.0), (90,   13.0), (100,  16.0), (110,  19.0),
        (120,  22.0),
    ],
    "R32": [
        (60,  -11.0), (70,   -8.0), (80,   -4.5), (90,   -1.5),
        (100,   1.0), (110,   3.5), (120,   6.0), (130,   8.5),
        (140,  11.0), (150,  13.5), (160,  15.5), (170,  17.5),
        (180,  19.5),
    ],
}


def tsat_from_suction_psig(refrigerant: str, psig: float) -> Tuple[Optional[float], List[str]]:
    warnings: List[str] = []
    ref = (refrigerant or "").strip().upper()

    if ref not in PT_TABLES:
        warnings.append(f"Refrigerante '{ref}' no soportado. Usa R410A, R22 o R32.")
        return None, warnings

    table = PT_TABLES[ref]
    xs = [p for p, _ in table]
    ys = [t for _, t in table]

    if psig < xs[0] or psig > xs[-1]:
        warnings.append(
            f"Presión de succión {psig:.1f} psig fuera del rango de tabla {ref} "
            f"({xs[0]}..{xs[-1]}). Tsat será extrapolada (puede ser imprecisa)."
        )

    if psig <= xs[0]:
        x0, y0 = xs[0], ys[0]
        x1, y1 = xs[1], ys[1]
    elif psig >= xs[-1]:
        x0, y0 = xs[-2], ys[-2]
        x1, y1 = xs[-1], ys[-1]
    else:
        x0 = y0 = x1 = y1 = None
        for i in range(len(xs) - 1):
            if xs[i] <= psig <= xs[i + 1]:
                x0, y0 = xs[i], ys[i]
                x1, y1 = xs[i + 1], ys[i + 1]
                break

    tsat = y0 + (psig - x0) * (y1 - y0) / (x1 - x0)
    return float(tsat), warnings


# ============================================================
# Altitud -> presión atmosférica (psia) (atm estándar aprox)
# ============================================================
def patm_psia_from_alt_m(alt_m: float) -> float:
    """
    Aproximación atmósfera estándar (troposfera) hasta ~11km.
    Devuelve presión atmosférica en psia.
    """
    alt_m = max(0.0, float(alt_m))
    # Constantes ISA
    P0 = 101325.0     # Pa
    T0 = 288.15       # K
    L = 0.0065        # K/m
    g = 9.80665       # m/s^2
    R = 287.05287     # J/(kg·K)

    # P = P0*(1 - L*h/T0)^(g/(R*L))
    x = 1.0 - (L * alt_m) / T0
    x = max(1e-6, x)
    P = P0 * (x ** (g / (R * L)))  # Pa

    # Pa -> psi
    psi = P * 0.0001450377377
    return float(psi)


def psig_local_to_sea_level_equiv(psig_local: float, alt_m: float) -> Tuple[float, List[str]]:
    """
    Convierte psig medido a una psig equivalente a nivel del mar para usar tablas,
    manteniendo el MISMO psia del sistema.
      psia_local = psig_local + patm_local
      psig_equiv = psia_local - patm_sea
    """
    warnings: List[str] = []
    psig_local = float(psig_local)
    patm_local = patm_psia_from_alt_m(alt_m)
    patm_sea = 14.6959  # psia

    psia = psig_local + patm_local
    psig_equiv = psia - patm_sea

    # Si altitud es alta, psig_equiv será un poco menor que psig_local (esperado)
    if alt_m > 500:
        warnings.append(
            f"Ajuste por altitud: patm_local≈{patm_local:.2f} psia (alt {alt_m:.0f} m). "
            f"PSI succión local {psig_local:.1f} -> equivalente nivel mar {psig_equiv:.1f}."
        )

    return float(psig_equiv), warnings


# ============================================================
# Modelo difuso (usa %RLA, Tevap IR, ΔTcond_ext + síntomas)
# ============================================================
def build_fuzzy_hvac_model_no_high_side() -> ctrl.ControlSystemSimulation:
    SH = ctrl.Antecedent(np.arange(0, 31, 1), 'SH')                 # °C
    SUC = ctrl.Antecedent(np.arange(20, 181, 1), 'SUC')             # psig (equiv nivel mar si aplica)
    dT_air = ctrl.Antecedent(np.arange(0, 21, 1), 'dT_air')         # °C
    pctRLA = ctrl.Antecedent(np.arange(0, 161, 1), 'pctRLA')        # %
    Tevap = ctrl.Antecedent(np.arange(-20, 31, 1), 'Tevap')         # °C
    dTcond_ext = ctrl.Antecedent(np.arange(0, 51, 1), 'dTcond_ext') # °C

    # síntomas (0..100)
    low_cooling = ctrl.Antecedent(np.arange(0, 101, 1), 'low_cooling')
    ice = ctrl.Antecedent(np.arange(0, 101, 1), 'ice')
    dirty_cond = ctrl.Antecedent(np.arange(0, 101, 1), 'dirty_cond')
    low_airflow = ctrl.Antecedent(np.arange(0, 101, 1), 'low_airflow')
    comp_hot = ctrl.Antecedent(np.arange(0, 101, 1), 'comp_hot')
    frost_restriction = ctrl.Antecedent(np.arange(0, 101, 1), 'frost_restriction')

    # NUEVOS síntomas: "OK" (0/50/100) -> no/maybe/yes
    fan_cond_ok = ctrl.Antecedent(np.arange(0, 101, 1), 'fan_cond_ok')
    fan_evap_ok = ctrl.Antecedent(np.arange(0, 101, 1), 'fan_evap_ok')

    # salidas
    low_charge = ctrl.Consequent(np.arange(0, 101, 1), 'low_charge')
    overcharge = ctrl.Consequent(np.arange(0, 101, 1), 'overcharge')
    restriction = ctrl.Consequent(np.arange(0, 101, 1), 'restriction')
    airflow_issue = ctrl.Consequent(np.arange(0, 101, 1), 'airflow_issue')

    # existente: condensador sucio / mala disipación
    cond_fouled = ctrl.Consequent(np.arange(0, 101, 1), 'cond_fouled')

    # NUEVO: ventilación exterior deficiente (ventilador/obstrucción/recirculación)
    cond_fan_issue = ctrl.Consequent(np.arange(0, 101, 1), 'cond_fan_issue')

    # ================= Memberships =================
    SH['low'] = fuzz.trapmf(SH.universe, [0, 0, 4, 8])
    SH['normal'] = fuzz.trimf(SH.universe, [6, 10, 14])
    SH['high'] = fuzz.trapmf(SH.universe, [12, 15, 30, 30])

    SUC['very_low'] = fuzz.trapmf(SUC.universe, [20, 20, 40, 60])
    SUC['low'] = fuzz.trapmf(SUC.universe, [50, 65, 85, 105])
    SUC['normal'] = fuzz.trimf(SUC.universe, [90, 115, 140])
    SUC['high'] = fuzz.trapmf(SUC.universe, [130, 150, 180, 180])

    dT_air['low'] = fuzz.trapmf(dT_air.universe, [0, 0, 4, 7])
    dT_air['normal'] = fuzz.trimf(dT_air.universe, [6, 10, 14])
    dT_air['high'] = fuzz.trapmf(dT_air.universe, [12, 15, 20, 20])

    pctRLA['low'] = fuzz.trapmf(pctRLA.universe, [0, 0, 40, 60])
    pctRLA['normal'] = fuzz.trimf(pctRLA.universe, [60, 80, 95])
    pctRLA['high'] = fuzz.trimf(pctRLA.universe, [92, 110, 125])
    pctRLA['over'] = fuzz.trapmf(pctRLA.universe, [115, 130, 160, 160])

    Tevap['very_cold'] = fuzz.trapmf(Tevap.universe, [-20, -20, -5, 0])
    Tevap['cold']      = fuzz.trimf(Tevap.universe, [-2, 6, 12])
    Tevap['warm']      = fuzz.trapmf(Tevap.universe, [10, 16, 30, 30])

    # Ajuste leve recomendado para evitar que "mala disipación" se dispare tan pronto:
    dTcond_ext['low']    = fuzz.trapmf(dTcond_ext.universe, [0, 0, 8, 12])
    dTcond_ext['normal'] = fuzz.trimf(dTcond_ext.universe, [10, 18, 26])
    dTcond_ext['high']   = fuzz.trapmf(dTcond_ext.universe, [24, 30, 50, 50])

    # Síntomas (presentes: no/maybe/yes)
    for s in [low_cooling, ice, dirty_cond, low_airflow, comp_hot, frost_restriction]:
        s['no'] = fuzz.trapmf(s.universe, [0, 0, 20, 45])
        s['maybe'] = fuzz.trimf(s.universe, [30, 50, 70])
        s['yes'] = fuzz.trapmf(s.universe, [60, 80, 100, 100])

    # "OK" (fan) -> no/maybe/yes
    # 0 => NO funciona, 50 => medio, 100 => Sí funciona
    for s in [fan_cond_ok, fan_evap_ok]:
        s['no'] = fuzz.trapmf(s.universe, [0, 0, 20, 45])
        s['maybe'] = fuzz.trimf(s.universe, [30, 50, 70])
        s['yes'] = fuzz.trapmf(s.universe, [60, 80, 100, 100])

    for out in [low_charge, overcharge, restriction, airflow_issue, cond_fouled, cond_fan_issue]:
        out['low'] = fuzz.trapmf(out.universe, [0, 0, 20, 40])
        out['med'] = fuzz.trimf(out.universe, [30, 50, 70])
        out['high'] = fuzz.trapmf(out.universe, [60, 80, 100, 100])

    # ================= Rules =================
    rules = []

    # Baja carga
    rules += [
        ctrl.Rule(low_cooling['yes'] & SUC['low'] & SH['high'], low_charge['high']),
        ctrl.Rule(low_cooling['yes'] & SUC['very_low'] & SH['high'], low_charge['high']),
        ctrl.Rule(SUC['low'] & SH['high'], low_charge['med']),
        ctrl.Rule(pctRLA['low'] & SUC['low'] & SH['high'], low_charge['high']),
        ctrl.Rule(Tevap['warm'] & SH['high'] & SUC['low'], low_charge['high']),
        ctrl.Rule(dTcond_ext['normal'] & SH['high'] & SUC['low'], low_charge['med']),
    ]

    # Restricción
    rules += [
        ctrl.Rule(frost_restriction['yes'] & SH['high'] & SUC['very_low'], restriction['high']),
        ctrl.Rule(SH['high'] & SUC['very_low'], restriction['med']),
        ctrl.Rule(pctRLA['over'] & SH['high'] & SUC['very_low'], restriction['high']),
        ctrl.Rule(Tevap['very_cold'] & SUC['very_low'] & SH['high'], restriction['med']),
    ]

    # Bajo flujo interior (ahora también con turbina)
    rules += [
        ctrl.Rule(low_airflow['yes'] & ice['yes'] & SH['low'], airflow_issue['high']),
        ctrl.Rule(low_airflow['yes'] & dT_air['low'], airflow_issue['high']),
        ctrl.Rule(ice['yes'] & SH['low'], airflow_issue['med']),
        ctrl.Rule(Tevap['very_cold'] & ice['yes'], airflow_issue['high']),
        ctrl.Rule(pctRLA['low'] & low_airflow['yes'], airflow_issue['med']),

        # NUEVO: turbina evaporador NO OK -> airflow_issue
        ctrl.Rule(fan_evap_ok['no'] & (dT_air['low'] | ice['yes']), airflow_issue['high']),
        ctrl.Rule(fan_evap_ok['no'] & low_airflow['maybe'], airflow_issue['high']),
        ctrl.Rule(fan_evap_ok['maybe'] & (dT_air['low'] | low_airflow['yes']), airflow_issue['med']),
    ]

    # Condensador sucio (más "literal": depende de dirty_cond)
    rules += [
        ctrl.Rule(dirty_cond['yes'] & low_cooling['yes'], cond_fouled['med']),
        ctrl.Rule(dirty_cond['yes'] & comp_hot['yes'], cond_fouled['med']),
        ctrl.Rule(dirty_cond['yes'] & (pctRLA['high'] | pctRLA['over']), cond_fouled['high']),
        ctrl.Rule(dTcond_ext['high'] & dirty_cond['yes'], cond_fouled['high']),
        ctrl.Rule(dTcond_ext['normal'] & dirty_cond['yes'], cond_fouled['med']),
    ]

    # NUEVO: ventilación exterior deficiente (ventilador/obstrucción/recirculación)
    rules += [
        ctrl.Rule(fan_cond_ok['no'] & dTcond_ext['high'] & comp_hot['yes'], cond_fan_issue['high']),
        ctrl.Rule(fan_cond_ok['no'] & dTcond_ext['high'] & (pctRLA['high'] | pctRLA['over']), cond_fan_issue['high']),
        ctrl.Rule(fan_cond_ok['maybe'] & dTcond_ext['high'], cond_fan_issue['med']),
        ctrl.Rule(fan_cond_ok['no'] & low_cooling['yes'], cond_fan_issue['med']),
    ]

    # Exceso de refrigerante (sobrecarga)
    rules += [
        ctrl.Rule(SH['low'] & SUC['high'] & (pctRLA['high'] | pctRLA['over']), overcharge['high']),
        ctrl.Rule(low_cooling['yes'] & SH['low'] & SUC['high'] & pctRLA['over'], overcharge['high']),
        ctrl.Rule(SH['low'] & (pctRLA['high'] | pctRLA['over']) & dTcond_ext['high'], overcharge['high']),
        ctrl.Rule(SH['low'] & dTcond_ext['high'] & comp_hot['yes'], overcharge['high']),
        ctrl.Rule(SH['low'] & dTcond_ext['high'], overcharge['med']),

        # NUEVO: si ventilador OK y condensador NO sucio, pero señales de sobrecarga -> subir sobrecarga
        ctrl.Rule(SH['low'] & SUC['high'] & pctRLA['over'] & fan_cond_ok['yes'] & dirty_cond['no'], overcharge['high']),
        ctrl.Rule(SH['low'] & SUC['high'] & (pctRLA['high'] | pctRLA['over']) & fan_cond_ok['yes'] & dirty_cond['no'], overcharge['high']),
        ctrl.Rule(SH['low'] & dTcond_ext['high'] & fan_cond_ok['yes'] & dirty_cond['no'], overcharge['med']),
    ]

    system = ctrl.ControlSystem(rules)
    return ctrl.ControlSystemSimulation(system)


# ============================================================
# Diagnóstico
# ============================================================
@dataclass
class StepResult:
    tsat_evap: Optional[float]
    sh: Optional[float]
    scores: Dict[str, float]
    top: List[Tuple[str, float]]
    warnings: List[str] = field(default_factory=list)
    suggested_actions: List[str] = field(default_factory=list)


def clamp(x: float, lo: float, hi: float) -> float:
    return max(lo, min(hi, x))


def compute_tsat_and_sh(state: Dict[str, Any]) -> Tuple[Optional[float], Optional[float], List[str]]:
    warnings: List[str] = []
    ref = (state.get("refrigerant") or "R410A").strip().upper()

    psig_local = state.get("suction_psi")
    slt_c = state.get("suction_line_temp_c")

    if psig_local is None:
        warnings.append("Falta presión de succión (PSI).")
        return None, None, warnings

    # Ajuste por altitud (para tablas)
    alt_m = float(state.get("altitude_m") or 0.0)
    psig_for_table, w_alt = psig_local_to_sea_level_equiv(float(psig_local), alt_m)
    warnings += w_alt

    tsat, w = tsat_from_suction_psig(ref, float(psig_for_table))
    warnings += w
    if tsat is None:
        return None, None, warnings

    if slt_c is None:
        warnings.append("Falta temp. de línea de succión (°C) para calcular SH.")
        return tsat, None, warnings

    sh = float(slt_c) - float(tsat)
    if sh < -2 or sh > 35:
        warnings.append(f"SH ({sh:.1f} °C) fuera de rango típico. Revisa medición o tabla/PSI.")
    return tsat, sh, warnings


def diagnose(state: Dict[str, Any], sim: ctrl.ControlSystemSimulation) -> Tuple[StepResult, float, float, float]:
    try:
        sim.reset()
    except Exception:
        pass

    warnings: List[str] = []

    tsat, sh, w = compute_tsat_and_sh(state)
    warnings += w

    # OJO: para el fuzzy usamos SUC también (psig equival nivel mar)
    suction_local = float(state.get("suction_psi") or 110.0)
    alt_m = float(state.get("altitude_m") or 0.0)
    suction_for_fuzzy, w_alt2 = psig_local_to_sea_level_equiv(suction_local, alt_m)
    warnings += w_alt2

    dt_air = float(state.get("deltaT_air_c") or 10.0)

    amps = float(state.get("comp_current_a") or 8.0)
    rla = float(state.get("comp_rla_a") or 10.0)
    if rla <= 0:
        rla = 10.0
        warnings.append("RLA inválido. Usando 10A por defecto (solo para evitar error).")

    pct_rla_val = (amps / rla) * 100.0
    pct_rla_val = max(0.0, min(160.0, pct_rla_val))

    tevap = float(state.get("temp_evap_c") or 6.0)
    tcond = float(state.get("temp_cond_c") or 45.0)
    tamb  = float(state.get("temp_amb_ext_c") or 35.0)

    dtcond_ext_val = max(0.0, float(tcond) - float(tamb))
    if dtcond_ext_val > 50:
        warnings.append("ΔTcond_ext muy alta (Tcond - Tamb). Revisa medición IR o punto de lectura.")
    if tcond < tamb:
        warnings.append("Tcond < Tamb_ext: posible error IR (superficie reflejante) o lectura incorrecta.")

    sym = state.get("symptoms", {}) or {}
    lc = float(sym.get("low_cooling", 50.0))
    ic = float(sym.get("ice", 50.0))
    dc = float(sym.get("dirty_cond", 50.0))
    la = float(sym.get("low_airflow", 50.0))
    ch = float(sym.get("comp_hot", 50.0))
    fr = float(sym.get("frost_restriction", 50.0))

    # NUEVOS: OK (si/medio/no) -> 100/50/0
    fco = float(sym.get("fan_cond_ok", 100.0))  # default "Sí"
    feo = float(sym.get("fan_evap_ok", 100.0))  # default "Sí"

    sh_in = 10.0 if sh is None else float(sh)

    inputs = {
        "SH": clamp(sh_in, 0, 30),                       # clamp para fuzzy
        "SUC": clamp(suction_for_fuzzy, 20, 180),
        "dT_air": clamp(dt_air, 0, 20),
        "pctRLA": clamp(pct_rla_val, 0, 160),
        "Tevap": clamp(tevap, -20, 30),
        "dTcond_ext": clamp(dtcond_ext_val, 0, 50),

        "low_cooling": clamp(lc, 0, 100),
        "ice": clamp(ic, 0, 100),
        "dirty_cond": clamp(dc, 0, 100),
        "low_airflow": clamp(la, 0, 100),
        "comp_hot": clamp(ch, 0, 100),
        "frost_restriction": clamp(fr, 0, 100),

        "fan_cond_ok": clamp(fco, 0, 100),
        "fan_evap_ok": clamp(feo, 0, 100),
    }

    for k, v in inputs.items():
        sim.input[k] = v

    sim.compute()
    out = sim.output

    scores = {
        "Baja carga (posible fuga)": float(out.get("low_charge", 0.0)),
        "Exceso de refrigerante (sobrecarga)": float(out.get("overcharge", 0.0)),
        "Restricción (filtro/TXV/capilar)": float(out.get("restriction", 0.0)),
        "Bajo flujo de aire interior": float(out.get("airflow_issue", 0.0)),
        "Condensador sucio / serpentin exterior sucio": float(out.get("cond_fouled", 0.0)),
        "Ventilación exterior deficiente (ventilador/obstrucción/recirculación)": float(out.get("cond_fan_issue", 0.0)),
    }

    top = sorted(scores.items(), key=lambda kv: kv[1], reverse=True)[:3]
    top_score = max(scores.values()) if scores else 0.0

    # ---- TODO OK ----
    ok_msg = None
    sh_ok = (sh is not None) and (4.0 <= sh <= 16.0)
    dt_ok = (8.0 <= dt_air <= 14.0)
    pct_ok = (60.0 <= pct_rla_val <= 100.0)
    tev_ok = (-2.0 <= tevap <= 14.0)
    dtcond_ok = (10.0 <= dtcond_ext_val <= 26.0)
    symptoms_ok = (lc <= 50 and ic <= 50 and dc <= 50 and la <= 50 and ch <= 50 and fr <= 50 and fco >= 50 and feo >= 50)
    no_fault_detected = (top_score < 5.0)

    if sh_ok and dt_ok and pct_ok and tev_ok and dtcond_ok and symptoms_ok and no_fault_detected:
        ok_msg = "Equipo en niveles y condiciones óptimas para trabajar."

    actions: List[str] = []
    if ok_msg:
        top = [(ok_msg, 100.0)]
        actions += [
            "Registrar lecturas como referencia (PSI, SH, ΔT aire, Amps, RLA, %RLA, T evap, T cond, Tamb_ext, ΔTcond_ext).",
            "Mantener mantenimiento preventivo (filtros y serpentines limpios)."
        ]
    else:
        if top:
            best_fault, best_score = top[0]
            if best_score >= 65:
                if "Baja carga" in best_fault:
                    actions += [
                        "Buscar fuga (jabón/detector) y reparar.",
                        "Vacío profundo y recarga por peso.",
                        "Revalidar SH y ΔT aire interior."
                    ]
                elif "Exceso de refrigerante" in best_fault:
                    actions += [
                        "Confirmar refrigerante correcto y que la carga fue por peso (no por presión).",
                        "Si se sospecha sobrecarga: recuperar y recargar POR PESO según placa.",
                        "Verificar ventilación exterior: ventilador, recirculación de aire y obstrucciones."
                    ]
                elif "Restricción" in best_fault:
                    actions += [
                        "Buscar escarcha localizada / caída de T en componente (filtro/capilar/TXV).",
                        "Revisar TXV y posibles obstrucciones/humedad.",
                        "Cambiar filtro secador, evacuar y recargar si aplica."
                    ]
                elif "Bajo flujo" in best_fault:
                    actions += [
                        "Revisar turbina interior, capacitor (si aplica), filtros y serpentín evaporador.",
                        "Confirmar ΔT aire tras corregir flujo."
                    ]
                elif "Condensador sucio" in best_fault:
                    actions += [
                        "Lavar condensador exterior (aletas/serpentín).",
                        "Confirmar que ΔTcond_ext baje tras la limpieza."
                    ]
                elif "Ventilación exterior deficiente" in best_fault:
                    actions += [
                        "Revisar ventilador del condensador (giro, capacitor, rpm, sentido).",
                        "Revisar obstrucciones y recirculación de aire (unidad muy encerrada).",
                        "Confirmar que ΔTcond_ext baje tras corregir ventilación."
                    ]

    res = StepResult(tsat, sh, scores, top, warnings, actions)
    return res, pct_rla_val, dtcond_ext_val, top_score


# ============================================================
# Chaquopy entrypoint: leer ClaseDatosPythonJava y escribir resultados
# ============================================================

ClaseDatosPythonJava = jclass("edu.mx.tecnm.hvac_pro2.ClaseDatosPythonJava")

_SIM = None


def _get_sim():
    global _SIM
    if _SIM is None:
        _SIM = build_fuzzy_hvac_model_no_high_side()
    return _SIM


def run_diagnosis_from_java():
    """
    Lee variables desde ClaseDatosPythonJava (static fields),
    ejecuta el diagnóstico, y escribe:
      - ClaseDatosPythonJava.diagnostico
      - ClaseDatosPythonJava.nivel_carga
      - ClaseDatosPythonJava.tsat_evap_c
      - ClaseDatosPythonJava.sh_c
    """
    sim = _get_sim()

    # ----- Entradas desde Java -----
    ref = (getattr(ClaseDatosPythonJava, "refrigerante", None) or "R410A").strip().upper()

    suction_psi = float(getattr(ClaseDatosPythonJava, "presion_succion_psi", 0.0) or 0.0)
    slt_c = float(getattr(ClaseDatosPythonJava, "temp_linea_succion_c", 0.0) or 0.0)
    dt_air = float(getattr(ClaseDatosPythonJava, "deltaT_aire_interior_c", 0.0) or 0.0)

    amps = float(getattr(ClaseDatosPythonJava, "amperaje_compresor_a", 0.0) or 0.0)
    rla = float(getattr(ClaseDatosPythonJava, "rla_compresor_a", 0.0) or 0.0)

    tevap = float(getattr(ClaseDatosPythonJava, "temp_evaporador_c", 0.0) or 0.0)
    tcond = float(getattr(ClaseDatosPythonJava, "temp_condensador_c", 0.0) or 0.0)
    tamb = float(getattr(ClaseDatosPythonJava, "temp_ambiente_exterior_c", 0.0) or 0.0)

    # Ciudad / altitud
    ciudad = str(getattr(ClaseDatosPythonJava, "ciudad", "") or "")
    alt_m = float(getattr(ClaseDatosPythonJava, "altitud_msnm", 0.0) or 0.0)

    # Síntomas (0/50/100)
    lc = float(getattr(ClaseDatosPythonJava, "s_enfria_poco", 50) or 50)
    ic = float(getattr(ClaseDatosPythonJava, "s_hielo_evaporador", 50) or 50)
    la = float(getattr(ClaseDatosPythonJava, "s_flujo_aire_bajo", 50) or 50)
    dc = float(getattr(ClaseDatosPythonJava, "s_condensador_sucio", 50) or 50)
    ch = float(getattr(ClaseDatosPythonJava, "s_compresor_muy_caliente", 50) or 50)
    fr = float(getattr(ClaseDatosPythonJava, "s_escarcha_localizada_restr", 50) or 50)

    # NUEVOS síntomas OK (0/50/100) - defaults 100
    fco = float(getattr(ClaseDatosPythonJava, "s_fan_cond_ok", 100) or 100)
    feo = float(getattr(ClaseDatosPythonJava, "s_fan_evap_ok", 100) or 100)

    state = {
        "refrigerant": ref,
        "suction_psi": suction_psi,
        "suction_line_temp_c": slt_c,
        "deltaT_air_c": dt_air,
        "comp_current_a": amps,
        "comp_rla_a": rla,
        "temp_evap_c": tevap,
        "temp_cond_c": tcond,
        "temp_amb_ext_c": tamb,
        "altitude_m": alt_m,
        "symptoms": {
            "low_cooling": lc,
            "ice": ic,
            "dirty_cond": dc,
            "low_airflow": la,
            "comp_hot": ch,
            "frost_restriction": fr,
            "fan_cond_ok": fco,
            "fan_evap_ok": feo,
        }
    }

    res, pct_rla_val, dtcond_ext_val, top_score = diagnose(state, sim)

    # ----- Escribir resultados básicos a Java -----
    ClaseDatosPythonJava.tsat_evap_c = float(res.tsat_evap or 0.0)
    ClaseDatosPythonJava.sh_c = float(res.sh or 0.0)

    # Determinar nivel_carga:
    # 1 Óptimo, 2 Exceso, 3 Falta, 4 Indefinido
    lvl = 4

    if res.top and res.top[0][0].startswith("Equipo en niveles"):
        lvl = 1
    else:
        best_name = res.top[0][0] if res.top else ""
        if top_score >= 65:
            if "Exceso de refrigerante" in best_name:
                lvl = 2
            elif "Baja carga" in best_name:
                lvl = 3
            else:
                lvl = 4
        else:
            lvl = 4

    # Armar texto completo para mostrar en Android
    lines = []
    lines.append("========== RESULTADOS ==========")
    lines.append(f"Ciudad: {ciudad}")
    lines.append(f"Altitud (msnm): {alt_m:.0f}")
    lines.append(f"Refrigerante: {ref}")
    lines.append(f"Presión succión (PSI) local: {suction_psi:.1f}")

    psig_equiv, _ = psig_local_to_sea_level_equiv(suction_psi, alt_m)
    lines.append(f"Presión succión (PSI) equiv nivel mar: {psig_equiv:.1f}")

    lines.append(f"SLT (°C): {slt_c:.1f}")
    lines.append(f"ΔT aire (°C): {dt_air:.1f}")
    lines.append(f"Amps (A): {amps:.1f}")
    lines.append(f"RLA (A): {rla:.1f}")
    lines.append(f"%RLA: {pct_rla_val:.1f}%")
    lines.append(f"T evap (°C): {tevap:.1f}")
    lines.append(f"T cond (°C): {tcond:.1f}")
    lines.append(f"Tamb ext (°C): {tamb:.1f}")
    lines.append(f"ΔTcond_ext (°C): {dtcond_ext_val:.1f}")
    lines.append(f"Tsat evap (°C): {('N/D' if res.tsat_evap is None else f'{res.tsat_evap:.1f}')}")
    lines.append(f"SH (°C): {('N/D' if res.sh is None else f'{res.sh:.1f}')}")

    lines.append("\nSíntomas (0/50/100):")
    lines.append(f" - Enfría poco: {lc:.0f}")
    lines.append(f" - Hielo evaporador: {ic:.0f}")
    lines.append(f" - Flujo aire bajo: {la:.0f}")
    lines.append(f" - Condensador sucio: {dc:.0f}")
    lines.append(f" - Ventilador condensador OK: {fco:.0f}")
    lines.append(f" - Turbina evaporador OK: {feo:.0f}")
    lines.append(f" - Compresor muy caliente: {ch:.0f}")
    lines.append(f" - Escarcha restricción: {fr:.0f}")

    if res.warnings:
        lines.append("\nAdvertencias:")
        for w in res.warnings:
            lines.append(f" - {w}")

    lines.append("\nTop diagnóstico:")
    for name, sc in res.top:
        lines.append(f" - {name}: {sc:.1f}%")

    lines.append("\nScores:")
    for name, sc in sorted(res.scores.items(), key=lambda x: x[1], reverse=True):
        lines.append(f" - {name}: {sc:.1f}%")

    if res.suggested_actions:
        lines.append("\nAcciones sugeridas:")
        for a in res.suggested_actions:
            lines.append(f" - {a}")

    diag_text = "\n".join(lines)

    ClaseDatosPythonJava.nivel_carga = int(lvl)
    ClaseDatosPythonJava.diagnostico = diag_text

    return diag_text
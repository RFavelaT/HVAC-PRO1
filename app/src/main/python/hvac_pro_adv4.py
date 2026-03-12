from edu.mx.tecnm.hvac_pro2 import ClaseDatosPythonJava as Datos

# -----------------------------
# Utilidad: parsear flotantes (soporta coma decimal)
# -----------------------------
def parse_float(texto):
    try:
        return float(texto.replace(",", ".").strip())
    except Exception:
        raise ValueError

# -----------------------------
# Rangos por refrigerante (centro, tolerancia)
# -----------------------------
RANGOS = {
    "R32": {
        "PB":  (120, 10),   # psi
        "TB":  (12.5, 5.5),     # °C (línea de succión)
        "TA":  (50, 6)     # °C ambiente / descarga
    },
    "R22": {
        "PB":  (68, 6),
        "TB":  (12, 6),
        "TA":  (50, 6)
    },
    "R410a": {
        "PB":  (115, 10),
        "TB":  (11, 6),
        "TA":  (50, 6)
    }
}

def bounds(centro, tol):
    return (centro - tol, centro + tol)

# -----------------------------
# NUEVO: Tablas P-T (Tsat vs presión) para R410a, R22 y R32
# Presiones en psig para vapor saturado aprox.
# -----------------------------
PT_R410A = {
    -20.0: 43.4,
    -16.0: 52.7,
    -12.0: 63.0,
    -8.0: 74.5,
    -4.0: 87.3,
    0.0: 101.4,
    4.0: 117.0,
    8.0: 134.0,
    12.0: 152.7,
    16.0: 173.0,
    20.0: 195.2,
    24.0: 219.3,
    28.0: 245.4,
    32.0: 273.7,
    36.0: 304.2,
    40.0: 337.0,
    44.0: 372.4,
    48.0: 410.5,
    52.0: 451.3,
    56.0: 495.3,
    60.0: 542.4,
}

PT_R22 = {
    -20.0: 20.9,
    -10.0: 36.8,
    0.0: 57.5,
    10.0: 84.1,
    20.0: 117.0,
    30.0: 158.0,
    40.0: 208.0,
    50.0: 267.0,
    60.0: 337.0,
}

PT_R32 = {
    -20.0: 44.2,
    -10.0: 69.8,
    0.0: 100.0,
    10.0: 146.0,
    20.0: 199.0,
    30.0: 265.0,
    40.0: 345.0,
    50.0: 441.0,
    60.0: 556.0,
}

TABLAS_PT = {
    "R410a": PT_R410A,
    "R22": PT_R22,
    "R32": PT_R32
}

# -----------------------------
# NUEVO: Interpolación Tsat por presión
# -----------------------------
def tsat_por_presion(ref, pb):
    """
    Calcula la temperatura de saturación (°C) a partir de la presión de baja (pb, en psi)
    usando una tabla P-T y interpolación lineal.
    """
    if ref not in TABLAS_PT:
        raise ValueError(f"No hay tabla P-T para el refrigerante {ref}")

    tabla = TABLAS_PT[ref]

    # Las llaves son temperaturas, los valores son presiones
    temperaturas = sorted(tabla.keys())
    presiones = [tabla[T] for T in temperaturas]

    # Si está fuera de rango, se satura al extremo más cercano
    if pb <= presiones[0]:
        return temperaturas[0]
    if pb >= presiones[-1]:
        return temperaturas[-1]

    # Buscar el intervalo P1 <= pb <= P2
    for i in range(len(presiones) - 1):
        P1 = presiones[i]
        P2 = presiones[i + 1]

        if P1 <= pb <= P2:
            T1 = temperaturas[i]
            T2 = temperaturas[i + 1]
            # Interpolación lineal
            slope = (T2 - T1) / (P2 - P1)
            Tsat = T1 + (pb - P1) * slope
            return Tsat

    # No debería llegar aquí
    raise RuntimeError("Error en la interpolación P-T")

# -----------------------------
# NUEVO: Cálculo de sobrecalentamiento
# -----------------------------
def calcular_sobrecalentamiento(ref, pb, t_succion):
    """
    Calcula Tsat y sobrecalentamiento (SH = T_succion - Tsat).
    Devuelve (SH, Tsat).
    """
    tsat = tsat_por_presion(ref, pb)
    sh = t_succion - tsat
    return sh, tsat

# -----------------------------
# Diagnóstico
# -----------------------------
def diagnosticar():
    ref = Datos.refrigerante
    pb = parse_float(Datos.presionLbaja)
    tb = parse_float(Datos.tempLbaja)   # asumimos T succión (línea gruesa)
    ta = parse_float(Datos.tempLalta)
    tipo_equipo = Datos.tipo_equipo

    if ref == "Otro":
        print("Error", "No hay información para otros refrigerantes.")
        Datos.diagnostico = "No hay información para otros refrigerantes."
        Datos.nivel_carga = 4
        return

    if ref not in RANGOS:
        print("Error", "Selecciona un refrigerante válido.")
        return

    # Rangos del refrigerante elegido
    pb_c, pb_tol = RANGOS[ref]["PB"]
    tb_c, tb_tol = RANGOS[ref]["TB"]
    ta_c, ta_tol = RANGOS[ref]["TA"]

    pb_min, pb_max = bounds(pb_c, pb_tol)
    tb_min, tb_max = bounds(tb_c, tb_tol)
    ta_min, ta_max = bounds(ta_c, ta_tol)

    # NUEVO: Rango de sobrecalentamiento "ideal" (ej. 4–12 °C alrededor de 8 °C)
    SH_CENTRO = 8.0
    SH_TOL = 4.0
    sh_min, sh_max = bounds(SH_CENTRO, SH_TOL)

    # Estado de cada variable (para mostrar detalle)
    def status(v, vmin, vmax):
        if v < vmin: return f"abajo ({v:.2f} < {vmin:.2f})"
        if v > vmax: return f"arriba ({v:.2f} > {vmax:.2f})"
        return f"en rango ({vmin:.2f}–{vmax:.2f})"

    # NUEVO: calcular sobrecalentamiento
    sobrecalentamiento = None
    tsat = None
    try:
        sobrecalentamiento, tsat = calcular_sobrecalentamiento(ref, pb, tb)
    except Exception as e:
        # Si falla el cálculo, seguimos usando solo PB/TB/TA
        print("Aviso", f"No se pudo calcular el sobrecalentamiento: {e}")

    detalle = (
        f"- PB: {status(pb, pb_min, pb_max)} psi\n"
        f"- T_succion (T_baja): {status(tb, tb_min, tb_max)} °C\n"
        f"- T_alta: {status(ta, ta_min, ta_max)} °C\n"
    )

    if sobrecalentamiento is not None and tsat is not None:
        # Clasificar sobrecalentamiento
        if sobrecalentamiento < sh_min:
            estado_sh = "BAJO (riesgo de líquido al compresor; posible exceso de gas o válvula muy abierta)"
        elif sobrecalentamiento > sh_max:
            estado_sh = "ALTO (posible falta de refrigerante o flujo de aire deficiente)"
        else:
            estado_sh = "EN RANGO"

        detalle += (
            f"- Tsat evaporador: {tsat:.2f} °C\n"
            f"- Sobrecalentamiento: {sobrecalentamiento:.2f} °C → {estado_sh}\n"
            f"- Rango recomendado SH: {sh_min:.1f}–{sh_max:.1f} °C\n"
        )

    # ---------- LÓGICA DE DECISIÓN ----------

    sh_ok = False
    sh_bajo = False
    sh_alto = False
    if sobrecalentamiento is not None:
        sh_ok = (sh_min <= sobrecalentamiento <= sh_max)
        sh_bajo = (sobrecalentamiento < sh_min)
        sh_alto = (sobrecalentamiento > sh_max)

    # Óptimo: los tres dentro + sobrecalentamiento en rango (si existe)
    if (pb_min <= pb <= pb_max) and (tb_min <= tb <= tb_max) and (ta_min <= ta <= ta_max) and (sh_ok or sobrecalentamiento is None):
        msg = (
            f"Condición ÓPTIMA para {ref} ({tipo_equipo}).\n\n"
            f"Parámetros dentro de rango esperado."
            f"\n\n{detalle}"
        )
        print("Diagnóstico", msg)
        Datos.diagnostico = msg
        Datos.nivel_carga = 1
        return

    # Regla: EXCESO si PB > PB_max y T_baja < T_baja_min y T_alta > T_alta_max
    # Afinada con sobrecalentamiento BAJO si se pudo calcular
    if (pb > pb_max) and (tb < tb_min) and (ta > ta_max) and (sh_bajo or sobrecalentamiento is None):
        extra = ""
        if sh_bajo:
            extra = "\nEl sobrecalentamiento BAJO refuerza el diagnóstico de EXCESO de refrigerante."
        msg = (
            f"EXCESO DE REFRIGERANTE detectado en {ref} ({tipo_equipo}).\n"
            f"Presión de succión alta, temperatura de succión baja y temperatura de alta elevada."
            f"{extra}\n\n{detalle}"
        )
        print("Diagnóstico", msg)
        Datos.diagnostico = msg
        Datos.nivel_carga = 2
        return

    # Regla: FALTA si PB < PB_min y T_baja > T_baja_max y T_alta < T_alta_min
    # Afinada con sobrecalentamiento ALTO si se pudo calcular
    if (pb < pb_min) and (tb > tb_max) and (ta < ta_min) and (sh_alto or sobrecalentamiento is None):
        extra = ""
        if sh_alto:
            extra = "\nEl sobrecalentamiento ALTO refuerza el diagnóstico de FALTA de refrigerante."
        msg = (
            f"FALTA DE REFRIGERANTE detectada en {ref} ({tipo_equipo}).\n"
            f"Presión de succión baja, temperatura de succión alta y temperatura de alta reducida."
            f"{extra}\n\n{detalle}"
        )
        print("Diagnóstico", msg)
        Datos.diagnostico = msg
        Datos.nivel_carga = 3
        return

    # Cualquier otro patrón: no concluyente
    msg = (
        "Falla indefinida / patrón mixto.\n"
        "Verifica flujo de aire, limpieza de filtros, serpentines, ventiladores, "
        "válvula de expansión/capilar, y lecturas de instrumentos.\n\n"
        f"{detalle}"
    )
    print("Diagnóstico", msg)
    Datos.diagnostico = msg
    Datos.nivel_carga = 4

# -----------------------------
# UI
# -----------------------------
diagnosticar()

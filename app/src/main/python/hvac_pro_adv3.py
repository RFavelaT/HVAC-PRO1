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
        "PB":  (118, 8),   # psi
        "TB":  (4, 3),    # °C
        "TA":  (50, 5)    # °C
    },
    "R22": {
        "PB":  (67, 7),
        "TB":  (3, 3),
        "TA":  (50, 5)
    },
    "R410a": {
        "PB":  (115, 12),
        "TB":  (3, 3),
        "TA":  (47, 3)
    }
}

def bounds(centro, tol):
    return (centro - tol, centro + tol)

# -----------------------------
# Diagnóstico
# -----------------------------
def diagnosticar():
    ref = Datos.refrigerante
    pb = parse_float(Datos.presionLbaja)
    tb = parse_float(Datos.tempLbaja)
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

    # Estado de cada variable (para mostrar detalle)
    def status(v, vmin, vmax):
        if v < vmin: return f"abajo ({v:.2f} < {vmin:.2f})"
        if v > vmax: return f"arriba ({v:.2f} > {vmax:.2f})"
        return f"en rango ({vmin:.2f}–{vmax:.2f})"

    detalle = (
        f"- PB: {status(pb, pb_min, pb_max)} psi\n"
        f"- T_baja: {status(tb, tb_min, tb_max)} °C\n"
        f"- T_alta: {status(ta, ta_min, ta_max)} °C"
    )

    # ---------- LÓGICA DE DECISIÓN ----------
    # Óptimo: los tres dentro
    if (pb_min <= pb <= pb_max) and (tb_min <= tb <= tb_max) and (ta_min <= ta <= ta_max):
        msg = f"Condición ÓPTIMA para {ref} ({tipo_equipo}).\n\n{detalle}"
        print("Diagnóstico", msg)
        Datos.diagnostico = msg
        Datos.nivel_carga = 1
        return

    # Regla: EXCESO si PB > PB_max y T_baja < T_baja_min
    if (pb > pb_max) and (tb < tb_min) and (ta > ta_max):
        msg = f"EXCESO DE REFRIGERANTE detectado en {ref} ({tipo_equipo}).\n\n{detalle}"
        print("Diagnóstico", msg)
        Datos.diagnostico = msg
        Datos.nivel_carga = 2
        return

    # Regla: FALTA si PB < PB_min y T_baja > T_baja_max
    if (pb < pb_min) and (tb > tb_max) and (ta < ta_min):
        msg = f"FALTA DE REFRIGERANTE detectada en {ref} ({tipo_equipo}).\n\n{detalle}"
        print("Diagnóstico", msg)
        Datos.diagnostico = msg
        Datos.nivel_carga = 3
        return

    # Cualquier otro patrón: no concluyente
    msg = f"Falla indefinida / patrón mixto.\nVerifica flujo de aire, limpieza de filtros, válvula de expansión/capilar, y lecturas.\n\n{detalle}"
    print("Diagnóstico", msg)
    Datos.diagnostico = msg
    Datos.nivel_carga = 4

# -----------------------------
# UI
# -----------------------------

diagnosticar()





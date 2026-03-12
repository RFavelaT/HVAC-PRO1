package edu.mx.tecnm.hvac_pro2;

public class ClaseDatosPythonJava {

    /*
    Clase puente entre Java y Python para comunicar datos en ambos sentidos.
    */

    // =========================
    // Ya existentes (se conservan)
    // =========================
    public static String refrigerante;     // "R410A", "R22", "R32"
    public static String tipo_equipo;      // opcional (minisplit, inverter, etc.)
    public static String presionLbaja;     // (legacy) presión baja
    public static String tempLbaja;        // (legacy) temp línea baja
    public static String tempLalta;        // (legacy) si algún día vuelves a usar alta
    public static String diagnostico;

    public static int nivel_carga;
    /*
        Niveles de carga
        1: Óptimo
        2: Exceso
        3: Falta
        4: Indefinido
     */


    // =========================
    // NUEVO: Mediciones UI (valores numéricos)
    // =========================

    // Medición principal (baja)
    public static float presion_succion_psi;       // slider: Presión de succión (PSI)
    public static float temp_linea_succion_c;      // slider: Temp. línea succión (°C) = SLT
    public static float deltaT_aire_interior_c;    // slider: ΔT aire interior (Evaporador)

    // Corriente y placa
    public static float amperaje_compresor_a;      // slider: Amperaje medido (A)
    public static float rla_compresor_a;           // slider: RLA de placa (A)

    // Temperaturas IR
    public static float temp_evaporador_c;         // slider: Temp. Evaporador (°C)
    public static float temp_condensador_c;        // slider: Temp. Condensador (°C)
    public static float temp_ambiente_exterior_c;  // slider: Temp. ambiente exterior (°C)

    // (Opcional) calculados en Java si quieres pasarlos ya listos a Python
    public static float porcentaje_rla;            // %RLA = (A/RLA)*100
    public static float deltaT_cond_ext_c;         // ΔTcond_ext = Tcond - Tamb_ext


    // =========================
    // NUEVO: Síntomas/observaciones (No/Medio/Sí -> 0/50/100)
    // =========================
    public static int s_enfria_poco;               // 0/50/100
    public static int s_hielo_evaporador;          // 0/50/100
    public static int s_flujo_aire_bajo;           // 0/50/100
    public static int s_condensador_sucio;         // 0/50/100
    public static int s_compresor_muy_caliente;    // 0/50/100
    public static int s_escarcha_localizada_restr; // 0/50/100


    // =========================
    // NUEVO: Resultados extra (si Python te los regresa)
    // =========================
    public static float tsat_evap_c;               // Tsat calculada desde P succión
    public static float sh_c;                      // Sobrecalentamiento calculado

    public static String ciudad = "";
    public static float altitud_msnm = 0.0f;

    // NUEVOS síntomas (0/50/100)
    public static int s_fan_cond_ok = 100;  // Ventilador condensador OK (Sí=100, Medio=50, No=0)
    public static int s_fan_evap_ok = 100;  // Turbina evaporador OK (Sí=100, Medio=50, No=0)

}

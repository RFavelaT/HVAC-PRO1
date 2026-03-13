package edu.mx.tecnm.hvac_pro2;

public class ClaseDatosPythonJava {

    /*
    Clase puente entre Java y Python para comunicar datos en ambos sentidos.
    */

    // =========================
    // Ya existentes (legacy / compatibilidad)
    // =========================
    public static String refrigerante = "R410A";   // "R410A", "R22", "R32"
    public static String tipo_equipo = "";         // opcional (minisplit, inverter, etc.)
    public static String presionLbaja = "0";       // legacy
    public static String tempLbaja = "0";          // legacy
    public static String tempLalta = "0";          // legacy
    public static String diagnostico = "";

    public static int nivel_carga = 4;
    /*
        Niveles de carga
        1: Óptimo
        2: Exceso
        3: Falta
        4: Indefinido
     */


    // =========================
    // Mediciones UI (valores numéricos)
    // =========================

    // Medición principal (baja)
    public static float presion_succion_psi = 0.0f;       // ahora puede ser negativo hasta -10 psi
    public static float temp_linea_succion_c = 0.0f;      // SLT
    public static float deltaT_aire_interior_c = 0.0f;    // ΔT aire interior

    // Corriente y placa
    public static float amperaje_compresor_a = 0.0f;      // amperaje medido
    public static float rla_compresor_a = 0.0f;           // RLA placa

    // Temperaturas IR
    public static float temp_evaporador_c = 0.0f;
    public static float temp_condensador_c = 0.0f;
    public static float temp_ambiente_exterior_c = 0.0f;

    // Calculados opcionales en Java
    public static float porcentaje_rla = 0.0f;
    public static float deltaT_cond_ext_c = 0.0f;


    // =========================
    // Síntomas / observaciones (No/Medio/Sí -> 0/50/100)
    // =========================
    public static int s_enfria_poco = 50;
    public static int s_hielo_evaporador = 50;
    public static int s_flujo_aire_bajo = 50;
    public static int s_condensador_sucio = 50;
    public static int s_compresor_muy_caliente = 50;
    public static int s_escarcha_localizada_restr = 50;

    // Nuevos síntomas OK (Sí=100, Medio=50, No=0)
    public static int s_fan_cond_ok = 100;
    public static int s_fan_evap_ok = 100;


    // =========================
    // Resultados devueltos por Python
    // =========================
    public static float tsat_evap_c = 0.0f;
    public static float sh_c = 0.0f;

    public static String ciudad = "";
    public static float altitud_msnm = 0.0f;
}
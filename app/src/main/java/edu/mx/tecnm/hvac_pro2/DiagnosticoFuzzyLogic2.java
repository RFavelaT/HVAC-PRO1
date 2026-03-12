package edu.mx.tecnm.hvac_pro2;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DiagnosticoFuzzyLogic2 extends AppCompatActivity {

    // ===== Catálogo de ciudades y altitud =====
    private final Map<String, Integer> CIUDADES_ALT = new LinkedHashMap<String, Integer>() {{
        put("Chihuahua", 1415);
        put("Cd. Juárez", 1140);
        put("Delicias", 1171);
        put("Cuauhtémoc", 2060);
        put("Parral", 1620);
        put("Jiménez", 1370);
        put("Ciudad de México", 2240);
        put("Guadalajara", 1566);
        put("Monterrey", 540);
        put("Cancún", 10);
        put("Acapulco", 30);
        put("Puerto Vallarta", 7);
        put("Mazatlán", 10);
    }};

    // Helpers: SeekBar progress -> float
    private float progressToFloat(int progress, float min, float step) {
        return min + (progress * step);
    }

    private String fmt1(float x) {
        return String.format(Locale.US, "%.1f", x);
    }

    private String fmt0(float x) {
        return String.format(Locale.US, "%.0f", x);
    }

    private interface OnProgressChange {
        void onChange(int progress);
    }

    private SeekBar.OnSeekBarChangeListener simpleListener(final OnProgressChange cb) {
        return new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                cb.onChange(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        };
    }

    // Convierte Spinner "No/Medio/Sí" -> 0/50/100
    private int triToInt(String tri) {
        if (tri == null) return 50;
        tri = tri.trim();
        if (tri.equalsIgnoreCase("No")) return 0;
        if (tri.equalsIgnoreCase("Sí") || tri.equalsIgnoreCase("Si")) return 100;
        return 50; // "Medio"
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diagnostico_fuzzy_logic2);

        // Revisar si Python está iniciado
        if (!Python.isStarted()) {
            Python.start(new AndroidPlatform(this));
        }
        final Python py = Python.getInstance();

        // ===== Referencias UI =====
        Spinner spCity = findViewById(R.id.spCity);
        TextView tvAltitudeValue = findViewById(R.id.tvAltitudeValue);

        Spinner spRef = findViewById(R.id.spRefrigerant);

        Spinner spLowCooling = findViewById(R.id.spLowCooling);
        Spinner spIceEvap = findViewById(R.id.spIceEvap);
        Spinner spLowAirflow = findViewById(R.id.spLowAirflow);
        Spinner spDirtyCond = findViewById(R.id.spDirtyCond);

        // ===== NUEVOS spinners =====
        Spinner spCondFanOk = findViewById(R.id.spCondFanOk);
        Spinner spEvapFanOk = findViewById(R.id.spEvapFanOk);

        Spinner spCompHot = findViewById(R.id.spCompHot);
        Spinner spFrostRestr = findViewById(R.id.spFrostRestr);

        SeekBar sbSuction = findViewById(R.id.sbSuctionPsi);
        TextView tvSuction = findViewById(R.id.tvSuctionPsiValue);

        SeekBar sbSlt = findViewById(R.id.sbSuctionLineTemp);
        TextView tvSlt = findViewById(R.id.tvSuctionLineTempValue);

        SeekBar sbDt = findViewById(R.id.sbDeltaTAir);
        TextView tvDt = findViewById(R.id.tvDeltaTAirValue);

        SeekBar sbAmps = findViewById(R.id.sbCompAmps);
        TextView tvAmps = findViewById(R.id.tvCompAmpsValue);

        SeekBar sbRla = findViewById(R.id.sbRla);
        TextView tvRla = findViewById(R.id.tvRlaValue);

        SeekBar sbTevap = findViewById(R.id.sbTevap);
        TextView tvTevap = findViewById(R.id.tvTevapValue);

        SeekBar sbTcond = findViewById(R.id.sbTcond);
        TextView tvTcond = findViewById(R.id.tvTcondValue);

        SeekBar sbTamb = findViewById(R.id.sbTambExt);
        TextView tvTamb = findViewById(R.id.tvTambExtValue);

        Button btn = findViewById(R.id.btnDiagnose);
        TextView tvResults = findViewById(R.id.tvResults);

        // =====================================================
        // Spinner de ciudad + altitud
        // =====================================================
        String[] ciudades = CIUDADES_ALT.keySet().toArray(new String[0]);

        ArrayAdapter<String> cityAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                ciudades
        );
        cityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCity.setAdapter(cityAdapter);

        // Selección por defecto: Chihuahua
        int defaultCityIndex = 0;
        spCity.setSelection(defaultCityIndex);

        String ciudadInicial = ciudades[defaultCityIndex];
        int altitudInicial = CIUDADES_ALT.get(ciudadInicial);

        tvAltitudeValue.setText("Altitud: " + altitudInicial + " msnm");
        ClaseDatosPythonJava.ciudad = ciudadInicial;
        ClaseDatosPythonJava.altitud_msnm = (float) altitudInicial;

        spCity.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String ciudad = ciudades[position];
                int altitud = CIUDADES_ALT.get(ciudad);

                tvAltitudeValue.setText("Altitud: " + altitud + " msnm");

                ClaseDatosPythonJava.ciudad = ciudad;
                ClaseDatosPythonJava.altitud_msnm = (float) altitud;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        // =====================================================
        // Refrigerante
        // =====================================================
        List<String> refs = Arrays.asList("R410A", "R22", "R32");
        ArrayAdapter<String> refAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                refs
        );
        spRef.setAdapter(refAdapter);

        // =====================================================
        // Spinner tri-state (No/Medio/Sí)
        // =====================================================
        List<String> triValues = Arrays.asList("No", "Medio", "Sí");
        ArrayAdapter<String> triAdapter = new ArrayAdapter<>(
                this,
                R.layout.item_spinner_tri,
                triValues
        );
        triAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        Spinner[] triSpinners = new Spinner[] {
                spLowCooling, spIceEvap, spLowAirflow, spDirtyCond,
                spCondFanOk, spEvapFanOk, // <<<<<< NUEVOS
                spCompHot, spFrostRestr
        };
        for (Spinner s : triSpinners) {
            s.setAdapter(triAdapter);
            s.setSelection(0); // default "No"
        }
        spLowCooling.setSelection(1); // default "Medio"

        // (opcional) defaults para nuevos síntomas:
        // Si quieres que por defecto se asuma que sí funcionan:
        spCondFanOk.setSelection(2);  // "Sí"
        spEvapFanOk.setSelection(2);  // "Sí"

        // =====================================================
        // SeekBars + TextViews
        // =====================================================
        sbSuction.setProgress(95); // 115
        tvSuction.setText("115");
        sbSuction.setOnSeekBarChangeListener(simpleListener(progress -> {
            int v = 20 + progress;
            tvSuction.setText(String.valueOf(v));
        }));

        sbSlt.setProgress(270); // 22.0
        tvSlt.setText("22.0");
        sbSlt.setOnSeekBarChangeListener(simpleListener(progress -> {
            float v = progressToFloat(progress, -5f, 0.1f);
            tvSlt.setText(fmt1(v));
        }));

        sbDt.setProgress(100); // 10.0
        tvDt.setText("10.0");
        sbDt.setOnSeekBarChangeListener(simpleListener(progress -> {
            float v = progressToFloat(progress, 0f, 0.1f);
            tvDt.setText(fmt1(v));
        }));

        sbAmps.setProgress(80); // 8.0
        tvAmps.setText("8.0");
        sbAmps.setOnSeekBarChangeListener(simpleListener(progress -> {
            float v = progressToFloat(progress, 0f, 0.1f);
            tvAmps.setText(fmt1(v));
        }));

        sbRla.setProgress(90); // 10.0
        tvRla.setText("10.0");
        sbRla.setOnSeekBarChangeListener(simpleListener(progress -> {
            float v = progressToFloat(progress, 1f, 0.1f);
            tvRla.setText(fmt1(v));
        }));

        sbTevap.setProgress(260); // 6.0
        tvTevap.setText("6.0");
        sbTevap.setOnSeekBarChangeListener(simpleListener(progress -> {
            float v = progressToFloat(progress, -20f, 0.1f);
            tvTevap.setText(fmt1(v));
        }));

        sbTcond.setProgress(250); // 45.0
        tvTcond.setText("45.0");
        sbTcond.setOnSeekBarChangeListener(simpleListener(progress -> {
            float v = progressToFloat(progress, 20f, 0.1f);
            tvTcond.setText(fmt1(v));
        }));

        sbTamb.setProgress(350); // 35.0
        tvTamb.setText("35.0");
        sbTamb.setOnSeekBarChangeListener(simpleListener(progress -> {
            float v = progressToFloat(progress, 0f, 0.1f);
            tvTamb.setText(fmt1(v));
        }));

        // =====================================================
        // Botón Diagnosticar
        // =====================================================
        btn.setOnClickListener(v -> {
            String ref = spRef.getSelectedItem().toString();

            String ciudadSel = spCity.getSelectedItem().toString();
            int altitudSel = CIUDADES_ALT.get(ciudadSel);

            float suctionPsi = (20 + sbSuction.getProgress());
            float sltVal = progressToFloat(sbSlt.getProgress(), -5f, 0.1f);
            float dTairVal = progressToFloat(sbDt.getProgress(), 0f, 0.1f);

            float ampsVal = progressToFloat(sbAmps.getProgress(), 0f, 0.1f);
            float rlaVal = progressToFloat(sbRla.getProgress(), 1f, 0.1f);

            float pctRlaVal = (rlaVal > 0f) ? (ampsVal / rlaVal) * 100f : 0f;

            float tevVal = progressToFloat(sbTevap.getProgress(), -20f, 0.1f);
            float tcondVal = progressToFloat(sbTcond.getProgress(), 20f, 0.1f);
            float tambVal = progressToFloat(sbTamb.getProgress(), 0f, 0.1f);

            float dtCondExtVal = tcondVal - tambVal;
            if (dtCondExtVal < 0f) dtCondExtVal = 0f;

            // Síntomas tri-state -> 0/50/100
            int sLowCooling = triToInt(spLowCooling.getSelectedItem().toString());
            int sIceEvapVal = triToInt(spIceEvap.getSelectedItem().toString());
            int sLowAirflowVal = triToInt(spLowAirflow.getSelectedItem().toString());
            int sDirtyCondVal = triToInt(spDirtyCond.getSelectedItem().toString());
            int sCompHotVal = triToInt(spCompHot.getSelectedItem().toString());
            int sFrostRestrVal = triToInt(spFrostRestr.getSelectedItem().toString());

            // ===== NUEVOS SÍNTOMAS =====
            int sCondFanOkVal = triToInt(spCondFanOk.getSelectedItem().toString());
            int sEvapFanOkVal = triToInt(spEvapFanOk.getSelectedItem().toString());

            // --------- ASIGNAR A ClaseDatosPythonJava ----------
            ClaseDatosPythonJava.refrigerante = ref;
            ClaseDatosPythonJava.presionLbaja = fmt0(suctionPsi);
            ClaseDatosPythonJava.tempLbaja = fmt1(sltVal);

            ClaseDatosPythonJava.presion_succion_psi = suctionPsi;
            ClaseDatosPythonJava.temp_linea_succion_c = sltVal;
            ClaseDatosPythonJava.deltaT_aire_interior_c = dTairVal;

            ClaseDatosPythonJava.amperaje_compresor_a = ampsVal;
            ClaseDatosPythonJava.rla_compresor_a = rlaVal;

            ClaseDatosPythonJava.porcentaje_rla = pctRlaVal;
            ClaseDatosPythonJava.temp_evaporador_c = tevVal;

            ClaseDatosPythonJava.temp_condensador_c = tcondVal;
            ClaseDatosPythonJava.temp_ambiente_exterior_c = tambVal;
            ClaseDatosPythonJava.deltaT_cond_ext_c = dtCondExtVal;

            ClaseDatosPythonJava.ciudad = ciudadSel;
            ClaseDatosPythonJava.altitud_msnm = (float) altitudSel;

            ClaseDatosPythonJava.s_enfria_poco = sLowCooling;
            ClaseDatosPythonJava.s_hielo_evaporador = sIceEvapVal;
            ClaseDatosPythonJava.s_flujo_aire_bajo = sLowAirflowVal;
            ClaseDatosPythonJava.s_condensador_sucio = sDirtyCondVal;
            ClaseDatosPythonJava.s_compresor_muy_caliente = sCompHotVal;
            ClaseDatosPythonJava.s_escarcha_localizada_restr = sFrostRestrVal;

            // ===== NUEVOS CAMPOS (Paso 3 los agregamos a la clase) =====
            ClaseDatosPythonJava.s_fan_cond_ok = sCondFanOkVal;
            ClaseDatosPythonJava.s_fan_evap_ok = sEvapFanOkVal;

            // Limpiar salida previa
            ClaseDatosPythonJava.diagnostico = "";
            ClaseDatosPythonJava.tsat_evap_c = 0f;
            ClaseDatosPythonJava.sh_c = 0f;
            ClaseDatosPythonJava.nivel_carga = 4;

            String report =
                    "====== CAPTURA ======\n" +
                            "Ciudad: " + ciudadSel + "\n" +
                            "Altitud: " + altitudSel + " msnm\n" +
                            "Ref: " + ref + "\n" +
                            "Succión (PSI): " + fmt0(suctionPsi) + "\n" +
                            "SLT (°C): " + fmt1(sltVal) + "\n" +
                            "ΔT aire (°C): " + fmt1(dTairVal) + "\n" +
                            "Amps (A): " + fmt1(ampsVal) + "\n" +
                            "RLA (A): " + fmt1(rlaVal) + "\n" +
                            "%RLA: " + fmt1(pctRlaVal) + "%\n" +
                            "T evap (°C): " + fmt1(tevVal) + "\n" +
                            "T cond (°C): " + fmt1(tcondVal) + "\n" +
                            "Tamb ext (°C): " + fmt1(tambVal) + "\n" +
                            "ΔTcond_ext (°C): " + fmt1(dtCondExtVal) + "\n\n" +
                            "Síntomas (0/50/100):\n" +
                            "- Enfría poco: " + sLowCooling + "\n" +
                            "- Hielo evap: " + sIceEvapVal + "\n" +
                            "- Bajo flujo: " + sLowAirflowVal + "\n" +
                            "- Cond sucio: " + sDirtyCondVal + "\n" +
                            "- Vent cond OK: " + sCondFanOkVal + "\n" +
                            "- Turb evap OK: " + sEvapFanOkVal + "\n" +
                            "- Comp caliente: " + sCompHotVal + "\n" +
                            "- Escarcha restr: " + sFrostRestrVal + "\n\n" +
                            "Ejecutando diagnóstico...\n";

            tvResults.setText(report);

            try {
                PyObject mod = py.getModule("hvac_pro_fuzzy2");
                PyObject result = mod.callAttr("run_diagnosis_from_java");

                String diag;
                if (ClaseDatosPythonJava.diagnostico != null &&
                        !ClaseDatosPythonJava.diagnostico.trim().isEmpty()) {
                    diag = ClaseDatosPythonJava.diagnostico;
                } else if (result != null) {
                    diag = result.toString();
                } else {
                    diag = "No se obtuvo respuesta del módulo Python.";
                }

                tvResults.setText(diag);

            } catch (Exception e) {
                tvResults.setText(
                        report +
                                "\nERROR al ejecutar Python:\n" +
                                e.getClass().getSimpleName() + ": " + e.getMessage()
                );
            }
        });
    }
}
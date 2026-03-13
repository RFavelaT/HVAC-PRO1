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

    private static final int SUCTION_MIN_PSI = -10;
    private static final int SUCTION_MAX_PSI = 180;

    // Valores iniciales óptimos
    private static final int SUCTION_DEFAULT_PSI = 115;
    private static final float SLT_DEFAULT_C = 13.0f;
    private static final float DT_AIR_DEFAULT_C = 10.0f;
    private static final float AMPS_DEFAULT_A = 8.0f;
    private static final float RLA_DEFAULT_A = 10.0f;
    private static final float TEVAP_DEFAULT_C = 6.0f;
    private static final float TCOND_DEFAULT_C = 45.0f;
    private static final float TAMB_DEFAULT_C = 35.0f;

    private float progressToFloat(int progress, float min, float step) {
        return min + (progress * step);
    }

    private int floatToProgress(float value, float min, float step) {
        return Math.round((value - min) / step);
    }

    private int progressToSuctionPsi(int progress) {
        return SUCTION_MIN_PSI + progress;
    }

    private int suctionPsiToProgress(int psi) {
        return psi - SUCTION_MIN_PSI;
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

    private int triToInt(Spinner spinner) {
        int pos = spinner.getSelectedItemPosition();
        if (pos == 0) return 0;
        if (pos == 1) return 50;
        if (pos == 2) return 100;
        return 50;
    }

    private String triToLabel(int value) {
        if (value <= 0) return "No";
        if (value >= 100) return "Sí";
        return "Medio";
    }

    private String triToDebugText(int value) {
        return value + " (" + triToLabel(value) + ")";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diagnostico_fuzzy_logic2);

        if (!Python.isStarted()) {
            Python.start(new AndroidPlatform(this));
        }
        final Python py = Python.getInstance();

        Spinner spCity = findViewById(R.id.spCity);
        TextView tvAltitudeValue = findViewById(R.id.tvAltitudeValue);

        Spinner spRef = findViewById(R.id.spRefrigerant);

        Spinner spLowCooling = findViewById(R.id.spLowCooling);
        Spinner spIceEvap = findViewById(R.id.spIceEvap);
        Spinner spLowAirflow = findViewById(R.id.spLowAirflow);
        Spinner spDirtyCond = findViewById(R.id.spDirtyCond);
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

        String[] ciudades = CIUDADES_ALT.keySet().toArray(new String[0]);
        ArrayAdapter<String> cityAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                ciudades
        );
        cityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCity.setAdapter(cityAdapter);

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

        List<String> refs = Arrays.asList("R410A", "R22", "R32");
        ArrayAdapter<String> refAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                refs
        );
        spRef.setAdapter(refAdapter);
        spRef.setSelection(0);

        List<String> triValues = Arrays.asList("No", "Medio", "Sí");
        ArrayAdapter<String> triAdapter = new ArrayAdapter<>(
                this,
                R.layout.item_spinner_tri,
                triValues
        );
        triAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        Spinner[] triSpinners = new Spinner[] {
                spLowCooling, spIceEvap, spLowAirflow, spDirtyCond,
                spCondFanOk, spEvapFanOk, spCompHot, spFrostRestr
        };

        for (Spinner s : triSpinners) {
            s.setAdapter(triAdapter);
        }

        // Síntomas óptimos al iniciar
        spLowCooling.setSelection(0); // No
        spIceEvap.setSelection(0);    // No
        spLowAirflow.setSelection(0); // No
        spDirtyCond.setSelection(0);  // No
        spCondFanOk.setSelection(2);  // Sí
        spEvapFanOk.setSelection(2);  // Sí
        spCompHot.setSelection(0);    // No
        spFrostRestr.setSelection(0); // No

        sbSuction.setMax(SUCTION_MAX_PSI - SUCTION_MIN_PSI);
        sbSuction.setProgress(suctionPsiToProgress(SUCTION_DEFAULT_PSI));
        tvSuction.setText(String.valueOf(SUCTION_DEFAULT_PSI));
        sbSuction.setOnSeekBarChangeListener(simpleListener(progress -> {
            int v = progressToSuctionPsi(progress);
            tvSuction.setText(String.valueOf(v));
        }));

        sbSlt.setProgress(floatToProgress(SLT_DEFAULT_C, -5f, 0.1f));
        tvSlt.setText(fmt1(SLT_DEFAULT_C));
        sbSlt.setOnSeekBarChangeListener(simpleListener(progress -> {
            float val = progressToFloat(progress, -5f, 0.1f);
            tvSlt.setText(fmt1(val));
        }));

        sbDt.setProgress(floatToProgress(DT_AIR_DEFAULT_C, 0f, 0.1f));
        tvDt.setText(fmt1(DT_AIR_DEFAULT_C));
        sbDt.setOnSeekBarChangeListener(simpleListener(progress -> {
            float val = progressToFloat(progress, 0f, 0.1f);
            tvDt.setText(fmt1(val));
        }));

        sbAmps.setProgress(floatToProgress(AMPS_DEFAULT_A, 0f, 0.1f));
        tvAmps.setText(fmt1(AMPS_DEFAULT_A));
        sbAmps.setOnSeekBarChangeListener(simpleListener(progress -> {
            float val = progressToFloat(progress, 0f, 0.1f);
            tvAmps.setText(fmt1(val));
        }));

        sbRla.setProgress(floatToProgress(RLA_DEFAULT_A, 1f, 0.1f));
        tvRla.setText(fmt1(RLA_DEFAULT_A));
        sbRla.setOnSeekBarChangeListener(simpleListener(progress -> {
            float val = progressToFloat(progress, 1f, 0.1f);
            tvRla.setText(fmt1(val));
        }));

        sbTevap.setProgress(floatToProgress(TEVAP_DEFAULT_C, -20f, 0.1f));
        tvTevap.setText(fmt1(TEVAP_DEFAULT_C));
        sbTevap.setOnSeekBarChangeListener(simpleListener(progress -> {
            float val = progressToFloat(progress, -20f, 0.1f);
            tvTevap.setText(fmt1(val));
        }));

        sbTcond.setProgress(floatToProgress(TCOND_DEFAULT_C, 20f, 0.1f));
        tvTcond.setText(fmt1(TCOND_DEFAULT_C));
        sbTcond.setOnSeekBarChangeListener(simpleListener(progress -> {
            float val = progressToFloat(progress, 20f, 0.1f);
            tvTcond.setText(fmt1(val));
        }));

        sbTamb.setProgress(floatToProgress(TAMB_DEFAULT_C, 0f, 0.1f));
        tvTamb.setText(fmt1(TAMB_DEFAULT_C));
        sbTamb.setOnSeekBarChangeListener(simpleListener(progress -> {
            float val = progressToFloat(progress, 0f, 0.1f);
            tvTamb.setText(fmt1(val));
        }));

        btn.setOnClickListener(v -> {
            String ref = spRef.getSelectedItem().toString();

            String ciudadSel = spCity.getSelectedItem().toString();
            int altitudSel = CIUDADES_ALT.get(ciudadSel);

            float suctionPsi = (float) progressToSuctionPsi(sbSuction.getProgress());
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

            int sLowCooling = triToInt(spLowCooling);
            int sIceEvapVal = triToInt(spIceEvap);
            int sLowAirflowVal = triToInt(spLowAirflow);
            int sDirtyCondVal = triToInt(spDirtyCond);
            int sCompHotVal = triToInt(spCompHot);
            int sFrostRestrVal = triToInt(spFrostRestr);
            int sCondFanOkVal = triToInt(spCondFanOk);
            int sEvapFanOkVal = triToInt(spEvapFanOk);

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
            ClaseDatosPythonJava.s_fan_cond_ok = sCondFanOkVal;
            ClaseDatosPythonJava.s_fan_evap_ok = sEvapFanOkVal;

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
                            "- Enfría poco: " + triToDebugText(sLowCooling) + "\n" +
                            "- Hielo evap: " + triToDebugText(sIceEvapVal) + "\n" +
                            "- Bajo flujo: " + triToDebugText(sLowAirflowVal) + "\n" +
                            "- Cond sucio: " + triToDebugText(sDirtyCondVal) + "\n" +
                            "- Vent cond OK: " + triToDebugText(sCondFanOkVal) + "\n" +
                            "- Turb evap OK: " + triToDebugText(sEvapFanOkVal) + "\n" +
                            "- Comp caliente: " + triToDebugText(sCompHotVal) + "\n" +
                            "- Escarcha restr: " + triToDebugText(sFrostRestrVal) + "\n\n" +
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
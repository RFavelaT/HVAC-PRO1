package edu.mx.tecnm.hvac_pro2;

import android.os.Bundle;
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
import java.util.List;
import java.util.Locale;

import edu.mx.tecnm.hvac_pro2.ClaseDatosPythonJava; // <-- AJUSTA si tu paquete/clase está en otro módulo

public class DiagnosticoFuzzyLogic extends AppCompatActivity {

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
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                cb.onChange(progress);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override public void onStopTrackingTouch(SeekBar seekBar) { }
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
        setContentView(R.layout.activity_diagnostico_fuzzy_logic);


        // Revisar si Python está iniciado
        if(!Python.isStarted()){
            Python.start(new AndroidPlatform(this));
        }
        final Python py = Python.getInstance();


        // Refrigerante
        Spinner spRef = findViewById(R.id.spRefrigerant);
        List<String> refs = Arrays.asList("R410A", "R22", "R32");
        ArrayAdapter<String> refAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                refs
        );
        spRef.setAdapter(refAdapter);

        // Spinner tri-state (No/Medio/Sí) con item grande
        List<String> triValues = Arrays.asList("No", "Medio", "Sí");
        ArrayAdapter<String> triAdapter = new ArrayAdapter<>(
                this,
                R.layout.item_spinner_tri,
                triValues
        );
        triAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        Spinner spLowCooling = findViewById(R.id.spLowCooling);
        Spinner spIceEvap = findViewById(R.id.spIceEvap);
        Spinner spLowAirflow = findViewById(R.id.spLowAirflow);
        Spinner spDirtyCond = findViewById(R.id.spDirtyCond);
        Spinner spCompHot = findViewById(R.id.spCompHot);
        Spinner spFrostRestr = findViewById(R.id.spFrostRestr);

        Spinner[] triSpinners = new Spinner[] {
                spLowCooling, spIceEvap, spLowAirflow, spDirtyCond, spCompHot, spFrostRestr
        };
        for (Spinner s : triSpinners) {
            s.setAdapter(triAdapter);
            s.setSelection(0); // default "No"
        }
        spLowCooling.setSelection(1); // default "Medio"

        // ===== SeekBars + TextViews =====

        // Succión PSI: 20..180 step 1  -> max=160 (ya en XML)
        SeekBar sbSuction = findViewById(R.id.sbSuctionPsi);
        TextView tvSuction = findViewById(R.id.tvSuctionPsiValue);
        sbSuction.setProgress(95); // 20+95=115
        tvSuction.setText("115");
        sbSuction.setOnSeekBarChangeListener(simpleListener(progress -> {
            int v = 20 + progress;
            tvSuction.setText(String.valueOf(v));
        }));

        // SLT: -5..40 step 0.1 -> max=450
        SeekBar sbSlt = findViewById(R.id.sbSuctionLineTemp);
        TextView tvSlt = findViewById(R.id.tvSuctionLineTempValue);
        sbSlt.setProgress(270); // 22.0
        tvSlt.setText("22.0");
        sbSlt.setOnSeekBarChangeListener(simpleListener(progress -> {
            float v = progressToFloat(progress, -5f, 0.1f);
            tvSlt.setText(fmt1(v));
        }));

        // ΔT aire: 0..20 step 0.1 -> max=200
        SeekBar sbDt = findViewById(R.id.sbDeltaTAir);
        TextView tvDt = findViewById(R.id.tvDeltaTAirValue);
        sbDt.setProgress(100); // 10.0
        tvDt.setText("10.0");
        sbDt.setOnSeekBarChangeListener(simpleListener(progress -> {
            float v = progressToFloat(progress, 0f, 0.1f);
            tvDt.setText(fmt1(v));
        }));

        // Amps: 0..30 step 0.1 -> max=300
        SeekBar sbAmps = findViewById(R.id.sbCompAmps);
        TextView tvAmps = findViewById(R.id.tvCompAmpsValue);
        sbAmps.setProgress(80); // 8.0
        tvAmps.setText("8.0");
        sbAmps.setOnSeekBarChangeListener(simpleListener(progress -> {
            float v = progressToFloat(progress, 0f, 0.1f);
            tvAmps.setText(fmt1(v));
        }));

        // RLA: 1..30 step 0.1 -> max=290
        SeekBar sbRla = findViewById(R.id.sbRla);
        TextView tvRla = findViewById(R.id.tvRlaValue);
        sbRla.setProgress(90); // 10.0
        tvRla.setText("10.0");
        sbRla.setOnSeekBarChangeListener(simpleListener(progress -> {
            float v = progressToFloat(progress, 1f, 0.1f);
            tvRla.setText(fmt1(v));
        }));

        // Tevap: -20..30 step 0.1 -> max=500
        SeekBar sbTevap = findViewById(R.id.sbTevap);
        TextView tvTevap = findViewById(R.id.tvTevapValue);
        sbTevap.setProgress(260); // 6.0
        tvTevap.setText("6.0");
        sbTevap.setOnSeekBarChangeListener(simpleListener(progress -> {
            float v = progressToFloat(progress, -20f, 0.1f);
            tvTevap.setText(fmt1(v));
        }));

        // Tcond: 20..80 step 0.1 -> max=600
        SeekBar sbTcond = findViewById(R.id.sbTcond);
        TextView tvTcond = findViewById(R.id.tvTcondValue);
        sbTcond.setProgress(250); // 45.0
        tvTcond.setText("45.0");
        sbTcond.setOnSeekBarChangeListener(simpleListener(progress -> {
            float v = progressToFloat(progress, 20f, 0.1f);
            tvTcond.setText(fmt1(v));
        }));

        // Tamb ext: 0..55 step 0.1 -> max=550
        SeekBar sbTamb = findViewById(R.id.sbTambExt);
        TextView tvTamb = findViewById(R.id.tvTambExtValue);
        sbTamb.setProgress(350); // 35.0
        tvTamb.setText("35.0");
        sbTamb.setOnSeekBarChangeListener(simpleListener(progress -> {
            float v = progressToFloat(progress, 0f, 0.1f);
            tvTamb.setText(fmt1(v));
        }));

        // ===== Botón Diagnosticar =====
        Button btn = findViewById(R.id.btnDiagnose);
        TextView tvResults = findViewById(R.id.tvResults);

        btn.setOnClickListener(v -> {
            // --------- Leer UI ----------
            String ref = spRef.getSelectedItem().toString();

            float suctionPsi = (20 + sbSuction.getProgress());
            float sltVal = progressToFloat(sbSlt.getProgress(), -5f, 0.1f);
            float dTairVal = progressToFloat(sbDt.getProgress(), 0f, 0.1f);

            float ampsVal = progressToFloat(sbAmps.getProgress(), 0f, 0.1f);
            float rlaVal = progressToFloat(sbRla.getProgress(), 1f, 0.1f);

            // Conversiones sugeridas:
            float pctRlaVal = (rlaVal > 0f) ? (ampsVal / rlaVal) * 100f : 0f;

            float tevVal = progressToFloat(sbTevap.getProgress(), -20f, 0.1f);
            float tcondVal = progressToFloat(sbTcond.getProgress(), 20f, 0.1f);
            float tambVal = progressToFloat(sbTamb.getProgress(), 0f, 0.1f);

            float dtCondExtVal = tcondVal - tambVal;
            if (dtCondExtVal < 0f) dtCondExtVal = 0f;

            // Síntomas tri-state -> 0/50/100
            int sLowCooling = triToInt(spLowCooling.getSelectedItem().toString());
            int sIceEvap = triToInt(spIceEvap.getSelectedItem().toString());
            int sLowAirflow = triToInt(spLowAirflow.getSelectedItem().toString());
            int sDirtyCond = triToInt(spDirtyCond.getSelectedItem().toString());
            int sCompHot = triToInt(spCompHot.getSelectedItem().toString());
            int sFrostRestr = triToInt(spFrostRestr.getSelectedItem().toString());

            // --------- ASIGNAR A ClaseDatosPythonJava ----------
            // (1) Campos legacy (por compatibilidad con tu app actual)
            ClaseDatosPythonJava.refrigerante = ref;
            ClaseDatosPythonJava.presionLbaja = fmt0(suctionPsi); // legacy string
            ClaseDatosPythonJava.tempLbaja = fmt1(sltVal);        // legacy string
            // tipo_equipo lo puedes asignar desde otra pantalla si existe:
            // ClaseDatosPythonJava.tipo_equipo = "MINISPLIT";

            // (2) Campos nuevos (UI actual)
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

            ClaseDatosPythonJava.s_enfria_poco = sLowCooling;
            ClaseDatosPythonJava.s_hielo_evaporador = sIceEvap;
            ClaseDatosPythonJava.s_flujo_aire_bajo = sLowAirflow;
            ClaseDatosPythonJava.s_condensador_sucio = sDirtyCond;
            ClaseDatosPythonJava.s_compresor_muy_caliente = sCompHot;
            ClaseDatosPythonJava.s_escarcha_localizada_restr = sFrostRestr;

            // (3) Opcional: limpia salida previa (si tu Python la llenará)
            ClaseDatosPythonJava.diagnostico = "";
            ClaseDatosPythonJava.tsat_evap_c = 0f;
            ClaseDatosPythonJava.sh_c = 0f;
            ClaseDatosPythonJava.nivel_carga = 4; // "Indefinido" hasta que Python responda

            // --------- (Demo) Mostrar captura en pantalla ----------
            String report =
                    "====== CAPTURA (guardada en ClaseDatosPythonJava) ======\n" +
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
                            "- Hielo evap: " + sIceEvap + "\n" +
                            "- Bajo flujo: " + sLowAirflow + "\n" +
                            "- Cond sucio: " + sDirtyCond + "\n" +
                            "- Comp caliente: " + sCompHot + "\n" +
                            "- Escarcha restr: " + sFrostRestr + "\n";

            tvResults.setText(report);

            // Aquí ya puedes llamar tu función Python (Chaquopy) que lea ClaseDatosPythonJava
            // y regrese diagnóstico para llenar ClaseDatosPythonJava.diagnostico, etc.


            PyObject mod = py.getModule("hvac_pro_fuzzy");
            PyObject result = mod.callAttr("run_diagnosis_from_java");




// Resultado texto (opcional si quieres usar retorno)
            String diag = result.toString();

// O usa directamente ClaseDatosPythonJava.diagnostico
            tvResults.setText(ClaseDatosPythonJava.diagnostico);
        });
    }
}




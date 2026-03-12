package edu.mx.tecnm.hvac_pro2;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

public class Captura_Datos extends AppCompatActivity {

    Spinner spinnerRefrigerantes;


    //Declarar objetos JAVA

    RadioButton radioButton_Inverter;
    RadioButton radioButton_Convencional;

    EditText editText_presion_baja;
    EditText editText_temperatura_baja;

    EditText editText_temperatura_alta;

    Button button_Diagnosticar;

    TextView textView_diagnostico;

    Button button_detalle_diagnostico;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_captura_datos);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        // Vinvular objetos JAVA con los objetos XML

        spinnerRefrigerantes = findViewById(R.id.spinnerRefrigerantes);


        radioButton_Inverter = findViewById(R.id.radioButton_Inverter);
        radioButton_Convencional = findViewById(R.id.radioButton_Convencional);

        editText_presion_baja = findViewById(R.id.editText_Presion_Baja);
        editText_temperatura_baja = findViewById(R.id.editText_Temperatura_Baja);
        editText_temperatura_alta = findViewById(R.id.editText_temperatura_Alta);

        button_Diagnosticar = findViewById(R.id.button_Diagnosticar);

        textView_diagnostico = findViewById(R.id.textView_diagnostico);

        button_detalle_diagnostico = findViewById(R.id.button_detalle_diagnostico);


        // Escuchador para el Spinner

        spinnerRefrigerantes.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                Toast.makeText(Captura_Datos.this, "Valor: " + position, Toast.LENGTH_SHORT).show();

                if (position == 1) {
                    ClaseDatosPythonJava.refrigerante = "R32";
                }
                if (position == 2) {
                    ClaseDatosPythonJava.refrigerante = "R22";
                }
                if (position == 3) {
                    ClaseDatosPythonJava.refrigerante = "R410a";
                }
                if (position == 4) {
                    ClaseDatosPythonJava.refrigerante = "Otro";
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });


        button_detalle_diagnostico.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Abrir actividad detalle de diagnóstico
                Intent intent = new Intent(Captura_Datos.this,
                        DetalleDeDiagnostico.class);
                startActivity(intent);

            }
        });


        // Instanciar objeto Python





        // Revisar si Python está iniciado
        if(!Python.isStarted()){
            Python.start(new AndroidPlatform(this));
        }
        final Python py = Python.getInstance();
        button_Diagnosticar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String tipo_equipo = "";
                String presionLbaja = "";
                String tempLbaja = "";
                String tempLalta = "";



                // Obtener tipo de equipo.

                if (radioButton_Inverter.isChecked()) {
                    tipo_equipo = "Inverter";
                } else if (radioButton_Convencional.isChecked()) {
                    tipo_equipo = "Convencional";
                }

                // Obtener presión de baja presión.
                presionLbaja = editText_presion_baja.getText().toString();

                // Obtener temperatura de baja presión.
                tempLbaja = editText_temperatura_baja.getText().toString();

                // Obtener temperatura de alta presión.
                tempLalta = editText_temperatura_alta.getText().toString();


// Guarda datos en la clase python java, que sirve como puente entre java y python.


                ClaseDatosPythonJava.tipo_equipo = tipo_equipo;
                ClaseDatosPythonJava.presionLbaja = presionLbaja;
                ClaseDatosPythonJava.tempLbaja = tempLbaja;
                ClaseDatosPythonJava.tempLalta = tempLalta;


;

                // Mandar llamar objeto python

                PyObject pyo = py.getModule("hvac_pro_adv4");
                // Llamar función Python con parámetros
                Object obj = pyo.callAttr("diagnosticar" );


                // Recuperar resultado de función Python

                textView_diagnostico.setText(ClaseDatosPythonJava.diagnostico);


            }
        });

    }//Fin de onCreate
}//Fin de Captura_Datos
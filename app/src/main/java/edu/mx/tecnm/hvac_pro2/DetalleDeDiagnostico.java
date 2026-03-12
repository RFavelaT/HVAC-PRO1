package edu.mx.tecnm.hvac_pro2;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DetalleDeDiagnostico extends AppCompatActivity {


TextView textView_detalle_diagnostico_descripcion;
ImageView imageView_detalle_diagnostico;

Button button_detalle_diagnostico_volver;

Button button_guardar_diagnostico;
Button button_ver_diagnosticos;


    FileOutputStream fileOutputStream;
    FileInputStream fileInputStream;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_detalle_de_diagnostico);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        textView_detalle_diagnostico_descripcion = findViewById(R.id.textView_detalle_diagnostico_descripcion);
        imageView_detalle_diagnostico = findViewById(R.id.imageView_detalle_diagnostico);

        button_detalle_diagnostico_volver = findViewById(R.id.button_detalle_diagnostico_volver);

        button_guardar_diagnostico = findViewById(R.id.button_guardar_diagnostico);
        button_ver_diagnosticos = findViewById(R.id.button_ver_diagnosticos);


button_detalle_diagnostico_volver.setOnClickListener(new View.OnClickListener() {
    @Override
    public void onClick(View v) {
        finish();
    }
});



button_guardar_diagnostico.setOnClickListener(new View.OnClickListener() {
    @Override
    public void onClick(View v) {

        // Guardar diagnóstico en archivo

        try {
            fileOutputStream = openFileOutput("diagnostico.txt", MODE_APPEND);

            String timestamp = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault())
                    .format(new Date());

            fileOutputStream.write(timestamp.getBytes(StandardCharsets.UTF_8));
            fileOutputStream.write("\n".getBytes(StandardCharsets.UTF_8));


            fileOutputStream.write(textView_detalle_diagnostico_descripcion.getText().toString().getBytes(StandardCharsets.UTF_8));
            fileOutputStream.write("\n".getBytes(StandardCharsets.UTF_8));
            fileOutputStream.write("\u25A0\u25A0\u25A0\u25A0\u25A0\u25A0\u25A0\u25A0\u25A0\u25A0\u25A0\u25A0\u25A0\u25A0\u25A0\u25A0\u25A0\u25A0\u25A0\u25A0\u25A0\u25A0".getBytes(StandardCharsets.UTF_8));

            fileOutputStream.write("\n\n".getBytes(StandardCharsets.UTF_8));


            fileOutputStream.close();

            // Mostrar mensaje al usuario

            Toast.makeText(DetalleDeDiagnostico.this, "Diagnóstico guardado", Toast.LENGTH_SHORT).show();

        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    } } );


// Escuchador para el botón de ver diagnósticos

button_ver_diagnosticos.setOnClickListener(new View.OnClickListener() {
    @Override
    public void onClick(View v) {

        // Abrir actividad de historial de diagnóstico

Intent intent = new Intent(DetalleDeDiagnostico.this, HistorialDeDiagnostico.class);

    startActivity(intent);
    }
});

        // Mostrar diagnóstico e imágen

        textView_detalle_diagnostico_descripcion.setText(ClaseDatosPythonJava.diagnostico);

        if (ClaseDatosPythonJava.nivel_carga == 1) {
            imageView_detalle_diagnostico.setImageResource(R.drawable.refrigerante_optimo);
        } else if (ClaseDatosPythonJava.nivel_carga == 2) {
            imageView_detalle_diagnostico.setImageResource(R.drawable.exceso_refrigerante);}
        else if (ClaseDatosPythonJava.nivel_carga == 3) {
            imageView_detalle_diagnostico.setImageResource(R.drawable.falta_refrigerante);}
        else if (ClaseDatosPythonJava.nivel_carga == 4) {
            imageView_detalle_diagnostico.setImageResource(R.drawable.otro_refrigerante);
        }




    }
}
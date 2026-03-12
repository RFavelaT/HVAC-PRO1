package edu.mx.tecnm.hvac_pro2;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HistorialDeDiagnostico extends AppCompatActivity {

    ListView listView;
    String listado[];

    FileInputStream fileInputStream;
    FileOutputStream fileOutputStream;


    Button button_historial_volver;
    Button button_eliminar_historial;

    // 🔹 NUEVO: lista y adapter a nivel de clase
    List<String> listElementsArrayList;
    ArrayAdapter<String> adapter;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_historial_de_diagnostico);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        listView = (ListView)findViewById(R.id.list_view_historial);
        button_historial_volver = findViewById(R.id.button_historial_volver);
        button_eliminar_historial = findViewById(R.id.button_eliminar_historial);

        button_historial_volver.setOnClickListener(v -> finish());

        button_eliminar_historial.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                try {

                    fileOutputStream = openFileOutput("diagnostico.txt", MODE_PRIVATE);

                    fileOutputStream.close();


                    Toast.makeText(getApplicationContext(),
                            "Registros eliminados",Toast.LENGTH_SHORT).show();



                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }


                listElementsArrayList.clear();    // borra todos los elementos
                adapter.notifyDataSetChanged();   // refresca el ListView visualmente



            }
        });




        obtenerDatos();

        // Crear una lista java para guardar los datos

        listElementsArrayList =
                new ArrayList<String>(Arrays.asList(listado));

        // Crear un adaptador para la lista

                adapter = new
                ArrayAdapter<String>
                (HistorialDeDiagnostico.this,
                        android.R.layout.simple_list_item_1,
                        listElementsArrayList);

        listView.setAdapter(adapter);

    }


    protected void obtenerDatos() {
        try {
            FileInputStream fis = openFileInput("diagnostico.txt");

            // LECTOR EN UTF-8
            InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
            BufferedReader reader = new BufferedReader(isr);

            String linea;
            List<String> lista = new ArrayList<>();

            while ((linea = reader.readLine()) != null) {
                lista.add(linea);
            }

            reader.close();

            listado = lista.toArray(new String[0]);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
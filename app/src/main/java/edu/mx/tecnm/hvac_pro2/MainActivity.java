package edu.mx.tecnm.hvac_pro2;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    Button button_iniciar;
    Button button_fuzzy;

    Button button_valores_optimos;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        button_iniciar = findViewById(R.id.button_iniciar);

        button_valores_optimos = findViewById(R.id.button_valores_optimos);

        button_fuzzy = findViewById(R.id.button_fuzzy);



        button_iniciar.setOnClickListener(view -> {
            Intent intent = new Intent(this, Captura_Datos.class);
            startActivity(intent);
        });



        button_valores_optimos.setOnClickListener(view -> {
            Intent intent = new Intent(this, ValoresOptimos.class);
            startActivity(intent);
        });


        button_fuzzy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, DiagnosticoFuzzyLogic2.class);
                startActivity(intent);

            }
        });
    }
}
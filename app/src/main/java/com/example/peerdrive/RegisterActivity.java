package com.example.peerdrive;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import okhttp3.*;

import java.io.IOException;

public class RegisterActivity extends AppCompatActivity {
    private String backendIP = "";
    private RadioGroup rgUserType;
    private EditText etName, etEmail, etPassword, etPlate, etModel, etColor;
    private LinearLayout layoutCarDetails;
    private Button btnRegister;
    private String userType = "Pasajero"; // Valor predeterminado

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        backendIP = getString(R.string.backendIP);
        Log.i("RegisterActivity", "Backend IP: " + backendIP);

        rgUserType = findViewById(R.id.rgUserType);
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etPlate = findViewById(R.id.etPlate);
        etModel = findViewById(R.id.etModel);
        etColor = findViewById(R.id.etColor);
        layoutCarDetails = findViewById(R.id.layoutCarDetails);
        btnRegister = findViewById(R.id.btnRegister);

        // Mostrar u ocultar detalles del coche según el tipo de usuario
        rgUserType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbConductor) {
                userType = "driver";
                layoutCarDetails.setVisibility(View.VISIBLE);
            } else {
                userType = "passenger";
                layoutCarDetails.setVisibility(View.GONE);
            }
        });

        // Manejo del botón de registro
        btnRegister.setOnClickListener(v -> registerUser());
    }

    private void registerUser() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos obligatorios", Toast.LENGTH_SHORT).show();
            return;
        }

        // Datos específicos del conductor
        String plate = null, model = null, color = null;
        if (userType.equals("driver")) {
            plate = etPlate.getText().toString().trim();
            model = etModel.getText().toString().trim();
            color = etColor.getText().toString().trim();
            if (plate.isEmpty() || model.isEmpty() || color.isEmpty()) {
                Toast.makeText(this, "Completa los detalles del coche", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // Crear JSON para la solicitud
        String json = createRegisterJson(name, email, password, plate, model, color);

        // Realizar solicitud al backend
        String url = backendIP + "/users/register";
        Log.i("RegisterActivity", "URL: " + url);

        OkHttpClient client = new OkHttpClient();
        RequestBody body = RequestBody.create(MediaType.parse("application/json"), json);
        Request request = new Request.Builder().url(url).post(body).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(RegisterActivity.this, "Error al conectarse al servidor", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                String responseMessage = response.body().string();

                if (response.isSuccessful()) {
                    runOnUiThread(() -> Toast.makeText(RegisterActivity.this, responseMessage, Toast.LENGTH_SHORT).show());
                } else {
                    runOnUiThread(() -> Toast.makeText(RegisterActivity.this, responseMessage, Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private String createRegisterJson(String name, String email, String password, String plate, String model, String color) {
        return userType.equals("driver") ?
                String.format("{\"type\":\"%s\",\"name\":\"%s\",\"email\":\"%s\",\"password\":\"%s\",\"carDetails\":{\"plate\":\"%s\",\"model\":\"%s\",\"color\":\"%s\"}}",
                        userType, name, email, password, plate, model, color) :
                String.format("{\"type\":\"%s\",\"name\":\"%s\",\"email\":\"%s\",\"password\":\"%s\"}",
                        userType, name, email, password);
    }
}

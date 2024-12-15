package com.example.peerdrive;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import okhttp3.*;

import java.io.IOException;

public class LoginActivity extends AppCompatActivity {
    private TextView tvRegister;
    private EditText etEmail, etPassword;
    private Button btnLogin;
    private String backendIP = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        backendIP = getString(R.string.backendIP);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegister = findViewById(R.id.tvRegister);


        btnLogin.setOnClickListener(v -> loginUser());

        tvRegister.setOnClickListener(v -> {
            // Aquí rediriges a la pantalla de registro
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    private void loginUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Por favor ingresa correo y contraseña", Toast.LENGTH_SHORT).show();
            return;
        }

        // Crear el JSON para la solicitud
        String json = String.format("{\"email\":\"%s\",\"password\":\"%s\"}", email, password);

        // Realizar solicitud al backend
        String url = backendIP + "/users/login";
        OkHttpClient client = new OkHttpClient();
        RequestBody body = RequestBody.create(MediaType.parse("application/json"), json);
        Request request = new Request.Builder().url(url).post(body).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(LoginActivity.this, "Error al conectarse al servidor", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    // Leer el mensaje del backend
                    String successMessage = response.body().string();
                    Log.i("LoginActivity", "Success message: " + successMessage);

                    try {
                        // Parsear el JSON con Gson
                        Gson gson = new Gson();
                        JsonObject jsonObject = gson.fromJson(successMessage, JsonObject.class);

                        // Extraer datos del usuario
                        JsonObject userObject = jsonObject.getAsJsonObject("user");
                        String name = userObject.get("name").getAsString();
                        String type = userObject.get("type").getAsString();

                        saveUserSession(name, type);

                        runOnUiThread(() -> {
                            Toast.makeText(LoginActivity.this, "Bienvenido, " + name, Toast.LENGTH_SHORT).show();
                            // Redirigir al usuario al dashboard (Conductor o Pasajero)
                            Intent intent = new Intent(LoginActivity.this, RouteActivity.class);
                            startActivity(intent);
                            finish();
                        });
                    } catch (Exception e) {
                        runOnUiThread(() -> Toast.makeText(LoginActivity.this, "Error al analizar la respuesta del servidor", Toast.LENGTH_SHORT).show());
                        Log.e("LoginActivity", "Error al parsear el JSON", e);
                    }
                } else {
                    // Leer el mensaje de error
                    String errorMessage = response.body().string();
                    runOnUiThread(() -> Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void saveUserSession(String name, String userType) {
        String nameSharedPreferences = getString(R.string.nameSharedPreferences);
        String namePreferences = getString(R.string.namePreferences);
        String typePreferences = getString(R.string.typePreferences);
        String isLoggedInPreferences = getString(R.string.isLoggedInPreferences);

        SharedPreferences sharedPreferences = getSharedPreferences(nameSharedPreferences, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(namePreferences, name);
        editor.putString(typePreferences, userType);
        editor.putBoolean(isLoggedInPreferences, true);
        editor.apply();
    }

}




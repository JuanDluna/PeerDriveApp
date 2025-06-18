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

        String json = String.format("{\"email\":\"%s\",\"password\":\"%s\"}", email, password);
        Log.i("LoginActivity", "JSON enviado: " + json);

        String url = backendIP + "/usuarios/login";
        Log.i("LoginActivity", "URL de login: " + url);

        OkHttpClient client = new OkHttpClient();
        RequestBody body = RequestBody.create(MediaType.parse("application/json"), json);
        Request request = new Request.Builder().url(url).post(body).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("LoginActivity", "Fallo de conexión con el backend", e);
                runOnUiThread(() ->
                        Toast.makeText(LoginActivity.this, "Error al conectarse al servidor", Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body().string();
                Log.i("LoginActivity", "Código HTTP: " + response.code());
                Log.i("LoginActivity", "Respuesta del servidor: " + responseBody);

                if (response.isSuccessful()) {
                    try {
                        Gson gson = new Gson();
                        JsonObject jsonObject = gson.fromJson(responseBody, JsonObject.class);

                        if (!jsonObject.has("user")) {
                            runOnUiThread(() -> Toast.makeText(LoginActivity.this, "Respuesta inválida del servidor", Toast.LENGTH_SHORT).show());
                            Log.e("LoginActivity", "Falta el campo 'user' en la respuesta.");
                            return;
                        }

                        JsonObject userObject = jsonObject.getAsJsonObject("user");
                        String name = userObject.get("nombre").getAsString();
                        String type = userObject.get("tipo_usuario").getAsString();
                        String UID = userObject.get("id_usuario").getAsString();

                        saveUserSession(UID, name, type);

                        runOnUiThread(() -> {
                            Toast.makeText(LoginActivity.this, "Bienvenido, " + name, Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(LoginActivity.this, RouteActivity.class);
                            startActivity(intent);
                            finish();
                        });
                    } catch (Exception e) {
                        Log.e("LoginActivity", "Error al parsear el JSON", e);
                        runOnUiThread(() -> Toast.makeText(LoginActivity.this, "Error al procesar los datos", Toast.LENGTH_SHORT).show());
                    }
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(LoginActivity.this, "Error: " + responseBody, Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    private void saveUserSession(String UID, String name, String userType) {
        String nameSharedPreferences = getString(R.string.nameSharedPreferences);
        String userIdPreferences = getString(R.string.userIdPreferences);
        String namePreferences = getString(R.string.namePreferences);
        String typePreferences = getString(R.string.typePreferences);
        String isLoggedInPreferences = getString(R.string.isLoggedInPreferences);

        SharedPreferences sharedPreferences = getSharedPreferences(nameSharedPreferences, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(userIdPreferences, UID);
        editor.putString(namePreferences, name);
        editor.putString(typePreferences, userType);
        editor.putBoolean(isLoggedInPreferences, true);
        editor.apply();
    }
}

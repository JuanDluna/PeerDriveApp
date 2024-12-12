package com.example.peerdrive;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvRegister;

    private LoginService loginService = new LoginService(); // Servicio simulado

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegister = findViewById(R.id.tvRegister);

        btnLogin.setOnClickListener(v -> handleLogin());
        tvRegister.setOnClickListener(v -> {
            // Aquí rediriges a la pantalla de registro
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    private void handleLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean isLoggedIn = loginService.login(email, password);
        if (isLoggedIn) {
            Toast.makeText(this, "Inicio de sesión exitoso", Toast.LENGTH_SHORT).show();
            // Redirige a la siguiente pantalla
            Log.d("LoginActivity", "Usuario autenticado con éxito.");
        } else {
            Toast.makeText(this, "Credenciales inválidas", Toast.LENGTH_SHORT).show();
            Log.w("LoginActivity", "Error al autenticar el usuario.");
        }
    }
}

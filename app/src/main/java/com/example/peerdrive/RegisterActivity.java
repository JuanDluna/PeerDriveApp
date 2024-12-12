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

public class RegisterActivity extends AppCompatActivity {

    private EditText etName, etEmail, etPassword, etConfirmPassword;
    private Button btnRegister;
    private TextView tvLogin;

    private RegisterService registerService = new RegisterService(); // Servicio simulado

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);
        tvLogin = findViewById(R.id.tvLogin);

        btnRegister.setOnClickListener(v -> handleRegister());
        tvLogin.setOnClickListener(v -> {
            // Redirigir a la pantalla de login
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
        });
    }

    private void handleRegister() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!registerService.validatePassword(password, confirmPassword)) {
            Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean isRegistered = registerService.register(email, password);
        if (isRegistered) {
            Toast.makeText(this, "Registro exitoso", Toast.LENGTH_SHORT).show();
            Log.d("RegisterActivity", "Usuario registrado con éxito.");
            // Redirigir a la pantalla de login
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
        } else {
            Toast.makeText(this, "El usuario ya existe", Toast.LENGTH_SHORT).show();
            Log.w("RegisterActivity", "Intento de registro fallido: usuario ya existente.");
        }
    }
}

package com.example.peerdrive;

public class LoginService {
    // Simulación de credenciales (esto sería manejado por el backend)
    private static final String MOCK_EMAIL = "test@example.com";
    private static final String MOCK_PASSWORD = "password123";

    public boolean login(String email, String password) {
        return MOCK_EMAIL.equals(email) && MOCK_PASSWORD.equals(password);
    }
}

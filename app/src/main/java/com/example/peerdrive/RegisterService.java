package com.example.peerdrive;

import java.util.HashMap;

public class RegisterService {
    private HashMap<String, String> userDatabase = new HashMap<>();

    public boolean register(String email, String password) {
        if (userDatabase.containsKey(email)) {
            return false; // El usuario ya existe
        }
        userDatabase.put(email, password);
        return true;
    }

    public boolean validatePassword(String password, String confirmPassword) {
        return password.equals(confirmPassword);
    }
}

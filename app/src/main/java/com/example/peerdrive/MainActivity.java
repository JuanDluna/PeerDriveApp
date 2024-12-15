package com.example.peerdrive;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onStart() {
        super.onStart();
        String nameSharedPreferences = getString(R.string.nameSharedPreferences);
        String isLogginPreferences = getString(R.string.isLoggedInPreferences);


        SharedPreferences sharedPreferences = getSharedPreferences(nameSharedPreferences, MODE_PRIVATE);
        boolean isLoggedIn = sharedPreferences.getBoolean(isLogginPreferences, false);
        if (isLoggedIn) {
            Intent intent = new Intent(MainActivity.this, RouteActivity.class);
            startActivity(intent);
            finish();
        }else{
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        }
    }


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

    }
}
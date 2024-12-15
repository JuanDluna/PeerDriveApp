package com.example.peerdrive;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class PassengerFragment extends Fragment {

    private LinearLayout scrollRouteList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_passenger, container, false);

        scrollRouteList = view.findViewById(R.id.scrollRouteList);

        // Agregar rutas (esto debe reemplazarse por datos dinámicos del backend)
        for (int i = 1; i <= 5; i++) {
            TextView route = new TextView(getContext());
            route.setText("Ruta " + i + ": Conductor X - Tarifa: $100");
            route.setPadding(16, 16, 16, 16);
            route.setOnClickListener(v -> {

                // Lógica para manejar el clic en la ruta
                route.setBackgroundColor(Color.GREEN);
            });
            scrollRouteList.addView(route);
        }

        return view;
    }
}

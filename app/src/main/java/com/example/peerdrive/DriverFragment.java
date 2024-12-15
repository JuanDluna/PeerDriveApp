package com.example.peerdrive;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class DriverFragment extends Fragment {

    private EditText etPassengers, etFare;
    private Button btnStartCountdown, btnStartTrip;
    private TextView tvCountdown;
    private CountDownTimer countDownTimer;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_driver, container, false);

        etPassengers = view.findViewById(R.id.etPassengers);
        etFare = view.findViewById(R.id.etFare);
        btnStartCountdown = view.findViewById(R.id.btnStartCountdown);
        btnStartTrip = view.findViewById(R.id.btnStartTrip);
        tvCountdown = view.findViewById(R.id.tvCountdown);

        btnStartCountdown.setOnClickListener(v -> startCountdown());
        btnStartTrip.setOnClickListener(v -> startTrip());

        return view;
    }

    private void startCountdown() {
        String passengers = etPassengers.getText().toString().trim();
        String fare = etFare.getText().toString().trim();

        if (passengers.isEmpty() || fare.isEmpty()) {
            Toast.makeText(getActivity(), "Por favor llena todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        btnStartCountdown.setVisibility(View.GONE);
        tvCountdown.setVisibility(View.VISIBLE);

        countDownTimer = new CountDownTimer(15 * 60 * 1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long minutes = millisUntilFinished / 60000;
                long seconds = (millisUntilFinished % 60000) / 1000;
                tvCountdown.setText(String.format("Tiempo restante: %02d:%02d", minutes, seconds));
            }

            @Override
            public void onFinish() {
                tvCountdown.setText("¡Tiempo finalizado!");
                btnStartTrip.setVisibility(View.VISIBLE);
            }
        };
        countDownTimer.start();
    }

    private void startTrip() {
        // Actualizar estado del viaje aquí
        Toast.makeText(getActivity(), "Viaje iniciado", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}

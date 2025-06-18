package com.example.peerdrive;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.RemoteMessage;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class DriverFragment extends Fragment {

    private LinearLayout SettingDriver, Countdown;
    private EditText etPassengers, etFare;
    private Button btnStartCountdown, btnStartTrip, btnCancelTrip;
    private TextView tvCountdown;
    private CountDownTimer countDownTimer;

    private DriverViewModel driverViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_driver, container, false);

        // Inicializa los contenedores
        SettingDriver = view.findViewById(R.id.llSettingDriver);
        Countdown = view.findViewById(R.id.llCountdown);

        // Inicializa las vistas de configuración
        etPassengers = view.findViewById(R.id.etPassengers);
        etFare = view.findViewById(R.id.etFare);
        btnStartCountdown = view.findViewById(R.id.btnStartCountdown);

        // Inicializa las vistas de cuenta regresiva
        tvCountdown = view.findViewById(R.id.tvCountdown);
        btnCancelTrip = view.findViewById(R.id.btnCancelTrip);
        btnStartTrip = view.findViewById(R.id.btnStartTrip);

        // Inicializa el ViewModel
        driverViewModel = new ViewModelProvider(requireActivity()).get(DriverViewModel.class);

        // Configura los botones
        btnStartCountdown.setOnClickListener(v -> startCountdown());
        btnCancelTrip.setOnClickListener(v -> cancelTrip());
        btnStartTrip.setOnClickListener(v -> startTrip());

        return view;
    }

    private void startCountdown() {
        // Validar los campos de entrada
        String passengersStr = etPassengers.getText().toString().trim();
        String fareStr = etFare.getText().toString().trim();

        if (passengersStr.isEmpty() || fareStr.isEmpty()) {
            Toast.makeText(getActivity(), "Por favor llena todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            int passengers = Integer.parseInt(passengersStr);
            double fare = Double.parseDouble(fareStr);

            if (passengers < 1 || passengers > 4) {
                Toast.makeText(getActivity(), "El número de pasajeros debe estar entre 1 y 4", Toast.LENGTH_SHORT).show();
                return;
            }

            if (fare <= 0) {
                Toast.makeText(getActivity(), "El monto debe ser mayor a cero", Toast.LENGTH_SHORT).show();
                return;
            }

            // Actualiza el ViewModel con los valores ingresados
            driverViewModel.setPassengers(passengers);
            driverViewModel.setFare(fare);

            // Registra el viaje en el backend
            registerTripInBackend(passengers, fare);

            // Envía una notificación a Firebase
            sendFirebaseNotification("Nuevo viaje iniciado", "El pasajero está esperando.");

            // Cambiar a la vista de cuenta regresiva
            SettingDriver.setVisibility(View.GONE);
            Countdown.setVisibility(View.VISIBLE);

            // Configurar el contador
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
        } catch (NumberFormatException e) {
            Toast.makeText(getActivity(), "Por favor ingresa valores numéricos válidos", Toast.LENGTH_SHORT).show();
        }
    }

    private void cancelTrip() {
        // Cancelar el contador
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        // Obtener el tripId del ViewModel
        String tripId = driverViewModel.getTripId().getValue();
        if (tripId == null || tripId.isEmpty()) {
            Toast.makeText(requireActivity(), "Error: ID del viaje no encontrado", Toast.LENGTH_SHORT).show();
            return;
        }

        // URL del backend
        String url = getString(R.string.backendIP) + "/viajes/cancelTrip";

        // Crear JSON para la solicitud
        String json = String.format("{\"tripId\":%s}", tripId);

        OkHttpClient client = new OkHttpClient();
        RequestBody body = RequestBody.create(
                okhttp3.MediaType.parse("application/json"), json);
        Request request = new Request.Builder()
                .url(url)
                .put(body)
                .build();

        // Hacer la solicitud al backend
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("DriverFragment", "Error al cancelar el viaje: " + e.getMessage());
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireActivity(), "Error al cancelar el viaje. Revisa tu conexión.", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireActivity(), "Viaje cancelado exitosamente.", Toast.LENGTH_SHORT).show();

                        // Enviar notificación de cancelación
                        sendFirebaseNotification("Viaje Cancelado", "Tu viaje ha sido cancelado.");

                        // Restaurar la vista inicial
                        Countdown.setVisibility(View.GONE);
                        SettingDriver.setVisibility(View.VISIBLE);
                        btnStartCountdown.setVisibility(View.VISIBLE);
                        etPassengers.setText("");
                        etFare.setText("");
                    });
                } else {
                    String errorMessage = response.body() != null ? response.body().string() : "Error desconocido";
                    Log.e("DriverFragment", "Error al cancelar el viaje: " + errorMessage);
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(requireActivity(), "Error del servidor: " + errorMessage, Toast.LENGTH_SHORT).show());
                }
            }
        });
    }


    private void startTrip() {
        // Aquí puedes añadir lógica adicional para manejar el inicio del viaje
        Toast.makeText(getActivity(), "¡El viaje ha comenzado!", Toast.LENGTH_SHORT).show();

        // Enviar notificación al iniciar el viaje
        sendFirebaseNotification("Viaje Iniciado", "Tu viaje ha comenzado. ¡Disfruta el trayecto!");
    }


    private void registerTripInBackend(int passengers, double fare) {
        if (getActivity() instanceof RouteActivity) {
            ((RouteActivity) getActivity()).registerTrip(passengers, fare);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }

    private void sendFirebaseNotification(String title, String message) {
        // Aquí configuras y envías la notificación de Firebase
        FirebaseMessaging.getInstance().send(
                new RemoteMessage.Builder("your_notification_topic")
                        .setMessageId(Integer.toString((int) System.currentTimeMillis()))
                        .addData("title", title)
                        .addData("message", message)
                        .build());
    }
}

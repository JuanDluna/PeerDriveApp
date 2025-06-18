package com.example.peerdrive;

import android.content.Context;
import android.content.SharedPreferences;
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

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

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
    private TextView countDown;
    private CountDownTimer countDownTimer;

    private DriverViewModel driverViewModel;
    private static final String TAG = "DriverFragment";

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
        countDown = view.findViewById(R.id.tvCountdown);
        btnCancelTrip = view.findViewById(R.id.btnCancelTrip);
        btnStartTrip = view.findViewById(R.id.btnStartTrip);

        // Inicializa el ViewModel
        driverViewModel = new ViewModelProvider(requireActivity()).get(DriverViewModel.class);

        // Configura los botones
        btnStartCountdown.setOnClickListener(v -> startCountdown());
        btnCancelTrip.setOnClickListener(v -> cancelTrip());
        btnStartTrip.setOnClickListener(v -> startTrip());

        // Suscribirse al topic de notificaciones
        FirebaseMessaging.getInstance().subscribeToTopic("rutas")
                .addOnCompleteListener(task -> {
                    String msg = "Suscrito al topic";
                    if (!task.isSuccessful()) {
                        msg = "No se pudo suscribirse";
                    }
                    Log.d(TAG, msg);
                    Toast.makeText(requireActivity(), msg, Toast.LENGTH_SHORT).show();
                });

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

            // Validar origen y destino desde el ViewModel
            LatLng origin = driverViewModel.getOrigin().getValue();
            LatLng destination = driverViewModel.getDestination().getValue();
            if (origin == null || destination == null) {
                Toast.makeText(getActivity(), "Seleccione origen y destino", Toast.LENGTH_SHORT).show();
                return;
            }

            // Obtener id_conductor desde SharedPreferences
            SharedPreferences sharedPreferences = requireActivity()
                    .getSharedPreferences(getString(R.string.nameSharedPreferences), Context.MODE_PRIVATE);
            String driverId = sharedPreferences.getString(getString(R.string.userIdPreferences), "");
            Log.d(TAG, "Driver ID from SharedPreferences: " + (driverId.isEmpty() ? "EMPTY" : driverId));
            if (driverId.isEmpty()) {
                Toast.makeText(getActivity(), "Error: ID del conductor no encontrado", Toast.LENGTH_SHORT).show();
                return;
            }
            // Validar que driverId sea un UUID
            try {
                UUID.fromString(driverId);
                Log.d(TAG, "Driver ID is valid UUID: " + driverId);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "ID del conductor inválido: " + driverId);
                Toast.makeText(getActivity(), "Error: ID del conductor inválido", Toast.LENGTH_SHORT).show();
                return;
            }

            // Actualiza el ViewModel con los valores ingresados
            driverViewModel.setPassengers(passengers);
            driverViewModel.setFare(fare);

            // Registra el viaje en el backend
            registerTripInBackend(driverId, origin, destination, passengers, fare);

        } catch (NumberFormatException e) {
            Toast.makeText(getActivity(), "Por favor ingresa valores numéricos válidos", Toast.LENGTH_SHORT).show();
        }
    }

    private void cancelTrip() {
        // Cancelar el contador
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }

        // Obtener el tripId del ViewModel
        String tripId = driverViewModel.getTripId().getValue();

        Log.d(TAG, "tripId: " + tripId);

        if (tripId == null || tripId.isEmpty()) {
            Toast.makeText(requireActivity(), "Error: ID del viaje no encontrado", Toast.LENGTH_SHORT).show();
            resetUI();
            return;
        }

        // URL del backend
        String url = getString(R.string.backendIP) + "/viajes/cancelTrip";

        // Crear JSON para la solicitud
        String json = String.format("{\"tripId\":\"%s\"}", tripId);
        Log.d(TAG, "Enviando JSON a /viajes/cancelTrip: " + json);

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
                Log.e(TAG, "Error al cancelar el viaje: " + e.getMessage());
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireActivity(), "Error al cancelar el viaje. Revisa tu conexión.", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseData = response.body() != null ? response.body().string() : "";
                Log.i(TAG, "Respuesta de /viajes/cancelTrip (HTTP " + response.code() + "): " + responseData);
                if (response.isSuccessful()) {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireActivity(), "Viaje cancelado exitosamente.", Toast.LENGTH_SHORT).show();
                        resetUI();
                    });
                } else {
                    try {
                        Gson gson = new Gson();
                        JsonObject errorJson = gson.fromJson(responseData, JsonObject.class);
                        String errorMessage = errorJson.has("error") && errorJson.get("error").isJsonObject() ?
                                errorJson.getAsJsonObject("error").get("message").getAsString() : responseData;
                        Log.e(TAG, "Error al cancelar el viaje: " + errorMessage);
                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(requireActivity(), "Error del servidor: " + errorMessage, Toast.LENGTH_LONG).show());
                    } catch (Exception e) {
                        Log.e(TAG, "Error al parsear error: " + responseData);
                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(requireActivity(), "Error del servidor: " + responseData, Toast.LENGTH_LONG).show());
                    }
                }
            }
        });
    }

    private void startTrip() {
        // Obtener el tripId del ViewModel
        String tripId = driverViewModel.getTripId().getValue();
        if (tripId == null || tripId.isEmpty()) {
            Toast.makeText(requireActivity(), "Error: ID del viaje no encontrado", Toast.LENGTH_SHORT).show();
            return;
        }

        // URL del backend para iniciar el viaje
        String url = getString(R.string.backendIP) + "/viajes/" + tripId;

        // Crear JSON para la solicitud
        String json = "{\"estado\":\"en_progreso\"}";
        Log.d(TAG, "Enviando JSON a /viajes/" + tripId + ": " + json);

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
                Log.e(TAG, "Error al iniciar el viaje: " + e.getMessage());
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireActivity(), "Error al iniciar el viaje. Revisa tu conexión.", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseData = response.body() != null ? response.body().string() : "";
                Log.i(TAG, "Respuesta de /viajes/" + tripId + " (HTTP " + response.code() + "): " + responseData);
                if (response.isSuccessful()) {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireActivity(), "¡El viaje ha comenzado!", Toast.LENGTH_SHORT).show();
                        resetUI();
                    });
                } else {
                    try {
                        Gson gson = new Gson();
                        JsonObject errorJson = gson.fromJson(responseData, JsonObject.class);
                        String errorMessage = errorJson.has("error") && errorJson.get("error").isJsonObject() ?
                                errorJson.getAsJsonObject("error").get("message").getAsString() : responseData;
                        Log.e(TAG, "Error al iniciar el viaje: " + errorMessage);
                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(requireActivity(), "Error del servidor: " + errorMessage, Toast.LENGTH_LONG).show());
                    } catch (Exception e) {
                        Log.e(TAG, "Error al parsear error: " + responseData);
                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(requireActivity(), "Error del servidor: " + responseData, Toast.LENGTH_LONG).show());
                    }
                }
            }
        });
    }

    private void registerTripInBackend(String driverId, LatLng origin, LatLng destination, int passengers, double fare) {
        // URL del backend
        String url = getString(R.string.backendIP) + "/viajes/addTrip";

        // Formatear fecha explícitamente
        String formattedDate = DateTimeFormatter.ISO_INSTANT.format(Instant.now());

        // Crear objeto JSON para depuración
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("driverId", driverId);
        JsonObject originJson = new JsonObject();
        originJson.addProperty("lat", origin.latitude);
        originJson.addProperty("lng", origin.longitude);
        jsonObject.add("origin", originJson);
        JsonObject destinationJson = new JsonObject();
        destinationJson.addProperty("lat", destination.latitude);
        destinationJson.addProperty("lng", destination.longitude);
        jsonObject.add("destination", destinationJson);
        jsonObject.addProperty("fare", fare);
        jsonObject.addProperty("num_pasajeros", passengers);
        jsonObject.addProperty("schedule", formattedDate);

        // Convertir a string para la solicitud
        String json = jsonObject.toString();
        Log.i(TAG, "Enviando JSON a /viajes/addTrip: " + json);

        OkHttpClient client = new OkHttpClient();
        RequestBody body = RequestBody.create(
                okhttp3.MediaType.parse("application/json"), json);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        // Hacer la solicitud al backend
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Error al registrar el viaje: " + e.getMessage());
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireActivity(), "Error al registrar el viaje. Revisa tu conexión.", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseData = response.body() != null ? response.body().string() : "";
                Log.i(TAG, "Respuesta de /viajes/addTrip (HTTP " + response.code() + "): " + responseData);

                if (response.isSuccessful()) {
                    try {
                        Gson gson = new Gson();
                        JsonObject responseJson = gson.fromJson(responseData, JsonObject.class);
                        if (responseJson.has("tripId") && !responseJson.get("tripId").isJsonNull()) {
                            String tripId = responseJson.get("tripId").getAsString();
                            requireActivity().runOnUiThread(() -> {
                                driverViewModel.setTripId(tripId);
                                // Cambiar a la vista de cuenta regresiva
                                SettingDriver.setVisibility(View.GONE);
                                Countdown.setVisibility(View.VISIBLE);
                                // Configurar el contador
                                countDownTimer = new CountDownTimer(15 * 60 * 1000, 1000) {
                                    @Override
                                    public void onTick(long millisUntilFinished) {
                                        long minutes = millisUntilFinished / 60000;
                                        long seconds = (millisUntilFinished % 60000) / 1000;
                                        countDown.setText(String.format("Tiempo restante: %02d:%02d", minutes, seconds));
                                    }

                                    @Override
                                    public void onFinish() {
                                        countDown.setText("¡Tiempo finalizado!");
                                        btnStartTrip.setVisibility(View.VISIBLE);
                                    }
                                };
                                countDownTimer.start();
                            });
                        } else {
                            Log.e(TAG, "Respuesta del backend sin tripId: " + responseData);
                            requireActivity().runOnUiThread(() ->
                                    Toast.makeText(requireActivity(), "Error: No se recibió ID del viaje", Toast.LENGTH_SHORT).show());
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error al parsear respuesta: " + responseData, e);
                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(requireActivity(), "Error al procesar respuesta del servidor", Toast.LENGTH_SHORT).show());
                    }
                } else {
                    try {
                        Gson gson = new Gson();
                        JsonObject errorJson = gson.fromJson(responseData, JsonObject.class);
                        String errorMessage = errorJson.has("error") && errorJson.get("error").isJsonObject() ?
                                errorJson.getAsJsonObject("error").get("message").getAsString() : responseData;
                        Log.e(TAG, "Error al registrar el viaje (HTTP " + response.code() + "): " + errorMessage);
                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(requireActivity(), "Error del servidor: " + errorMessage, Toast.LENGTH_LONG).show());
                    } catch (Exception e) {
                        Log.e(TAG, "Error al parsear error: " + responseData);
                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(requireActivity(), "Error del servidor: " + responseData, Toast.LENGTH_LONG).show());
                    }
                }
            }
        });
    }

    private void resetUI() {
        // Restaurar la vista inicial
        Countdown.setVisibility(View.GONE);
        SettingDriver.setVisibility(View.VISIBLE);
        btnStartCountdown.setVisibility(View.VISIBLE);
        etPassengers.setText("");
        etFare.setText("");
        driverViewModel.setTripId(null); // Limpiar tripId
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
    }

}
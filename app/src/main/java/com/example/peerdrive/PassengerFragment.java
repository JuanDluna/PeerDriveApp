package com.example.peerdrive;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
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
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.time.Instant;

public class PassengerFragment extends Fragment {

    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 100; // Código de solicitud de permisos para las notificaciones


    private View actualView;
    private View scrollContainer;
    private LinearLayout scrollRouteList;
    private LinearLayout layoutDriverDetails;
    private PassengerViewModel passengerViewModel;
    private static final String TAG = "PassengerFragment";
    private String remainingTime;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_passenger, container, false);

        FirebaseMessaging.getInstance().subscribeToTopic("rutas")
                .addOnCompleteListener(task -> {
                    String msg = "Suscrito al topic";
                    if (!task.isSuccessful()) {
                        msg = "No se pudo suscribirse";
                    }
                    Log.d(TAG, msg);
                    Toast.makeText(requireActivity(), msg, Toast.LENGTH_SHORT).show();
                });


        actualView = view;

        scrollRouteList = view.findViewById(R.id.scrollRouteList);
        layoutDriverDetails = view.findViewById(R.id.driverDetails);
        passengerViewModel = new ViewModelProvider(requireActivity()).get(PassengerViewModel.class); // Compartir el ViewModel con la actividad principal

        // Establecer altura máxima del ScrollView
        scrollContainer = view.findViewById(R.id.scrollRouteListContainer);

        observePassengerViewModel();

        actualView.setVisibility(View.GONE);
        return view;
    }

    private void observePassengerViewModel() {
        passengerViewModel.getOrigin().observe(getViewLifecycleOwner(), origin -> {
            if (passengerViewModel.isRouteReady()) {
                fetchAvailableRoutes();
            }
        });

        passengerViewModel.getDestination().observe(getViewLifecycleOwner(), destination -> {
            if (passengerViewModel.isRouteReady()) {
                fetchAvailableRoutes();
            }
        });
    }

    private void fetchAvailableRoutes() {
        LatLng origin = passengerViewModel.getOrigin().getValue();
        LatLng destination = passengerViewModel.getDestination().getValue();

        if (origin == null || destination == null) {
            Log.e(TAG, "Origen o destino no están configurados");
            return;
        }

        String url = getString(R.string.backendIP) + "/viajes/find";
        String json = String.format(
                "{\"origin\":{\"lat\":%.6f,\"lng\":%.6f},\"destination\":{\"lat\":%.6f,\"lng\":%.6f},\"maxFare\":%.2f}",
                origin.latitude, origin.longitude, destination.latitude, destination.longitude, 1000.0 // Cambia 100.0 según el máximo permitido
        );

        OkHttpClient client = new OkHttpClient();
        RequestBody body = RequestBody.create(
                okhttp3.MediaType.parse("application/json"), json);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Error al buscar rutas: " + e.getMessage());
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireActivity(), "Error al buscar rutas", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    Log.i(TAG, "Rutas recibidas: " + responseData);
                    Gson gson = new Gson();
                    JsonArray routesArray = gson.fromJson(responseData, JsonArray.class);

                    requireActivity().runOnUiThread(() -> displayRoutes(routesArray));
                } else {
                    Log.e(TAG, "Error en el servidor: " + response.message());
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(requireActivity(), "No se encontraron rutas disponibles", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void displayRoutes(JsonArray routesArray) {
        scrollRouteList.removeAllViews(); // Limpiar lista anterior
        scrollRouteList.setOrientation(LinearLayout.VERTICAL);
        actualView.setVisibility(View.VISIBLE);

        // Referencia para el último botón mostrado
        final Button[] lastShownButton = {null};

        if (routesArray.size() == 0) {
            TextView tvNoRoutes = new TextView(requireActivity());
            tvNoRoutes.setText("No se encontraron rutas disponibles");
            tvNoRoutes.setTextColor(Color.RED);
            tvNoRoutes.setPadding(10, 10, 10, 10);
            tvNoRoutes.setTextSize(18);
            tvNoRoutes.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER);
            scrollRouteList.addView(tvNoRoutes);
            return;
        }

        for (int i = 0; i < routesArray.size(); i++) {
            JsonObject route = routesArray.get(i).getAsJsonObject();

            // ID del viaje
            String tripID = route.get("tripId").getAsString();
            Log.i("PassengerFragment", "tripID: " + tripID);

            // Datos del viaje
            double fare = route.get("fare").getAsDouble();
            JsonObject origin = route.getAsJsonObject("route").getAsJsonObject("origin");
            JsonObject destination = route.getAsJsonObject("route").getAsJsonObject("destination");

            // Detalles del conductor
            String driverName = route.get("driverName").getAsString();
            String phoneNumber = route.get("phoneNumber").getAsString();
            String driverCarPlate = route.getAsJsonObject("carDetails").get("plate").getAsString();
            String driverCarModel = route.getAsJsonObject("carDetails").get("model").getAsString();
            String driverCarColor = route.getAsJsonObject("carDetails").get("color").getAsString();

            // Crear vista de cada ruta
            View routeView = LayoutInflater.from(requireActivity()).inflate(R.layout.item_route, scrollRouteList, false);

            TextView tvDriverName = routeView.findViewById(R.id.tvDriverName);
            TextView tvFare = routeView.findViewById(R.id.tvFare);

            tvDriverName.setText("Conductor: " + driverName);
            tvFare.setText(String.format("Tarifa: $%.2f", fare));

            // Calcula el tiempo restante
            String scheduleISO = route.get("schedule").getAsString();
            TextView tvTimeRemaining = routeView.findViewById(R.id.tvTimeRemaining);
            startRemainingTimeUpdater(scheduleISO, tvTimeRemaining);

            if (tvTimeRemaining.getText().toString().equals("¡Tiempo agotado!")) {
                continue;
            }

            // Botón para confirmar asistencia
            Button btnConfirmAttendance = routeView.findViewById(R.id.btnConfirmAttendance);
            btnConfirmAttendance.setVisibility(View.GONE); // Ocultar por defecto

            // Acción para seleccionar una ruta
            routeView.setOnClickListener(v -> {
                // Ocultar el botón del último seleccionado (si existe)
                if (lastShownButton[0] != null) {
                    lastShownButton[0].setVisibility(View.GONE);
                }

                // Actualizar el mapa para destacar la nueva ruta
                highlightRouteOnMap(
                        new LatLng(origin.get("lat").getAsDouble(), origin.get("lng").getAsDouble()),
                        new LatLng(destination.get("lat").getAsDouble(), destination.get("lng").getAsDouble())
                );

                // Mostrar el botón de confirmación para esta ruta
                btnConfirmAttendance.setVisibility(View.VISIBLE);
                lastShownButton[0] = btnConfirmAttendance; // Guardar referencia del botón actual

                // Configurar acción del botón de confirmación
                btnConfirmAttendance.setOnClickListener(v1 -> {
                    showDriverDetails(driverName, phoneNumber, fare, driverCarPlate, driverCarModel, driverCarColor, tripID);
                    confirmAssistance(tripID);
                });
            });

            adjustHeight();

            // Añadir la vista de la ruta a la lista
            scrollRouteList.addView(routeView);
        }
    }

    private void startRemainingTimeUpdater(String scheduleISO, TextView tvTimeRemaining) {
        // Parsear la hora de salida a Instant
        Instant departureTime = Instant.parse(scheduleISO); // Hora en UTC
        long departureTimeMillis = departureTime.toEpochMilli(); // Convertir a milisegundos

        // Handler para actualizar el tiempo restante periódicamente
        Handler handler = new Handler();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                // Obtener la hora actual en milisegundos
                long currentTimeMillis = System.currentTimeMillis();

                // Calcular el tiempo restante, incluyendo los 15 minutos adicionales
                long timeRemaining = (departureTimeMillis + 15 * 60 * 1000) - currentTimeMillis;

                if (timeRemaining > 0) {
                    // Formatear el tiempo restante a hh:mm:ss
                     remainingTime = String.format("%02d:%02d:%02d",
                            (timeRemaining / 1000) / 3600,            // Horas
                            (timeRemaining / 1000) % 3600 / 60,      // Minutos
                            (timeRemaining / 1000) % 60);            // Segundos

                    // Actualizar el TextView en el hilo principal
                    tvTimeRemaining.setText("Tiempo restante: " + remainingTime);

                    // Volver a ejecutar el Runnable después de 1 segundo
                    handler.postDelayed(this, 1000);
                } else {
                    // Si el tiempo se ha agotado, mostrar mensaje
                    tvTimeRemaining.setText("¡Tiempo agotado!");
                    fetchAvailableRoutes();
                    handler.removeCallbacks(this); // Detener actualizaciones
                }
            }
        };

        // Ejecutar el Runnable por primera vez
        handler.post(runnable);
    }


    private void adjustHeight() {
        scrollContainer.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            int itemHeight = getResources().getDimensionPixelSize(R.dimen.route_item_height); // Dimensión de un ítem
            int maxHeight = 2 * itemHeight; // Máximo 2 ítems
            scrollContainer.getLayoutParams().height = Math.min(maxHeight, scrollContainer.getHeight());
            scrollContainer.requestLayout();
        });
    }

    private void highlightRouteOnMap(LatLng origin, LatLng destination) {
        if (requireActivity() instanceof RouteActivity) {
            RouteActivity routeActivity = (RouteActivity) requireActivity();
            routeActivity.highlightRouteOnMap(origin, destination);
        }
    }

    private void showDriverDetails(String driverName, String phoneNumber, double fare,
                                   String carPlate, String carModel, String carColor, String tripId) {


        if (layoutDriverDetails != null) {
            scrollRouteList.removeAllViews();
            scrollRouteList.setOrientation(LinearLayout.VERTICAL);
            scrollRouteList.addView(layoutDriverDetails);
            ((TextView) layoutDriverDetails.findViewById(R.id.tvDriverNameDetails)).setText(driverName);
            ((TextView) layoutDriverDetails.findViewById(R.id.tvPhoneNumberDetails)).setText(phoneNumber);
            ((TextView) layoutDriverDetails.findViewById(R.id.tvCarPlate)).setText("Placa: " + carPlate);
            ((TextView) layoutDriverDetails.findViewById(R.id.tvCarModel)).setText("Modelo: " + carModel);
            ((TextView) layoutDriverDetails.findViewById(R.id.tvCarColor)).setText("Color: " + carColor);
            ((TextView) layoutDriverDetails.findViewById(R.id.tvTimeRemaining)).setText("Tiempo restante:" + remainingTime);
            ((Button) layoutDriverDetails.findViewById(R.id.btnCancel)).setOnClickListener(v -> {
                cancelAssistant(tripId);
                layoutDriverDetails.setVisibility(View.GONE);
                fetchAvailableRoutes();
            });
            layoutDriverDetails.setOrientation(LinearLayout.VERTICAL);
            layoutDriverDetails.setVisibility(View.VISIBLE);
        }
    }

    private void confirmAssistance(String tripID) {
        SharedPreferences sharedPreferences = requireActivity()
                .getSharedPreferences(getString(R.string.nameSharedPreferences), Context.MODE_PRIVATE);
        String UID = sharedPreferences.getString(getString(R.string.userIdPreferences), "");

        if (UID.isEmpty() || tripID.isEmpty()) {
            Log.e("PassengerFragment", "UID o tripID están vacíos.");
            Toast.makeText(requireActivity(), "No se pudo confirmar la asistencia. Datos faltantes.", Toast.LENGTH_SHORT).show();
            return;
        }

        String url = getString(R.string.backendIP) + "/trips/addUserInTrip";
        String json = String.format("{\"tripId\":%s, \"userId\":\"%s\"}", tripID, UID);

        OkHttpClient client = new OkHttpClient();
        RequestBody body = RequestBody.create(
                okhttp3.MediaType.parse("application/json"), json);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("PassengerFragment", "Error al confirmar la asistencia: " + e.getMessage());
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireActivity(), "Error al confirmar la asistencia. Revisa tu conexión.", Toast.LENGTH_SHORT).show();
                    sendNotification("Error al confirmar asistencia", "Revisa tu conexión a Internet.");
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    Log.i("PassengerFragment", "Asistencia confirmada");
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireActivity(), "Asistencia confirmada exitosamente.", Toast.LENGTH_SHORT).show();
                        sendNotification("Confirmación exitosa", "Asistencia confirmada correctamente.");
                    });
                } else {
                    String errorMessage = response.body() != null ? response.body().string() : "Error desconocido";

                    Log.e("PassengerFragment", "Error al confirmar la asistencia: " + errorMessage);
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireActivity(), "Error del servidor: " + errorMessage, Toast.LENGTH_SHORT).show();
                        sendNotification("Error del servidor", errorMessage);
                    });
                }
            }
        });
    }



    public void cancelAssistant(String tripId) {
        layoutDriverDetails.setVisibility(View.GONE);
        fetchAvailableRoutes();
        SharedPreferences sharedPreferences = requireActivity()
                .getSharedPreferences(getString(R.string.nameSharedPreferences), Context.MODE_PRIVATE);
        String UID = sharedPreferences.getString(getString(R.string.userIdPreferences), "");

        if (UID.isEmpty() || tripId.isEmpty()) {
            Log.e("PassengerFragment", "UID o tripID están vacíos.");
            Toast.makeText(requireActivity(), "No se pudo confirmar la asistencia. Datos faltantes.", Toast.LENGTH_SHORT).show();
            return;
        }

        String url = getString(R.string.backendIP) + "/trips/cancelAssistant";
        String json = String.format("{\"tripId\":%s, \"userId\":\"%s\"}", tripId, UID);

        OkHttpClient client = new OkHttpClient();
        RequestBody body = RequestBody.create(
                okhttp3.MediaType.parse("application/json"), json);
        Request request = new Request.Builder()
                .url(url)
                .delete(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                JsonObject jsonObject = new Gson().fromJson(e.getMessage(), JsonObject.class);

                String errorMessage = jsonObject.get("error").getAsString();

                Log.e("PassengerFragment", "Error al cancelar la asistencia: " + errorMessage);
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireActivity(), "Error al cancelar la asistencia. Revisa tu conexión.", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    Log.i("PassengerFragment", "Asistencia cancelada");
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(requireActivity(), "Asistencia cancelada exitosamente.", Toast.LENGTH_SHORT).show());
                } else {
                    String errorMessage = response.body() != null ? response.body().string() : "Error desconocido";

                    Log.e("PassengerFragment", "Error al cancelar la asistencia: " + errorMessage);
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(requireActivity(), "Error del servidor: " + errorMessage, Toast.LENGTH_SHORT).show());
                }
            }
        });

    }


    private void sendNotification(String title, String message) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(requireActivity(), "default")
                .setSmallIcon(R.drawable.ic_notification) // Ajusta el drawable al nombre de tu recurso
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(requireActivity());

        if (ActivityCompat.checkSelfPermission(requireActivity(), android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            // Solicitar permisos si no están concedidos
            ActivityCompat.requestPermissions(requireActivity(), new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_REQUEST_CODE);
            return;
        }

        notificationManager.notify(0, builder.build());
    }

    // Código adicional para manejar la respuesta a la solicitud de permisos
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
        }
    }



}

package com.example.peerdrive;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class RouteActivity extends AppCompatActivity implements OnMapReadyCallback {
    private static final int OPTION_CURRENT_LOCATION = 1;
    private static final int OPTION_SELECT_ON_MAP = 2;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private GoogleMap googleMap;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LatLng originLatLng, destinationLatLng;
    private Marker originMarker, destinationMarker;
    private Polyline highlightedRoute; // Variable para almacenar la ruta resaltada

    private DriverViewModel driverViewModel;
    private PassengerViewModel passengerViewModel;

    private String userType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route);

        String nameSharedPreferences = getString(R.string.nameSharedPreferences);
        String typePreferences = getString(R.string.typePreferences);
        String userNamePreferences = getString(R.string.namePreferences);

        SharedPreferences sharedPreferences = getSharedPreferences(nameSharedPreferences, MODE_PRIVATE);
        userType = sharedPreferences.getString(typePreferences, "");
        String userName = sharedPreferences.getString(userNamePreferences, "");

        Log.i("RouteActivity", "userType: " + userType);

        driverViewModel = new ViewModelProvider(this).get(DriverViewModel.class);
        passengerViewModel = new ViewModelProvider(this).get(PassengerViewModel.class);

        if (userType.equals("conductor")) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, new DriverFragment())
                    .commit();
        } else if (userType.equals("pasajero")) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, new PassengerFragment())
                    .commit();
        } else {
            Toast.makeText(this, "Error: Tipo de usuario desconocido", Toast.LENGTH_SHORT).show();
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Bienvenido, " + userName);

        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), getString(R.string.google_maps_key));
        }

        // Configurar botones de opciones
        findViewById(R.id.OriginOptions).setOnClickListener(v -> showOptionsMenu(true));
        findViewById(R.id.DestinationOptions).setOnClickListener(v -> showOptionsMenu(false));

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        createFragment();
        setupAutocomplete();

        observeViewModel();
    }
    private void observeViewModel() {
        if (userType.equals("driver")){
            driverViewModel.getOrigin().observe(this, origin -> {
                Log.d("RouteActivity", "Origen actualizado: " + origin);
                updateMarker(origin, true);
                checkAndCalculateRoute();
            });

            driverViewModel.getDestination().observe(this, destination -> {
                Log.d("RouteActivity", "Destino actualizado: " + destination);
                updateMarker(destination, false);
                checkAndCalculateRoute();
            });

            driverViewModel.getPassengers().observe(this, passengers ->
                    Log.d("RouteActivity", "Pasajeros actualizados: " + passengers)
            );

            driverViewModel.getFare().observe(this, mount ->
                    Log.d("RouteActivity", "Monto actualizado: " + mount)
            );
        }else if(userType.equals("passenger")){
            passengerViewModel.getOrigin().observe(this, origin -> {
                updateMarker(origin, true);
                checkAndCalculateRoute();
                Log.d("RouteActivity", "Origen actualizado: " + origin);
            });
            passengerViewModel.getDestination().observe(this, destination -> {
                updateMarker(destination, false);
                checkAndCalculateRoute();
                Log.d("RouteActivity", "Destino actualizado: " + destination);
            });
        }

    }

    private void checkAndCalculateRoute() {
        LatLng origin;
        LatLng destination;
        if(userType.equals("conductor")) {
            origin = driverViewModel.getOrigin().getValue();
            destination = driverViewModel.getDestination().getValue();
        }else{
            origin = passengerViewModel.getOrigin().getValue();
            destination = passengerViewModel.getDestination().getValue();
        }

        if (origin != null && destination != null) {
            adjustCameraToBounds(origin, destination);
            calculateRoute();
        }
    }

    private void  showOptionsMenu(boolean isOrigin) {
        View anchor = isOrigin ? findViewById(R.id.OriginOptions) : findViewById(R.id.DestinationOptions);
        PopupMenu popupMenu = new PopupMenu(this, anchor);

        popupMenu.getMenu().add(0, OPTION_CURRENT_LOCATION, 0, "Usar ubicación actual");
        popupMenu.getMenu().add(0, OPTION_SELECT_ON_MAP, 1, "Seleccionar en el mapa");

        popupMenu.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case OPTION_CURRENT_LOCATION:
                    if (isOrigin) useCurrentLocation(true);
                    else useCurrentLocation(false);
                    return true;

                case OPTION_SELECT_ON_MAP:
                    if (isOrigin) selectOnMap(true);
                    else selectOnMap(false);
                    return true;

                default:
                    return false;
            }
        });

        popupMenu.show();
    }

    private void useCurrentLocation(boolean isOrigin) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationProviderClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                    if (isOrigin) setOrigin(latLng);
                    else setDestination(latLng);
                } else {
                    Toast.makeText(this, "No se pudo obtener la ubicación actual", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(this, "Permiso de ubicación no otorgado", Toast.LENGTH_SHORT).show();
        }
    }

    private void selectOnMap(boolean isOrigin) {
        Toast.makeText(this, "Selecciona un punto en el mapa", Toast.LENGTH_SHORT).show();

        googleMap.setOnMapClickListener(latLng -> {
            if (isOrigin) setOrigin(latLng);
            else setDestination(latLng);
            googleMap.setOnMapClickListener(null); // Deshabilitar el listener después de seleccionar
        });
    }

    private void setOrigin(LatLng latLng) {
        originLatLng = latLng;
        if (originMarker != null) {
            originMarker.remove();
        }
        originMarker = googleMap.addMarker(new MarkerOptions().position(latLng).title("Origen"));
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));

        if (userType.equals("conductor"))
            driverViewModel.setOrigin(latLng); // Actualizar el ViewModel
        else if (userType.equals("pasajero"))
            passengerViewModel.setOrigin(latLng); // Actualizar el ViewModel

        // Actualizar el campo de autocompletado
        updateAutocompleteField(R.id.autocompleteOrigin, latLng);
    }

    private void setDestination(LatLng latLng) {
        destinationLatLng = latLng;
        if (destinationMarker != null) {
            destinationMarker.remove();
        }
        destinationMarker = googleMap.addMarker(new MarkerOptions().position(latLng).title("Destino"));
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));

        if (userType.equals("conductor"))
            driverViewModel.setDestination(latLng); // Actualizar el ViewModel
        else if (userType.equals("pasajero"))
            passengerViewModel.setDestination(latLng); // Actualizar el ViewModel

        // Actualizar el campo de autocompletado
        updateAutocompleteField(R.id.autocompleteDestination, latLng);
    }
    private void updateAutocompleteField(int autocompleteFragmentId, LatLng latLng) {
        // Usar Geocoder para obtener la dirección
        Geocoder geocoder = new Geocoder(this);
        try {
            List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
            if (!addresses.isEmpty()) {
                String address = addresses.get(0).getAddressLine(0);
                AutocompleteSupportFragment autocompleteFragment =
                        (AutocompleteSupportFragment) getSupportFragmentManager().findFragmentById(autocompleteFragmentId);
                if (autocompleteFragment != null) {
                    EditText editText = autocompleteFragment.getView().findViewById(com.google.android.libraries.places.R.id.places_autocomplete_search_input);
                    if (editText != null) {
                        editText.setText(address); // Mostrar la dirección en el campo
                    }
                }
            }
        } catch (IOException e) {
            Log.e("Geocoder Error", "No se pudo obtener la dirección", e);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflar el menú en la barra superior
        getMenuInflater().inflate(R.menu.menu_route, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            logout();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void logout() {
        String nameSharedPreferences = getString(R.string.nameSharedPreferences);
        // Eliminar las preferencias guardadas
        SharedPreferences sharedPreferences = getSharedPreferences(nameSharedPreferences, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear(); // Borra todos los datos
        editor.apply();

        // Mostrar mensaje y redirigir al inicio de sesión
        Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(RouteActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    private void createFragment() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        this.googleMap = map;

        googleMap.setTrafficEnabled(true);
        googleMap.getUiSettings().setMyLocationButtonEnabled(false);
        googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style));

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            enableUserLocation();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private void enableUserLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        googleMap.setMyLocationEnabled(true);
        getCurrentLocation();
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationProviderClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15));
            } else {
                Toast.makeText(this, "No se pudo obtener la ubicación actual", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupAutocomplete() {
        // Origen
        AutocompleteSupportFragment originAutocomplete = (AutocompleteSupportFragment) getSupportFragmentManager().findFragmentById(R.id.autocompleteOrigin);
        if (originAutocomplete != null) {
            originAutocomplete.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG));
            originAutocomplete.setOnPlaceSelectedListener(new PlaceSelectionListener() {
                @Override
                public void onPlaceSelected(@NonNull Place place) {
                    setOrigin(place.getLatLng());
                }

                @Override
                public void onError(@NonNull Status status) {
                    Log.e("Place API Error", status.toString());
                }
            });
        }

        // Destino
        AutocompleteSupportFragment destinationAutocomplete = (AutocompleteSupportFragment) getSupportFragmentManager().findFragmentById(R.id.autocompleteDestination);
        if (destinationAutocomplete != null) {
            destinationAutocomplete.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG));
            destinationAutocomplete.setOnPlaceSelectedListener(new PlaceSelectionListener() {
                @Override
                public void onPlaceSelected(@NonNull Place place) {
                    setDestination(place.getLatLng());
                }

                @Override
                public void onError(@NonNull Status status) {
                    Log.e("Place API Error", status.toString());
                }
            });
        }
    }

    private void calculateRoute() {
        if (originLatLng == null || destinationLatLng == null) {
            Toast.makeText(this, "Por favor selecciona un origen y un destino", Toast.LENGTH_SHORT).show();
            return;
        }

        // Construir la URL para la API de Google Directions
        String url = "https://maps.googleapis.com/maps/api/directions/json?origin=" +
                originLatLng.latitude + "," + originLatLng.longitude +
                "&destination=" + destinationLatLng.latitude + "," + destinationLatLng.longitude +
                "&key=" + getString(R.string.google_maps_key);

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("RouteActivity", "Error al obtener la ruta: " + e.getMessage());
                runOnUiThread(() -> Toast.makeText(RouteActivity.this, "Error al calcular la ruta", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    runOnUiThread(() -> Toast.makeText(RouteActivity.this, "Error al calcular la ruta", Toast.LENGTH_SHORT).show());
                    return;
                }

                String responseData = response.body().string();
                Gson gson = new Gson();
                JsonObject jsonObject = gson.fromJson(responseData, JsonObject.class);
                JsonArray routes = jsonObject.getAsJsonArray("routes");

                if (routes != null && routes.size() > 0) {
                    JsonObject route = routes.get(0).getAsJsonObject();
                    JsonObject overviewPolyline = route.getAsJsonObject("overview_polyline");
                    String points = overviewPolyline.get("points").getAsString();

                    runOnUiThread(() -> {
                        // Limpiar rutas y marcadores existentes antes de dibujar la nueva ruta
                        googleMap.clear();

                        // Dibujar la nueva ruta
                        PolylineOptions polylineOptions = new PolylineOptions()
                                .addAll(decodePolyline(points))
                                .width(10)
                                .color(Color.RED)
                                .geodesic(true);

                        googleMap.addPolyline(polylineOptions);

                        // Volver a agregar los marcadores de origen y destino
                        addMarker(originLatLng, "Origen", true);
                        addMarker(destinationLatLng, "Destino", false);

                        // Ajustar la cámara para mostrar ambos marcadores y la ruta
                        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
                        boundsBuilder.include(originLatLng);
                        boundsBuilder.include(destinationLatLng);
                        googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 100));
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(RouteActivity.this, "No se encontraron rutas", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }


    private List<LatLng> decodePolyline(String encodedPolyline) {
        List<LatLng> polylineList = new ArrayList<>();
        int index = 0;
        int len = encodedPolyline.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encodedPolyline.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            lat += result % 2 == 1 ? ~(result >> 1) : (result >> 1);

            shift = 0;
            result = 0;
            do {
                b = encodedPolyline.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            lng += result % 2 == 1 ? ~(result >> 1) : (result >> 1);

            polylineList.add(new LatLng(lat / 1E5, lng / 1E5));
        }

        return polylineList;
    }

    private void addMarker(LatLng position, String title, boolean isOrigin) {
        Marker marker = googleMap.addMarker(new MarkerOptions().position(position).title(title));
        if (isOrigin) {
            originMarker = marker;
        } else {
            destinationMarker = marker;
        }
    }


    private void updateMarker(LatLng latLng, boolean isOrigin) {
        if (isOrigin) {
            if (originMarker != null) {
                originMarker.remove();
            }
            originMarker = googleMap.addMarker(new MarkerOptions().position(latLng).title("Origen"));
        } else {
            if (destinationMarker != null) {
                destinationMarker.remove();
            }
            destinationMarker = googleMap.addMarker(new MarkerOptions().position(latLng).title("Destino"));
        }
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
    }


    public void registerTrip(int passengers, double fare) {
        LatLng origin = driverViewModel.getOrigin().getValue();
        LatLng destination = driverViewModel.getDestination().getValue();

        if (origin == null || destination == null) {
            Toast.makeText(this, "Origen y destino deben estar configurados", Toast.LENGTH_SHORT).show();
            return;
        }

        // Obtener el ID del conductor desde las preferencias
        SharedPreferences sharedPreferences = getSharedPreferences(getString(R.string.nameSharedPreferences), MODE_PRIVATE);
        String driverId = sharedPreferences.getString(getString(R.string.userIdPreferences), "");

        if (driverId.isEmpty()) {
            Toast.makeText(this, "No se encontró el ID del conductor. Inicia sesión nuevamente.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Obtener la fecha y hora actual en formato ISO 8601
        String schedule = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).format(new Date());

        // Preparar la URL y los datos en JSON
        String url = getString(R.string.backendIP) + "/trips/addTrip";
        String json = String.format(
                "{\"driverId\":\"%s\",\"origin\":{\"lat\":%.6f,\"lng\":%.6f},\"destination\":{\"lat\":%.6f,\"lng\":%.6f},\"schedule\":\"%s\",\"passengerCount\":%d,\"fare\":%.2f}",
                driverId, origin.latitude, origin.longitude, destination.latitude, destination.longitude, schedule, passengers, fare);

        Log.i("RouteActivity", "JSON enviado: " + json);

        // Realizar la solicitud al backend
        OkHttpClient client = new OkHttpClient();
        RequestBody body = RequestBody.create(MediaType.parse("application/json"), json);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("RouteActivity", "Error al registrar el viaje", e);
                runOnUiThread(() -> Toast.makeText(RouteActivity.this, "Error al registrar el viaje", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    runOnUiThread(() -> Toast.makeText(RouteActivity.this, "Viaje registrado exitosamente", Toast.LENGTH_SHORT).show());
                } else {
                    String errorMessage = response.body() != null ? response.body().string() : "Error desconocido";
                    Log.e("RouteActivity", "Error en el servidor: " + errorMessage);
                    runOnUiThread(() -> Toast.makeText(RouteActivity.this, "Error al registrar el viaje en el servidor", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }


    private void adjustCameraToBounds(LatLng origin, LatLng destination) {
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        builder.include(origin); // Añade el origen
        builder.include(destination); // Añade el destino

        LatLngBounds bounds = builder.build();

        // Ajusta la cámara con un margen
        int padding = 100; // Margen en píxeles alrededor de los límites
        googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));
    }



    public void highlightRouteOnMap(LatLng origin, LatLng destination) {
        if (googleMap == null) {
            Toast.makeText(this, "El mapa no está listo aún", Toast.LENGTH_SHORT).show();
            return;
        }

        // Eliminar cualquier ruta resaltada previa si existe
        if (highlightedRoute != null) {
            highlightedRoute.remove();
        }

        // Obtener la URL de la API Directions para la nueva ruta
        String url = "https://maps.googleapis.com/maps/api/directions/json?origin=" +
                origin.latitude + "," + origin.longitude +
                "&destination=" + destination.latitude + "," + destination.longitude +
                "&key=" + getString(R.string.google_maps_key);

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("RouteActivity", "Error al cargar la ruta resaltada", e);
                runOnUiThread(() -> Toast.makeText(RouteActivity.this, "No se pudo cargar la ruta resaltada", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    runOnUiThread(() -> Toast.makeText(RouteActivity.this, "Error en la respuesta de la API", Toast.LENGTH_SHORT).show());
                    return;
                }

                String responseData = response.body().string();
                Gson gson = new Gson();
                JsonObject jsonObject = gson.fromJson(responseData, JsonObject.class);
                JsonArray routes = jsonObject.getAsJsonArray("routes");

                if (routes != null && routes.size() > 0) {
                    JsonObject route = routes.get(0).getAsJsonObject();
                    JsonObject overviewPolyline = route.getAsJsonObject("overview_polyline");
                    String points = overviewPolyline.get("points").getAsString();

                    List<LatLng> routePoints = decodePolyline(points);

                    runOnUiThread(() -> {
                        // Dibujar la ruta resaltada en el mapa
                        highlightedRoute = googleMap.addPolyline(new PolylineOptions()
                                .addAll(routePoints)
                                .width(12)
                                .color(Color.BLUE) // Color distintivo para la ruta resaltada
                                .zIndex(2) // Asegurar que la ruta resaltada esté sobre otras líneas
                        );

                        // Ajustar la cámara para que incluya toda la ruta resaltada
                        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
                        for (LatLng point : routePoints) {
                            boundsBuilder.include(point);
                        }

                        googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 100));
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(RouteActivity.this, "No se encontró una ruta para resaltar", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

}

package com.example.peerdrive;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class RouteActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private GoogleMap googleMap;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LatLng originLatLng, destinationLatLng;
    private FloatingActionButton btnCalculateRoute;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route);

        String nameSharedPreferences = getString(R.string.nameSharedPreferences);
        String typePreferences = getString(R.string.typePreferences);
        String userNamePreferences = getString(R.string.namePreferences);

        SharedPreferences sharedPreferences = getSharedPreferences(nameSharedPreferences, MODE_PRIVATE);
        String userType = sharedPreferences.getString(typePreferences, "");
        String userName = sharedPreferences.getString(userNamePreferences, "");

        if (userType.equals("driver")) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, new DriverFragment())
                    .commit();
        } else if (userType.equals("passenger")) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, new PassengerFragment())
                    .commit();
        } else {
            Toast.makeText(this, "Error: Tipo de usuario desconocido", Toast.LENGTH_SHORT).show();
        }

        btnCalculateRoute = findViewById(R.id.btnCalculateRoute);
        btnCalculateRoute.setOnClickListener(v -> calculateRoute());

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Mostrar el nombre del usuario en la barra superior
        getSupportActionBar().setTitle("Bienvenido, " + userName);

        // Inicializar Places API
        String apiKey = getString(R.string.google_maps_key);
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), apiKey);
        }

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        createFragment();
        setupAutocomplete();
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
        AutocompleteSupportFragment autocompleteOrigin = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.autocompleteOrigin);
        AutocompleteSupportFragment autocompleteDestination = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.autocompleteDestination);

        if (autocompleteOrigin != null) {
            autocompleteOrigin.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.LAT_LNG, Place.Field.NAME));
            autocompleteOrigin.setHint("Punto de inicio");
            autocompleteOrigin.setOnPlaceSelectedListener(new PlaceSelectionListener() {
                @Override
                public void onPlaceSelected(@NonNull Place place) {
                    originLatLng = place.getLatLng();
                    if (originLatLng != null) {
                        googleMap.addMarker(new MarkerOptions().position(originLatLng).title("Origen"));
                    }
                }
                @Override
                public void onError(@NonNull com.google.android.gms.common.api.Status status) {
                    Toast.makeText(RouteActivity.this, "Error al seleccionar origen: " + status.getStatusMessage(), Toast.LENGTH_SHORT).show();
                }
            });

        }

        if (autocompleteDestination != null) {
            autocompleteDestination.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.LAT_LNG, Place.Field.NAME));
            autocompleteDestination.setHint("Punto de destino");
            autocompleteDestination.setOnPlaceSelectedListener(new PlaceSelectionListener() {
                @Override
                public void onPlaceSelected(@NonNull Place place) {
                    destinationLatLng = place.getLatLng();
                    if (destinationLatLng != null) {
                        googleMap.addMarker(new MarkerOptions().position(destinationLatLng).title("Destino"));
                    }
                }

                @Override
                public void onError(@NonNull com.google.android.gms.common.api.Status status) {
                    Toast.makeText(RouteActivity.this, "Error al seleccionar destino: " + status.getStatusMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void calculateRoute() {
        if (originLatLng == null || destinationLatLng == null) {
            Toast.makeText(this, "Por favor selecciona ambos puntos", Toast.LENGTH_SHORT).show();
            return;
        }

        // Construir URL para la API de Google Directions
        String origin = originLatLng.latitude + "," + originLatLng.longitude;
        String destination = destinationLatLng.latitude + "," + destinationLatLng.longitude;
        String apiKey = getString(R.string.google_maps_key);
        String url = "https://maps.googleapis.com/maps/api/directions/json?origin=" + origin +
                "&destination=" + destination + "&key=" + apiKey;

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url).get().build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("RouteActivity", "Error al conectarse al servidor", e);
                runOnUiThread(() -> Toast.makeText(RouteActivity.this, "Error al conectarse al servidor", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try (response) {
                        String jsonResponse = response.body().string();
                        JsonObject jsonObject = new Gson().fromJson(jsonResponse, JsonObject.class);
                        JsonArray routes = jsonObject.getAsJsonArray("routes");

                        if (routes.size() > 0) {
                            JsonObject route = routes.get(0).getAsJsonObject();
                            JsonObject overviewPolyline = route.getAsJsonObject("overview_polyline");
                            String encodedPolyline = overviewPolyline.get("points").getAsString();

                            // Decodificar la polilínea y dibujarla en el mapa
                            runOnUiThread(() -> displayRouteOnMap(decodePolyline(encodedPolyline)));
                        } else {
                            runOnUiThread(() -> Toast.makeText(RouteActivity.this, "No se encontró una ruta", Toast.LENGTH_SHORT).show());
                        }
                    } catch (Exception e) {
                        Log.e("RouteActivity", "Error al procesar la respuesta", e);
                    }
                } else {
                    runOnUiThread(() -> Toast.makeText(RouteActivity.this, "Error al calcular la ruta", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void displayRouteOnMap(List<LatLng> points) {
        googleMap.clear();

        // Dibuja la ruta en el mapa
        PolylineOptions polylineOptions = new PolylineOptions()
                .addAll(points)
                .color(Color.BLUE)
                .width(5);
        googleMap.addPolyline(polylineOptions);

        // Ajustar la cámara para mostrar toda la ruta
        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        for (LatLng point : points) {
            boundsBuilder.include(point);
        }
        googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 100));
    }

    // Método para decodificar polilínea de Google Maps
    private List<LatLng> decodePolyline(String encodedPolyline) {
        List<LatLng> polyline = new ArrayList<>();
        int index = 0, len = encodedPolyline.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encodedPolyline.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = (result & 1) != 0 ? ~(result >> 1) : (result >> 1);
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encodedPolyline.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = (result & 1) != 0 ? ~(result >> 1) : (result >> 1);
            lng += dlng;

            polyline.add(new LatLng((lat / 1e5), (lng / 1e5)));
        }

        return polyline;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableUserLocation();
            } else {
                Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show();
            }
        }
    }
}

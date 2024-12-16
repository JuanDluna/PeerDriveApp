package com.example.peerdrive;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.google.android.gms.maps.model.LatLng;

public class PassengerViewModel extends ViewModel {

    private final MutableLiveData<LatLng> origin = new MutableLiveData<>();
    private final MutableLiveData<LatLng> destination = new MutableLiveData<>();

    public void setOrigin(LatLng origin) {
        this.origin.setValue(origin);
    }

    public LiveData<LatLng> getOrigin() {
        return origin;
    }

    public void setDestination(LatLng destination) {
        this.destination.setValue(destination);
    }

    public LiveData<LatLng> getDestination() {
        return destination;
    }

    // Método para verificar si ambos puntos están configurados
    public boolean isRouteReady() {
        return origin.getValue() != null && destination.getValue() != null;
    }
}

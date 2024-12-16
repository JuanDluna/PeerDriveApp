package com.example.peerdrive;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.google.android.gms.maps.model.LatLng;

public class DriverViewModel extends ViewModel {
    private final MutableLiveData<String> tripId = new MutableLiveData<>();
    private final MutableLiveData<LatLng> origin = new MutableLiveData<>();
    private final MutableLiveData<LatLng> destination = new MutableLiveData<>();
    private final MutableLiveData<Integer> passengers = new MutableLiveData<>();
    private final MutableLiveData<Double> fare = new MutableLiveData<>();

    public void setTripId(String tripId) {this.tripId.setValue(tripId);}

    public LiveData<String> getTripId(){return tripId;}

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

    public void setPassengers(int passengers) {
        this.passengers.setValue(passengers);
    }

    public LiveData<Integer> getPassengers() {
        return passengers;
    }

    public void setFare(double fare) {
        this.fare.setValue(fare);
    }

    public LiveData<Double> getFare() {
        return fare;
    }
}

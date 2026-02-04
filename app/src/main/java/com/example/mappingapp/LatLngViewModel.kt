package com.example.mappingapp

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class LatLngViewModel: ViewModel() {
    var latLng = LatLng(51.05, -0.72)
        set(newValue) {
            field = newValue
            latLngLiveData.value = newValue
        }

    var latLngLiveData = MutableLiveData<LatLng>()
}
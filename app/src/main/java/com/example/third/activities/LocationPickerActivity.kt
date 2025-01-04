package com.example.third.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.location.Geocoder
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.third.R
import com.example.third.Utils
import com.example.third.databinding.ActivityLocationPickerBinding
import com.google.android.gms.common.api.Status
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener

class LocationPickerActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var binding: ActivityLocationPickerBinding

    private companion object{
        const val TAG = "LOCATION_PICKER_TAG"
        private const val DEFAULT_ZOOM = 15
    }

    private var mMap: GoogleMap? = null

    private var mPlaceClient: PlacesClient? = null
    private var mFusedLocationProviderClient: FusedLocationProviderClient? = null

    private var mLastKnownLocation: android.location.Location? = null
    private var selectedLatitude: Double? = null
    private var selectedLongitude: Double? = null
    private var selectedAddress = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLocationPickerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.donell.visibility = View.GONE

        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        Places.initialize(this, getString(R.string.my_google_map_api_key))

        mPlaceClient = Places.createClient(this)
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        val autocompleteSupportMapFragment = supportFragmentManager.findFragmentById(R.id.autocomplete_fragment) as AutocompleteSupportFragment

        val placeList = arrayOf(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG)

        autocompleteSupportMapFragment.setPlaceFields(listOf(*placeList))

        autocompleteSupportMapFragment.setOnPlaceSelectedListener(object: PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                Log.d(TAG, "onPlaceSelected: ")

                val id = place.id
                val name = place.name
                val address = place.address
                val latLng = place.latLng
                selectedLatitude = latLng?.latitude
                selectedLongitude = latLng?.longitude
                selectedAddress = place.address ?: ""

                Log.d(TAG, "onPlaceSelected: id: $id")
                Log.d(TAG, "onPlaceSelected: name: $name")
                Log.d(TAG,"onPlaceSelected: selectedLatitude: $selectedLatitude")
                Log.d(TAG,"onPlaceSelected: selectedLongitude: $selectedLongitude")
                Log.d(TAG, "onPlaceSelected: selectedAddress: $selectedAddress")

                addMarker(latLng, name, selectedAddress)
            }

            override fun onError(status: Status) {

            }
        })

        binding.toolbarBackBtn.setOnClickListener{
            onBackPressed()
        }

        binding.toolbarGpsBtn.setOnClickListener{
            if (isGPSEnabled()){
                requestLocationPermission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))

            }else{
                Utils.toast(this, "Location is not on! Turn it on the show current location")
            }
        }

        binding.doneBtn.setOnClickListener{
            val intent = Intent()
            intent.putExtra("Latitude", selectedLatitude)
            intent.putExtra("Longitude", selectedLongitude)
            intent.putExtra("Address", selectedAddress)
            setResult(Activity.RESULT_OK, intent)

            finish()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        Log.d(TAG, "onMapReady: ")
        mMap = googleMap

        requestLocationPermission.launch(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        )

        mMap!!.setOnMapClickListener { latLng ->

            selectedLatitude = latLng.latitude
            selectedLongitude = latLng.longitude
            Log.d(TAG,"onMapReady: selectedLatitude: $selectedLatitude")
            Log.d(TAG,"onMapReady: selectedLongitude: $selectedLongitude")

            addressFromLatLng(latLng)
        }
    }

    @SuppressLint("MissingPermission")
    private val requestLocationPermission: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val isFineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
            val isCoarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

            if (isFineLocationGranted || isCoarseLocationGranted) {
                Log.d(TAG, "Location permissions granted.")
                mMap!!.isMyLocationEnabled = true
                pickCurrentPlace()
            } else {
                Utils.toast(this, "Location permissions denied.")
            }
        }


    private fun addressFromLatLng(latLng: com.google.android.gms.maps.model.LatLng){
        Log.d(TAG, "addressFromLatLng: ")

        val geocoder = Geocoder(this)

        try {
            val addressList = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)

            val address = addressList!![0]

            val addressline = address.getAddressLine(0)
            val subLocality = address.subLocality

            selectedAddress = "$addressline"

            addMarker(latLng, "$subLocality", "$addressline")
        }catch (e:Exception){
            Log.e(TAG, "addressFromLatLng: ", e)
        }
    }

    private fun pickCurrentPlace(){
        Log.d(TAG, "pickCurrentPlace: ")
        if(mMap == null){
            return
        }
        detectAndShowDeviceLocationMap()
    }

    @SuppressLint("MissingPermission")
    private fun detectAndShowDeviceLocationMap(){

        try {
            val locationResult = mFusedLocationProviderClient!!.lastLocation

            locationResult.addOnSuccessListener { location ->
                if (location != null){

                    mLastKnownLocation = location

                    selectedLatitude = location.latitude
                    selectedLongitude = location.longitude

                    Log.d(TAG, "detectAndShowDeviceLocationMap: selectedLatitude: $selectedLatitude")
                    Log.d(TAG, "detectAndShowDeviceLocationMap: selectedLongitude: $selectedLongitude")

                    val latLng = com.google.android.gms.maps.model.LatLng(
                        location.latitude!!,
                        location.longitude!!
                    )
                    mMap!!.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, DEFAULT_ZOOM.toFloat()))
                    mMap!!.animateCamera(CameraUpdateFactory.zoomTo(DEFAULT_ZOOM.toFloat()))

                    addressFromLatLng(latLng)

                }

            }.addOnFailureListener { e ->
                Log.e(TAG, "detectAndShowDeviceLocationMap: ", e)
            }

        }catch (e: Exception){
            Log.e(TAG, "detectAndShowDeviceLocationMap: ", e)
        }
    }

    private fun isGPSEnabled(): Boolean{

        val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        var gpsEnabled = false
        var networkEnabled = false

        try {
            gpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
        }catch(e:Exception){
            Log.e(TAG, "isGPSEnabled: ", e)
        }

        try {
            networkEnabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        }catch(e:Exception){
            Log.e(TAG, "isGPSEnabled: ", e)
        }

        return !(!gpsEnabled || !networkEnabled)
    }

    private fun addMarker(latLng: com.google.android.gms.maps.model.LatLng, title: String, address: String){
        Log.d(TAG, "addMarker: latitude: ${latLng.latitude}")
        Log.d(TAG, "addMarker: longitude: ${latLng.longitude}")
        Log.d(TAG, "addMarker: title: $title")
        Log.d(TAG, "addMarker: address: $address")

        mMap!!.clear()

        try {
            val markerOptions = MarkerOptions()
            markerOptions.position(latLng)
            markerOptions.title(title)
            markerOptions.snippet(address)
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))

            mMap!!.addMarker(markerOptions)
            mMap!!.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, DEFAULT_ZOOM.toFloat()))

            binding.donell.visibility = View.VISIBLE
            binding.selectedPlaceTv.text = address

        }catch (e:Exception){
            Log.e(TAG, "addMarker: ", e)

        }
    }
}
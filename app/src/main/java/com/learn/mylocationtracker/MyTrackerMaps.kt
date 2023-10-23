package com.learn.mylocationtracker

import android.content.pm.PackageManager
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import android.Manifest
import android.content.IntentSender
import android.graphics.Color
import android.os.Looper
import android.util.Log
import androidx.activity.result.IntentSenderRequest
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.learn.mylocationtracker.databinding.ActivityMyTrackerMapsBinding
import java.util.concurrent.TimeUnit


@Suppress("DEPRECATION")
class MyTrackerMaps : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMyTrackerMapsBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    //location request untuk mendapatkan location terkini
    private lateinit var locationRequest: LocationRequest

    // unutk memulai tombol lari buatlah dahulu seperti ini
    private var isTracking = false

    //buat call back seperti ini untuk update lokasi terbaru
    private lateinit var locationCallback: LocationCallback

    //ini adalah penggunaan polyline yang berguna untuk membuat riwayat dengan  garis yang dapat di tambahkan di peta
    private var allLatLng = ArrayList<LatLng>()

    //ini adlah fungsi awal agar kamera dapat mengikuti koordinat
    private var boundsBuilder = LatLngBounds.Builder()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMyTrackerMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

//        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Add a marker in Sydney and move the camera
//        val sydney = LatLng(-34.0, 151.0)
//        mMap.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))
//        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney))

        //getMyLastLocation()
        createLocationRequest()
        createLocationCallback()

        binding.btnStart.setOnClickListener {
            if (!isTracking) {
                clearMaps()
                updateTrackingStatus(true)
                startLocationUpdates()
            } else {
                updateTrackingStatus(false)
                stopLocationUpdates()
            }
        }
    }

    //fungsi di bawha ini berguna untuk membersihkan map ketika tombol start running di tekan
    private fun clearMaps() {
        mMap.clear()
        allLatLng.clear()
        boundsBuilder = LatLngBounds.Builder()
    }



    //di bawah ini adlah fungsi unutk tombol ketika start runningdan stop running
    private fun updateTrackingStatus(newStatus: Boolean) {
        isTracking = newStatus
        if (isTracking) {
            binding.btnStart.text = getString(R.string.stop_running)
        } else {
            binding.btnStart.text = getString(R.string.start_running)
        }
    }

    //di bawha ini adlaah fungsi untuk membuat update ketika ada lokasi terbaru
    private fun createLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    Log.d(TAG, "onLocationResult: " + location.latitude + ", " + location.longitude)

                    //di bawah ini adlah fungsi untuk membuat polyline yang berguna untuk membuat riwayat dengan  garis yang dapat di tambahkan di peta
                    val lastLatLng = LatLng(location.latitude, location.longitude)
                    //draw polyline
                    allLatLng.add(lastLatLng)
                    mMap.addPolyline(
                        PolylineOptions()
                            .color(Color.CYAN)
                            .width(10f)
                            .addAll(allLatLng)
                    )

                    //set boundaries/supya kamera bisa mengikuti koordinat nya
                    boundsBuilder.include(lastLatLng)
                    val bounds: LatLngBounds = boundsBuilder.build()
                    mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 64))
                }
            }
        }
    }

    //di bawha ini adlah beberapa fungsi yang berguna untuk menajalankan request update ketika tombol start di tekan dan berhenti ketika tombol stop di tekan
    private fun startLocationUpdates() {
        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (exception: SecurityException) {
            Log.e(TAG, "Error : " + exception.message)
        }
    }
    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
    override fun onResume() {
        super.onResume()
        if (isTracking) {
            startLocationUpdates()
        }
    }
    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }

    //dibawha ini adlah fungsi unutk mendapatkan location terkini
    private val resolutionLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->
            when (result.resultCode) {
                RESULT_OK ->
                    Log.i(TAG, "onActivityResult: All location settings are satisfied.")
                RESULT_CANCELED ->
                    Toast.makeText(
                        this@MyTrackerMaps,
                        "Anda harus mengaktifkan GPS untuk menggunakan aplikasi ini!",
                        Toast.LENGTH_SHORT
                    ).show()
            }
        }
    private fun createLocationRequest() {
        locationRequest = LocationRequest.create().apply {
            interval = TimeUnit.SECONDS.toMillis(1)
            maxWaitTime = TimeUnit.SECONDS.toMillis(1)
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
        val client = LocationServices.getSettingsClient(this)
        client.checkLocationSettings(builder.build())
            .addOnSuccessListener {
                getMyLastLocation()
            }
            .addOnFailureListener { exception ->
                if (exception is ResolvableApiException) {
                    try {
                        resolutionLauncher.launch(
                            IntentSenderRequest.Builder(exception.resolution).build()
                        )
                    } catch (sendEx: IntentSender.SendIntentException) {
                        Toast.makeText(this@MyTrackerMaps, sendEx.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
    }

    //untuk mendapatkan akses location permission
    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            when {
                permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false -> {
                    // Precise location access granted.
                    getMyLastLocation()
                }
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false -> {
                    // Only approximate location access granted.
                    getMyLastLocation()
                }
                else -> {
                    // No location access granted.
                }
            }
        }


    private fun checkPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    //fungsi di bawha ini berguna untk mengetahui lokasi terkahir
    private fun getMyLastLocation() {
        if     (checkPermission(Manifest.permission.ACCESS_FINE_LOCATION) &&
            checkPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
        ){
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    showStartMarker(location)
                } else {
                    Toast.makeText(
                        this@MyTrackerMaps,
                        "Location is not found. Try Again",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        } else {
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun showStartMarker(location: Location) {
        val startLocation = LatLng(location.latitude, location.longitude)
        mMap.addMarker(
            MarkerOptions()
                .position(startLocation)
                .title(getString(R.string.start_point))
        )
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(startLocation, 17f))
    }

    companion object {
        private const val TAG = "MapsActivity"
    }
}
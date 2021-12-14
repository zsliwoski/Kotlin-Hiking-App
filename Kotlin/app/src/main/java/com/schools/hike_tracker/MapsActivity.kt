package com.schools.hike_tracker

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.*
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.schools.hike_tracker.databinding.ActivityMapsBinding

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, LocationListener {

    private lateinit var locationManager: LocationManager
    private val locationPermissionCode = 2

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var cPoints : MutableList<LatLng>

    private lateinit var gpsInput : EditText
    private lateinit var startStopButton : Button
    private lateinit var updateTrailButton : Button

    private var trailRecording : Boolean = false
    private lateinit var userMarker: Marker

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        getLocation()
        cPoints = mutableListOf()

        gpsInput = findViewById<EditText>(R.id.editTextGPSData) as EditText
        startStopButton = findViewById<Button>(R.id.startStopButton) as Button
        updateTrailButton = findViewById<Button>(R.id.updateTrailButton) as Button
        if (startStopButton != null) {
            startStopButton.setOnClickListener {
                toggleTrailRecord()
            }
        }

        if (updateTrailButton != null) {
            updateTrailButton.setOnClickListener {
                parseTrailText(gpsInput.text.toString())
            }
        }
    }

    private fun parseTrailText(text : String){
        cPoints.clear()
        //parse gps string

        var strPoints : List<String> = text.split("=>")
        for (strPoint in strPoints){
            var temp = strPoint
            temp = temp.removePrefix("lat/lng: (")
            temp = temp.removeSuffix(")")

            var data = temp.split(",")

            try {
                var lat = data[0].toDouble()
                var lon = data[1].toDouble()
                var gpsPoint = LatLng(lat,lon)

                cPoints.add(gpsPoint)
            } catch (ex : Exception){

            }
        }
        addMapMarkers(cPoints)
    }

    private fun fillTrailInputText(text: String){
        gpsInput.setText(text)
    }
    private fun toggleStartStopVisuals(){
        if (trailRecording) {
            startStopButton.setText("Finish Hike")
            updateTrailButton.setText("Hike in progress...")
        } else {
            startStopButton.setText("Start Hike")
            updateTrailButton.setText("Update Trail")
        }
        updateTrailButton.isClickable = !trailRecording
    }
    private fun toggleTrailRecord(){
        //if recording, finish up
        if (trailRecording) {
            fillTrailInputText(cPoints.joinToString("=>"))
            addMapMarkers(cPoints)
            trailRecording = false
        }else{
            cPoints.clear()
            clearMapMarkers()
            trailRecording = true

            if ((ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), locationPermissionCode)
            }
            var startLoc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            if (startLoc != null){
                var tempStart = LatLng(startLoc.latitude,startLoc.longitude)
                cPoints.add(tempStart)
                updateMapPosition(tempStart)
            }
        }

        toggleStartStopVisuals()
    }

    private fun getLocation() {
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if ((ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), locationPermissionCode)
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 5f, this)
    }
    override fun onLocationChanged(location: Location) {
        var curLatLng : LatLng = LatLng(location.latitude, location.longitude)
        if (trailRecording) {
            cPoints.add(curLatLng)
            updateMapPosition(curLatLng)
        }

        /*if (userMarker == null){
            userMarker = mMap.addMarker(MarkerOptions().position(curLatLng))
        }else{
            userMarker.position = curLatLng
        }*/
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
        val sydney = LatLng(-34.0, 151.0)
        mMap.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney))
    }
    fun updateMapPosition(pos : LatLng){
        mMap.moveCamera(CameraUpdateFactory.newLatLng(pos));
    }
    fun clearMapMarkers(){
        mMap.clear()
    }

    fun addMapMarkers(points : List<LatLng>){

        if(points.isEmpty()){
            return
        }

        clearMapMarkers();

        val line = PolylineOptions().clickable(true)

        //add points to our line
        for (p in points) {
            line.add(p)
        }
        mMap.addPolyline(line)

        //map our start
        mMap.addMarker(MarkerOptions().position(points.first()).title("Start"));
        //map our end
        mMap.addMarker(MarkerOptions().position(points.last()).title("End"));
        //move to our first point
        mMap.moveCamera(CameraUpdateFactory.newLatLng(points[0]));

    }
}
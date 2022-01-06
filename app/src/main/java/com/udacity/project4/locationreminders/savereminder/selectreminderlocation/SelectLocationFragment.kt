package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.findNavController
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import org.koin.android.ext.android.inject
import com.google.android.gms.maps.model.*
import com.udacity.project4.base.NavigationCommand


class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {

    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding
    private lateinit var mapFragment: SupportMapFragment

    private lateinit var map: GoogleMap
    private val REQUEST_LOCATION_PERMISSION = 1
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationManager: LocationManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)

        binding.viewModel = _viewModel
        binding.lifecycleOwner = this

        locationManager = activity?.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        setHasOptionsMenu(true)


//      add the map setup implementation
        mapFragment = childFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
//      zoom to the user location after taking his permission
//      add style to the map
//      put a marker to location that the user selected


//      call this function after the user confirms on the selected location

        return binding.root
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        //Change the map type based on the user's selection.
        R.id.normal_map -> {
            map.mapType = GoogleMap.MAP_TYPE_NORMAL
            true
        }
        R.id.hybrid_map -> {
            map.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }
        R.id.satellite_map -> {
            map.mapType = GoogleMap.MAP_TYPE_SATELLITE
            true
        }
        R.id.terrain_map -> {
            map.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        setMapStyle(map)
        enableMyLocation()
        moveToMyLocation()
        setPoiClick(map)
    }

    @SuppressLint("MissingPermission")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocation()
            } else {
                _viewModel.showSnackBar.value =
                    "Please enable location to receive full features of app"
            }
        }
    }

    private fun isPermissionGranted() : Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    private fun enableMyLocation() {
        if(isPermissionGranted()) {
            map.isMyLocationEnabled = true
        } else {
            requestPermissions(
                arrayOf<String>(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION),
                REQUEST_LOCATION_PERMISSION
            )
        }
        binding.refreshButton.setOnClickListener {
            enableMyLocation()
        }
    }

    @SuppressLint("MissingPermission")
    private fun moveToMyLocation() {
        var locationLatLng: LatLng
        val gps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val network = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        if(map.isMyLocationEnabled && gps && network) {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    locationLatLng = LatLng(
                        location!!.latitude, location.longitude)

                    map.addMarker(
                        MarkerOptions()
                            .position(locationLatLng)
                            .title("Current Position")
                    )

                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(
                        locationLatLng, 15f
                    ))
                }
        } else {
            locationLatLng = LatLng(37.42216782736121, -122.08407897285726)

            map.addMarker(
                MarkerOptions()
                    .position(locationLatLng)
                    .title("Current Position")
            )

            map.moveCamera(CameraUpdateFactory.newLatLngZoom(
                locationLatLng, 15f
            ))
        }
    }

    private fun setMapStyle(map: GoogleMap) {
        try {
            map.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    requireContext(),
                    R.raw.map_style
                )
            )
        } catch(e: Exception) {}
    }

    private fun setPoiClick(map: GoogleMap) {
        map.setOnPoiClickListener { poi ->
            map.addMarker(
                MarkerOptions()
                    .position(poi.latLng)
                    .title(poi.name)
            )
            _viewModel.selectedPOI.value = poi
            _viewModel.latitude.value = poi.latLng.latitude
            _viewModel.longitude.value = poi.latLng.longitude
            _viewModel.reminderSelectedLocationStr.value = poi.name
            _viewModel.navigationCommand.value = NavigationCommand.Back
            childFragmentManager.beginTransaction().remove(mapFragment).commit()
        }
        map.setOnMarkerClickListener {
            _viewModel.selectedPOI.value = PointOfInterest(it.position, "Current Location",
                "Current Location")
            _viewModel.latitude.value = it.position.latitude
            _viewModel.longitude.value = it.position.longitude
            _viewModel.reminderSelectedLocationStr.value = "Current Location"
            _viewModel.navigationCommand.value = NavigationCommand.Back
            childFragmentManager.beginTransaction().remove(mapFragment).commit()
            return@setOnMarkerClickListener true
        }
        map.setOnMapClickListener {
            _viewModel.selectedPOI.value = PointOfInterest(it, "Custom Location",
                "Custom Location")
            _viewModel.latitude.value = it.latitude
            _viewModel.longitude.value = it.longitude
            _viewModel.reminderSelectedLocationStr.value = "Custom Location"
            _viewModel.navigationCommand.value = NavigationCommand.Back
            childFragmentManager.beginTransaction().remove(mapFragment).commit()
        }
    }

}
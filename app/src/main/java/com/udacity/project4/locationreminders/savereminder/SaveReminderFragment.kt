package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import android.location.LocationManager
import android.content.Context.LOCATION_SERVICE
import android.location.Location
import android.location.LocationListener
import android.location.LocationRequest
import androidx.core.content.ContextCompat.getSystemService
import com.google.android.gms.location.*
import com.google.android.gms.location.LocationRequest.create
import com.google.gson.internal.UnsafeAllocator.create
import com.udacity.project4.locationreminders.RemindersActivity
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationServices

import android.content.DialogInterface
import android.provider.Settings
import com.google.android.gms.location.LocationSettingsStatusCodes

import android.content.IntentSender
import android.content.IntentSender.SendIntentException
import android.util.Log
import android.widget.Toast
import com.google.android.gms.common.api.*

import com.google.android.gms.location.LocationSettingsStates

import com.google.android.gms.location.LocationSettingsResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY


class SaveReminderFragment : BaseFragment() {
    //Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSaveReminderBinding

    private var googleApiClient: GoogleApiClient? = null

    private val REQUEST_LOCATION_PERMISSION = 1
    private lateinit var geofencingClient: GeofencingClient

    private lateinit var locationManager: LocationManager

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(requireContext(), GeofenceBroadcastReceiver::class.java)
        PendingIntent.getBroadcast(requireContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_save_reminder, container, false)

        setDisplayHomeAsUpEnabled(true)

        binding.viewModel = _viewModel

        locationManager = activity?.getSystemService(LOCATION_SERVICE) as LocationManager

        geofencingClient = LocationServices.getGeofencingClient(requireActivity())

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this
        binding.selectLocation.setOnClickListener {
            _viewModel.navigationCommand.value =
                NavigationCommand.To(SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment())
        }

        binding.saveReminder.setOnClickListener {
            enableMyLocation()
            checkLocationAndStartGeofence()
        }
    }

    private fun getGeofencingRequest(geofence: Geofence): GeofencingRequest {
        return GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()
    }

    @SuppressLint("MissingPermission")
    private fun createGeofence() {
        val title = _viewModel.reminderTitle.value
        val description = _viewModel.reminderDescription.value
        val location = _viewModel.reminderSelectedLocationStr.value
        val latitude = _viewModel.latitude.value
        val longitude = _viewModel.longitude.value

        if (title != null && description != null && location != null &&
            latitude != null && longitude != null
        ) {
            try {
                _viewModel.validateAndSaveReminder(
                    ReminderDataItem(
                        title, description, location, latitude, longitude, title
                    )
                )

                val geofence = Geofence.Builder()
                    .setRequestId(title)
                    .setCircularRegion(
                        latitude,
                        longitude,
                        100f
                    )
                    .setExpirationDuration(604800000L)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                    .build()
                _viewModel.showSnackBar.value = "Geofence added"
                geofencingClient.addGeofences(getGeofencingRequest(geofence), geofencePendingIntent)
            } catch (e: Exception) {
                Log.e("SaveReminderFragment", e.toString())
            }
        } else {
            _viewModel.showToast.value = "Enter all fields"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        //make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
    }

    private fun isPermissionGranted(): Boolean {
        val locationPermission = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        //ONLY FOR ANDROID 10 DUE TO THE DEVICE IM TESTING ON
        //NOT CALLING DIALOG (FEATURE OR BUG IN ANDROID 11)
        return if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
            val backgroundPermission = ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if(!backgroundPermission && locationPermission) {
                _viewModel.showToast.value = "Enable full location access to create reminder"
            }

            backgroundPermission && locationPermission
        } else {
            locationPermission
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableMyLocation() {
        if (isPermissionGranted()) {
            _viewModel.locationEnabled.value = true
        } else {
            //ONLY FOR ANDROID 10 DUE TO THE DEVICE IM TESTING ON BEING ANDROID 11
            //NOT CALLING DIALOG (FEATURE OR BUG IN ANDROID 11)
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                requestPermissions(
                    arrayOf<String>(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    ),
                    REQUEST_LOCATION_PERMISSION
                )
            } else {
                requestPermissions(
                    arrayOf<String>(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ),
                    REQUEST_LOCATION_PERMISSION
                )
            }
            _viewModel.locationEnabled.value = false
        }
    }

    private fun dialogEnableLocation() {
        googleApiClient = GoogleApiClient.Builder(activity!!)
            .addApi(LocationServices.API)
            .addConnectionCallbacks(object : GoogleApiClient.ConnectionCallbacks {
                override fun onConnected(bundle: Bundle?) {}
                override fun onConnectionSuspended(i: Int) {
                    googleApiClient?.connect()
                }
            })
            .addOnConnectionFailedListener {
            }.build()
        googleApiClient?.connect()

        val locationRequest = com.google.android.gms.location.LocationRequest.create()
        val locationBuilder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
        locationBuilder.setAlwaysShow(true)

        val result: PendingResult<LocationSettingsResult> =
            LocationServices.SettingsApi.checkLocationSettings(googleApiClient, locationBuilder.build())
        result.setResultCallback { result ->
            val status: Status = result.status
             if(status.statusCode == LocationSettingsStatusCodes.RESOLUTION_REQUIRED) {
                    status.startResolutionForResult(
                        activity,
                        REQUEST_LOCATION_PERMISSION
                    )

            }
        }
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_LOCATION_PERMISSION -> when (resultCode) {
                Activity.RESULT_OK -> checkLocationAndStartGeofence()
                Activity.RESULT_CANCELED -> _viewModel.showSnackBar.value = "Cannot create reminder without location"
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                _viewModel.locationEnabled.value = true
            } else {
                _viewModel.showSnackBar.value =
                    "Enable location to create reminder"
            }
        }
    }

    private fun checkLocationAndStartGeofence() {
        val gps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val network = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        if(_viewModel.locationEnabled.value == true) {
            if(gps && network) {
                createGeofence()
            } else {
                dialogEnableLocation()
            }
        }
    }
}

package com.udacity.project4.locationreminders

import android.Manifest
import android.annotation.TargetApi
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.R
import com.udacity.project4.authentication.AuthenticationActivity
import com.udacity.project4.databinding.ActivityRemindersBinding
import com.udacity.project4.utils.REQUEST_TURN_DEVICE_LOCATION_ON

/**
 * The RemindersActivity that holds the reminders fragments
 */
class RemindersActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRemindersBinding
    private lateinit var geofencingClient: GeofencingClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRemindersBinding.inflate(layoutInflater)
        setContentView(binding.root)
        geofencingClient = LocationServices.getGeofencingClient(this)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                findNavController(binding.navHostFragment.id).popBackStack()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    fun navigateToAuthActivity() {
        val intent = Intent(this, AuthenticationActivity::class.java)
        startActivity(intent)
        finish()
    }

//    @TargetApi(29)
//    fun foregroundLocationPermissionApproved(): Boolean {
//        val foregroundLocationApproved = (
//                PackageManager.PERMISSION_GRANTED ==
//                        ActivityCompat.checkSelfPermission(
//                            this,
//                            Manifest.permission.ACCESS_FINE_LOCATION
//                        ))
//        return foregroundLocationApproved
//    }
//
//    @TargetApi(29)
//    fun requestForegroundLocationPermission() {
//        if (foregroundLocationPermissionApproved())
//            return
//        val permissionsArray = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
//        val resultCode = REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
//        ActivityCompat.requestPermissions(
//            this@RemindersActivity,
//            permissionsArray,
//            resultCode
//        )
//    }

    private fun showSnackBar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    fun addGeofence(geofencingRequest: GeofencingRequest, geofencePendingIntent: PendingIntent) {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent).run {
            addOnSuccessListener {
                println("prueba,geofence addedd")
                showSnackBar(getString(R.string.geofence_created))
            }
            addOnFailureListener {
                if (it.message != null) {
                    println("prueba,geofence error: ${it.message}")
                    showSnackBar(getString(R.string.geofences_not_added) + " ${it.message}")
                }
            }
        }
    }
    fun checkDeviceLocationSettingsAndStartGeofence(
        resolve: Boolean = true,
        onCompleteCallback: () -> Unit
    ) {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val settingsClient = LocationServices.getSettingsClient(this)
        val locationSettingsResponseTask =
            settingsClient.checkLocationSettings(builder.build())

        println("prueba, adding failure listener to location on/off task")
        locationSettingsResponseTask.addOnFailureListener { exception ->
            if (exception is ResolvableApiException && resolve) {
                println("prueba,location task exception, trying to resolute")
                try {
                    exception.startResolutionForResult(
                        this,
                        REQUEST_TURN_DEVICE_LOCATION_ON
                    )
                } catch (sendEx: IntentSender.SendIntentException) {
                    println("prueba,location task exception, failed to resolute")
                    // show snackbar error
                }
            } else {
                println("prueba,location task failure, rejected by user, showing snackbar")
                Snackbar.make(
                    binding.root,
                    R.string.location_required_error, Snackbar.LENGTH_INDEFINITE
                ).setAction(android.R.string.ok) {
                    checkDeviceLocationSettingsAndStartGeofence(onCompleteCallback = onCompleteCallback)
                }.show()
            }
        }
        locationSettingsResponseTask.addOnCompleteListener {
            //#3 Automatically attempt to add geofence if we are certain that device location is on
            if (it.isSuccessful) {
                println("prueba,location task success, calling callback")
                onCompleteCallback()
            }
        }
    }

    companion object {
        const val LOCATION_PERMISSION_INDEX = 0
        const val BACKGROUND_LOCATION_PERMISSION_INDEX = 1
        const val REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE = 3
        const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 4
    }
}

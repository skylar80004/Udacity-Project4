package com.udacity.project4.locationreminders

import android.Manifest
import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
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

/**
 * The RemindersActivity that holds the reminders fragments
 */
class RemindersActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRemindersBinding
    private lateinit var geofencingClient: GeofencingClient
    private lateinit var resolutionForResultLauncher: ActivityResultLauncher<IntentSenderRequest>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRemindersBinding.inflate(layoutInflater)
        setContentView(binding.root)
        geofencingClient = LocationServices.getGeofencingClient(this)

        resolutionForResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // Location was enabled
                println("Location was enabled, calling additional function")
                startGeofenceCallback?.invoke()
            } else {
                // Location was not enabled
                println("Location was not enabled")
                // Handle the case where location wasn't enabled
                Toast.makeText(this, R.string.location_required_error, Toast.LENGTH_LONG).show()
            }
        }
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

    private var startGeofenceCallback: (() -> Unit)? = null

    fun checkDeviceLocationSettingsAndStartGeofence(
        resolve: Boolean = true,
        startGeofenceCallback: () -> Unit
    ) {
        this.startGeofenceCallback = startGeofenceCallback
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
                val intentSenderRequest = IntentSenderRequest.Builder(exception.resolution).build()
                resolutionForResultLauncher.launch(intentSenderRequest)  // Launch
            } else {
                println("prueba,location task failure, rejected by user, showing snackbar")
                Snackbar.make(
                    binding.root,
                    R.string.location_required_error, Snackbar.LENGTH_INDEFINITE
                ).setAction(android.R.string.ok) {
                    checkDeviceLocationSettingsAndStartGeofence(startGeofenceCallback = startGeofenceCallback)
                }.show()
            }
        }
        locationSettingsResponseTask.addOnCompleteListener {
            //#3 Automatically attempt to add geofence if we are certain that device location is on
            if (it.isSuccessful) {
                println("prueba,location task success, calling callback")
                startGeofenceCallback()
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

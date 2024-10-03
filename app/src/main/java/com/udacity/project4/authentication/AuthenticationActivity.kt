package com.udacity.project4.authentication

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.udacity.project4.R
import com.udacity.project4.databinding.ActivityAuthenticationBinding // This is generated after enabling DataBinding
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.utils.GEOFENCE_CHANNEL_ID

class AuthenticationActivity : AppCompatActivity() {
    companion object {
        private const val FIREBASE_AUTH_REQUEST_CODE = 100
    }

    private lateinit var binding: ActivityAuthenticationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_authentication)

        binding.btnAuthLogin.setOnClickListener {
            launchSignInFlow()
        }
        createChannel()

        val auth = FirebaseAuth.getInstance()

        val currentUser = auth.currentUser
        if (currentUser != null) {
            navigateToRemindersActivity()
        }
    }

    private fun launchSignInFlow() {
        val providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build(),    // Email/Password
            AuthUI.IdpConfig.GoogleBuilder().build()    // Google Sign-in
        )

        val signInIntent = AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setAvailableProviders(providers)
            .setTheme(R.style.AppTheme) // Optional: apply custom theme
            .build()

        startActivityForResult(signInIntent, FIREBASE_AUTH_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == FIREBASE_AUTH_REQUEST_CODE) {
            val response = IdpResponse.fromResultIntent(data)
            if (resultCode == RESULT_OK) {
                // val user = FirebaseAuth.getInstance().currentUser
                navigateToRemindersActivity()
            } else {
                Toast.makeText(this, getString(R.string.error_happened), Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun navigateToRemindersActivity() {
        val intent = Intent(this, RemindersActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun createChannel() {
        val channelName = getString(R.string.channel_name)
        val channelDescription = getString(R.string.notification_channel_description)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                GEOFENCE_CHANNEL_ID,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationChannel.enableLights(true)
            notificationChannel.enableVibration(true)
            notificationChannel.description = channelDescription

            val notificationManager = getSystemService(
                NotificationManager::class.java
            )
            notificationManager.createNotificationChannel(notificationChannel)
        }
    }
}

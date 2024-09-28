package com.udacity.project4.authentication

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.firebase.ui.auth.AuthUI
import com.udacity.project4.R
import com.udacity.project4.databinding.ActivityAuthenticationBinding // This is generated after enabling DataBinding

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
}

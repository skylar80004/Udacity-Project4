package com.udacity.project4.locationreminders.savereminder.selectreminderlocation

import android.Manifest
import android.annotation.TargetApi
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.findNavController
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import java.util.Locale

class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {

    // Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val layoutId = R.layout.fragment_select_location
        binding = DataBindingUtil.inflate(inflater, layoutId, container, false)

        binding.viewModel = _viewModel
        binding.lifecycleOwner = this

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)

        // TODO: add the map setup implementation
        // TODO: zoom to the user location after taking his permission
        // TODO: add style to the map
        // TODO: put a marker to location that the user selected

        // TODO: call this function after the user confirms on the selected location
        onLocationSelected()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mapFragment = childFragmentManager
            .findFragmentById(R.id.map_fragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        _viewModel.navigateBack.observe(viewLifecycleOwner) { event ->
            if (event != null) {
                _viewModel.clearNavigateBack()
                findNavController().popBackStack()
            }
        }
        _viewModel.showError.observe(viewLifecycleOwner) { showError ->
            if (showError) {
                _viewModel.clearError()
                Toast.makeText(
                    requireContext(),
                    getString(R.string.err_select_location), Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun isLocationPermissionGranted(): Boolean {
        return ActivityCompat.checkSelfPermission(
            requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onResume() {
        super.onResume()
        if (isLocationPermissionGranted()) {
            enableLocationOnMap()
        }
    }

    private fun onLocationSelected() {
        // TODO: When the user confirms on the selected location,
        //  send back the selected location details to the view model
        //  and navigate back to the previous fragment to save the reminder and add the geofence
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        // TODO: Change the map type based on the user's selection.
        R.id.normal_map -> {
            true
        }

        R.id.hybrid_map -> {
            true
        }

        R.id.satellite_map -> {
            true
        }

        R.id.terrain_map -> {
            true
        }

        else -> super.onOptionsItemSelected(item)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        this.googleMap = googleMap
        enableLocationOnMap()
    }

    private var googleMap: GoogleMap? = null

    private val runningQOrLater = android.os.Build.VERSION.SDK_INT >=
            android.os.Build.VERSION_CODES.Q

    @TargetApi(29)
    fun enableLocationOnMap() {
        // Check both foreground and background location permissions
        val foregroundLocationApproved = (
                PackageManager.PERMISSION_GRANTED ==
                        ActivityCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.ACCESS_FINE_LOCATION
                        )
                )

        val backgroundPermissionApproved = if (runningQOrLater) {
            PackageManager.PERMISSION_GRANTED ==
                    ActivityCompat.checkSelfPermission(
                        requireContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    )
        } else {
            true
        }

        if (foregroundLocationApproved && backgroundPermissionApproved) {
            googleMap?.let { map ->
                // Enable location on the map
                map.isMyLocationEnabled = true
                // Get the last known location and move the camera to it
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        val currentLatLng = LatLng(location.latitude, location.longitude)
                        googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                    }
                }
                // Set a map click listener
                map.setOnMapClickListener { latLng ->
                    _viewModel.latitude.value = latLng.latitude
                    _viewModel.longitude.value = latLng.longitude

                    val geocoder = Geocoder(requireContext(), Locale.getDefault())
                    val addressList: List<Address>? = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
                    val title = addressList?.firstOrNull()?.getAddressLine(0) ?: "Selected Location"
                    _viewModel.reminderSelectedLocationStr.value = title

                    map.clear()
                    map.addMarker(
                        MarkerOptions()
                            .position(latLng)
                            .title(title)
                    )?.showInfoWindow()
                }
                // Set a POI click listener
                map.setOnPoiClickListener { poi ->
                    _viewModel.selectedPOI.value = poi
                    _viewModel.reminderSelectedLocationStr.value = poi.name
                    map.clear() // Clear any existing markers
                    map.addMarker(
                        MarkerOptions()
                            .position(poi.latLng)
                            .title(poi.name)
                    )?.showInfoWindow()
                }
            }
        } else {
            (requireActivity() as? RemindersActivity)?.requestForegroundAndBackgroundLocationPermissions()
        }
    }
}
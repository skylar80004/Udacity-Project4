package com.udacity.project4.locationreminders.savereminder.selectreminderlocation

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
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
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PointOfInterest
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import com.udacity.project4.utils.showToast
import org.koin.android.ext.android.inject

class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {

    // Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding

    private lateinit var fusedLocationClient: FusedLocationProviderClient


    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val layoutId = R.layout.fragment_select_location
        binding = DataBindingUtil.inflate(inflater, layoutId, container, false)

        binding.viewModel = _viewModel
        binding.lifecycleOwner = this

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)

        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            val fineLocationGranted = isGranted

            if (fineLocationGranted) {
            } else {
                showToast(R.string.location_required_error)
            }

            initMap()
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager
            .findFragmentById(R.id.map_fragment) as SupportMapFragment

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

        mapFragment.getMapAsync(this)
    }

    private fun isLocationPermissionGranted(): Boolean {
        return ActivityCompat.checkSelfPermission(
            requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.normal_map -> {
                // Set map type to Normal, using ?. for null safety
                googleMap?.mapType = GoogleMap.MAP_TYPE_NORMAL
                true
            }

            R.id.hybrid_map -> {
                // Set map type to Hybrid
                googleMap?.mapType = GoogleMap.MAP_TYPE_HYBRID
                true
            }

            R.id.satellite_map -> {
                // Set map type to Satellite
                googleMap?.mapType = GoogleMap.MAP_TYPE_SATELLITE
                true
            }

            R.id.terrain_map -> {
                // Set map type to Terrain
                googleMap?.mapType = GoogleMap.MAP_TYPE_TERRAIN
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }


    override fun onMapReady(googleMap: GoogleMap) {
        this.googleMap = googleMap

        try {
            val success = googleMap.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.map_style)
            )
            if (!success) {
                Toast.makeText(requireContext(), "Error when parsing map style", Toast.LENGTH_SHORT)
                    .show()
            }
        } catch (e: Resources.NotFoundException) {
            Toast.makeText(requireContext(), "Map style not found", Toast.LENGTH_SHORT).show()
        }
        checkPermissionsAndInitMap()
    }

    private var googleMap: GoogleMap? = null


    private fun initMap() {
        googleMap?.let { map ->
            // Enable location on the map
            if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
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

                var title = "Selected location"
//                    try {
//                        val geocoder = Geocoder(requireContext(), Locale.getDefault())
//                        val addressList: List<Address>? = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
//                        title = addressList?.firstOrNull()?.getAddressLine(0) ?: "Selected Location"
//                    } catch (e: Exception) {
//
//                    }
                _viewModel.reminderSelectedLocationStr.value = title

                map.clear()
                map.addMarker(
                    MarkerOptions()
                        .position(latLng)
                        .title(title)
                )?.showInfoWindow()
            }
            map.setOnPoiClickListener { poi ->
                setPOI(map = map, poi = poi)
            }
        }
    }

    private fun checkPermissionsAndInitMap() {
        // Check both foreground and background location permissions
        val foregroundLocationApproved = (
                PackageManager.PERMISSION_GRANTED ==
                        ActivityCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.ACCESS_FINE_LOCATION
                        )
                )
        if (foregroundLocationApproved) {
            initMap()
        } else {
            println("prueba, permission not granted, requesting it")
            // Request foreground permission
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun setPOI(map: GoogleMap, poi: PointOfInterest) {
        _viewModel.selectedPOI.value = poi
        _viewModel.reminderSelectedLocationStr.value = poi.name
        _viewModel.latitude.value = poi.latLng.latitude
        _viewModel.longitude.value = poi.latLng.longitude
        setPointOfInterestInMap(map = map, poi = poi)
    }

    private fun setPointOfInterestInMap(map: GoogleMap, poi: PointOfInterest) {
        map.clear() // Clear any existing markers
        map.addMarker(
            MarkerOptions()
                .position(poi.latLng)
                .title(poi.name)
        )?.showInfoWindow()
    }
}
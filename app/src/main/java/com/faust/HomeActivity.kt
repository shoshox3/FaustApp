package com.faust

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.faust.databinding.ActivityHomeBinding
import com.faust.viewmodels.HomeViewModel
import com.faust.viewmodels.LiveWorkoutViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main container activity. Hosts Navigation Component with bottom nav.
 * Tabs: Home | History | Log Workout | Progress | Profile
 */
@AndroidEntryPoint
class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var navController: NavController

    private val homeViewModel: HomeViewModel by viewModels()
    private val liveWorkoutViewModel: LiveWorkoutViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavigation()
        setupObservers()

        // Sync on launch
        homeViewModel.syncData()
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        binding.bottomNavigation.setupWithNavController(navController)

        // Hide bottom nav on certain screens
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val hideNavDestinations = setOf(
                R.id.liveWorkoutFragment,
                R.id.workoutSummaryFragment,
                R.id.authFragment
            )
            binding.bottomNavigation.visibility =
                if (destination.id in hideNavDestinations) View.GONE else View.VISIBLE
        }
    }

    private fun setupObservers() {
        // Show active workout bar when workout is in progress
        liveWorkoutViewModel.currentWorkout.observe(this) { workout ->
            binding.activeWorkoutBar.visibility =
                if (workout != null) View.VISIBLE else View.GONE
        }
    }
}

package com.example.dailyinsight

import android.os.Bundle
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import com.example.dailyinsight.databinding.ActivityMainBinding
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.dailyinsight.di.ServiceLocator
import androidx.navigation.NavController
import com.google.android.material.appbar.AppBarLayout

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ServiceLocator.init(applicationContext)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Find the NavController
        // First, Get the NavHostFragment from the FragmentManager
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_activity_main) as NavHostFragment
        // Then we have the navController
        navController = navHostFragment.navController

        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        // This prevents the back arrow (Up button) from showing on these screens.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_stock,
                R.id.navigation_market_index,
                R.id.navigation_profile
            )
        )

        // This is the magic line that does many things
        // It connects your CollapsingToolbarLayout, Toolbar, and NavController.
        // It will automatically handle title updates (including arguments) and the Up button.
        // Without it, you would have needed all these code:
        //
        // **MATERIAL DESIGN 3** Set the custom MaterialToolbar from your layout as the support action bar.
        // setSupportActionBar(binding.materialToolbar)
        //
        // setupActionBarWithNavController(navController, appBarConfiguration)
        //
        // Listen for navigation changes and set the title on the CollapsingToolbarLayout.
        // navController.addOnDestinationChangedListener { _, destination, _ ->
        //     binding.collapsingToolbarLayout.title = destination.label
        // }
        NavigationUI.setupWithNavController(
            binding.collapsingToolbarLayout,
            binding.materialToolbar,
            navController,
            appBarConfiguration
        )

        // Connect your BottomNavigationView for navigation
        val navView: BottomNavigationView = binding.navView
        navView.setupWithNavController(navController)

        // Handle bottom navigation item reselection to do nothing (prevent reloading)
        navView.setOnItemReselectedListener { /* Do nothing to prevent reloading */ }

        // Expand the AppBarLayout whenever navigation occurs
        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.appBarLayout.setExpanded(true, true)

            // Update bottom nav selection based on the navigation graph hierarchy
            // This ensures the correct bottom nav item is selected even when in detail fragments
            when (destination.id) {
                R.id.navigation_stock, R.id.stock_detail_fragment -> {
                    navView.menu.findItem(R.id.navigation_stock)?.isChecked = true
                }
                R.id.navigation_market_index, R.id.stockIndexDetailFragment -> {
                    navView.menu.findItem(R.id.navigation_market_index)?.isChecked = true
                }
                R.id.navigation_profile -> {
                    navView.menu.findItem(R.id.navigation_profile)?.isChecked = true
                }
            }
        }
    }

    // onSupportNavigateUp()이 있어야지 뒤로가기 버튼이 제대로 작동합니다.
    override fun onSupportNavigateUp(): Boolean {
        // The `|| super.onSupportNavigateUp()` provides a fallback.
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}
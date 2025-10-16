package com.example.dailyinsight

import android.os.Bundle
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.dailyinsight.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // **MATERIAL DESIGN 3** Set the custom MaterialToolbar from your layout as the support action bar.
        setSupportActionBar(binding.materialToolbar)

        // Get the NavHostFragment from the FragmentManager
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_activity_main) as NavHostFragment

        val navView: BottomNavigationView = binding.navView

        val navController = navHostFragment.navController
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_market_index
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        // **THIS IS THE FIX FOR THE TITLE**
        // Listen for navigation changes and set the title on the CollapsingToolbarLayout.
        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.collapsingToolbarLayout.title = destination.label
        }
    }

    // onSupportNavigateUp()이 있어야지 뒤로가기 버튼이 제대로 작동합니다.
    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        // The `|| super.onSupportNavigateUp()` provides a fallback.
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}
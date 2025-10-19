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

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ServiceLocator.init(applicationContext)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.container)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

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
                R.id.navigation_today,
                R.id.navigation_history,
                R.id.navigation_market_index
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
    }

    // onSupportNavigateUp()이 있어야지 뒤로가기 버튼이 제대로 작동합니다.
    override fun onSupportNavigateUp(): Boolean {
        // The `|| super.onSupportNavigateUp()` provides a fallback.
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}
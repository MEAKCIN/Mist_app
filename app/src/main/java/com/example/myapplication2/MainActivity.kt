package com.example.myapplication2

import androidx.appcompat.app.AppCompatActivity
import android.net.Uri
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.google.android.material.bottomnavigation.BottomNavigationView

enum class Emotion {
    HAPPY, SAD, ANGRY, NEUTRAL
}

class MainActivity : AppCompatActivity() {

    var sprayPeriodValue: Int = 10
    var sprayLengthValue: Int = 30
    var bigSwitchState: Boolean = true
    var photoUri: Uri? = null  // New variable to hold photograph URI

    private val emotionViewModel: EmotionViewModel by viewModels()
    private lateinit var toolbar: Toolbar
    private lateinit var bottomNavigationView: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = getString(R.string.app_name)

        bottomNavigationView = findViewById(R.id.bottom_navigation)
        setupBottomNavigation()

        if (savedInstanceState == null) {
            loadFragment(HomeFragment())
            bottomNavigationView.selectedItemId = R.id.navigation_home
        }
    }

    private fun setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener { item ->
            val fragment: Fragment = when (item.itemId) {
                R.id.navigation_home -> HomeFragment()
                R.id.navigation_manual -> ManualFragment()
                R.id.navigation_photograph -> PhotographFragment()
                else -> return@setOnItemSelectedListener false
            }
            loadFragment(fragment)
            true
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.commit {
            replace(R.id.fragment_container, fragment)
        }
    }
}
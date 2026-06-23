package com.counter.app

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.counter.app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        viewModel.totalCount.observe(this) { count ->
            binding.totalCountText.text = count.toString()
        }
        viewModel.instagramCount.observe(this) { count ->
            binding.instagramCount.text = count.toString()
        }
        viewModel.youtubeCount.observe(this) { count ->
            binding.youtubeCount.text = count.toString()
        }
        viewModel.facebookCount.observe(this) { count ->
            binding.facebookCount.text = count.toString()
        }

        updateServiceStatus()

        binding.enableServiceButton.setOnClickListener {
            if (!isAccessibilityServiceEnabled()) {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            } else {
                AlertDialog.Builder(this)
                    .setTitle("Service is Active")
                    .setMessage("Reel Counter service is already enabled. You can disable it in Accessibility Settings if needed.")
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateServiceStatus()
    }

    private fun updateServiceStatus() {
        if (isAccessibilityServiceEnabled()) {
            binding.serviceStatusText.text = getString(R.string.service_active)
            binding.serviceStatusText.setTextColor(getColor(R.color.accent_green))
            binding.enableServiceButton.text = getString(R.string.service_active)
        } else {
            binding.serviceStatusText.text = getString(R.string.service_inactive)
            binding.serviceStatusText.setTextColor(getColor(R.color.accent_red))
            binding.enableServiceButton.text = getString(R.string.enable_service)
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = am.getEnabledAccessibilityServiceList(
            AccessibilityServiceInfo.FEEDBACK_GENERIC
        )
        return enabledServices.any { it.resolveInfo.serviceInfo.packageName == packageName }
    }
}

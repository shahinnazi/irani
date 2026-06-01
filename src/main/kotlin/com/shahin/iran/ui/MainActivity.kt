package com.shahin.iran.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.shahin.iran.PREF_NOTIFY_DATE
import com.shahin.iran.entities.Jdn
import com.shahin.iran.global.initGlobal
import com.shahin.iran.global.language
import com.shahin.iran.global.updateStoredPreference
import com.shahin.iran.ui.theme.AppTheme
import com.shahin.iran.utils.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*
import com.shahin.iran.R
class MainActivity : ComponentActivity() {
    private lateinit var notificationPermissionLauncher: ActivityResultLauncher<String>
    private val _showPermissionBox = MutableStateFlow(false)
    private val showPermissionBox = _showPermissionBox.asStateFlow()

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.BaseTheme)
        applyAppLanguage(this)
        super.onCreate(savedInstanceState)

        // Initialize _showPermissionBox after super.onCreate to ensure context is ready
        _showPermissionBox.value = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED

        initGlobal(this)
        startWorker(this)
        readAndStoreDeviceCalendarEventsOfTheDay(applicationContext)
        update(applicationContext, false)

        notificationPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                preferences.edit { putBoolean(PREF_NOTIFY_DATE, isGranted) }
                updateStoredPreference(this)
                if (isGranted) update(this, updateDate = true)
                _showPermissionBox.value = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
            }

        val initialJdn = run {
            intent?.data?.takeIf {
                it.path?.startsWith("/time") == true
            }?.pathSegments?.last()?.toLongOrNull()?.let {
                Jdn(Date(it).toGregorianCalendar().toCivilDate())
            } ?: intent?.getLongExtra(jdnActionKey, -1L)?.takeIf { it != -1L }?.let(::Jdn)
        }

        setContent {
            AppTheme {
                val context = LocalContext.current
                val view = LocalView.current
                val showPermissionBox by showPermissionBox.collectAsState()
                var showBatteryOptBox by remember {
                    mutableStateOf(
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                                !isIgnoringBatteryOptimizations(context)
                    )
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    App(intent?.action, initialJdn, ::finish)

                    if (showPermissionBox || showBatteryOptBox) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xAA000000))
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White, RoundedCornerShape(16.dp))
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                if (showPermissionBox) {
                                    Text("اعطای مجوز اعلان", fontSize = 20.sp, color = Color.Black)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        "برای دریافت یادآورها و اعلان‌های مناسب، لطفاً مجوز اعلان را فعال کنید.",
                                        fontSize = 16.sp,
                                        color = Color.DarkGray
                                    )
                                    Spacer(modifier = Modifier.height(20.dp))
                                    Button(
                                        onClick = {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                when {
                                                    ContextCompat.checkSelfPermission(
                                                        context,
                                                        Manifest.permission.POST_NOTIFICATIONS
                                                    ) == PackageManager.PERMISSION_GRANTED -> {
                                                        _showPermissionBox.value = false
                                                    }
                                                    shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS).not() -> {
                                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                                    }
                                                    else -> {
                                                        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                                        }
                                                        context.startActivity(intent)
                                                    }
                                                }
                                            }
                                            _showPermissionBox.value = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                                    ContextCompat.checkSelfPermission(
                                                        context,
                                                        Manifest.permission.POST_NOTIFICATIONS
                                                    ) != PackageManager.PERMISSION_GRANTED
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(56.dp)
                                    ) {
                                        Text("اعطای مجوز اعلان", fontSize = 18.sp)
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                }

                                if (showBatteryOptBox) {
                                    Text("استثنا از بهینه‌سازی باتری", fontSize = 20.sp, color = Color.Black)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        "برای اجرای مداوم سرویس‌، لطفاً اپلیکیشن را از بهینه‌سازی باتری مستثنی کنید.",
                                        fontSize = 16.sp,
                                        color = Color.DarkGray
                                    )
                                    Spacer(modifier = Modifier.height(20.dp))
                                    Button(
                                        onClick = {
                                            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                                data = android.net.Uri.parse("package:${context.packageName}")
                                            }
                                            context.startActivity(intent)
                                            showBatteryOptBox = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                                                    !isIgnoringBatteryOptimizations(context)
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(56.dp)
                                    ) {
                                        Text("مستثنی کردن", fontSize = 18.sp)
                                    }
                                }
                            }
                        }
                    }

                    LaunchedEffect(Unit) {
                        while (true) {
                            kotlinx.coroutines.delay(2000)
                            _showPermissionBox.value = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                    ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.POST_NOTIFICATIONS
                                    ) != PackageManager.PERMISSION_GRANTED
                            showBatteryOptBox = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                                    !isIgnoringBatteryOptimizations(context)
                        }
                    }

                    LaunchedEffect(Unit) {
                        language.collect {
                            onConfigurationChanged(resources.configuration)
                            view.dispatchConfigurationChanged(resources.configuration)
                        }
                    }
                }
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(applyLanguageToConfiguration(newConfig))
        applyAppLanguage(this)
    }

    override fun onResume() {
        super.onResume()
        applyAppLanguage(this)
        update(applicationContext, false)
        ++resumeToken_.value
    }
}

private val resumeToken_ = MutableStateFlow(0)
val resumeToken: StateFlow<Int> = resumeToken_

// Helper function to check if battery optimizations are ignored
private fun isIgnoringBatteryOptimizations(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        powerManager.isIgnoringBatteryOptimizations(context.packageName)
    } else {
        true // Pre-M, no optimization
    }
}

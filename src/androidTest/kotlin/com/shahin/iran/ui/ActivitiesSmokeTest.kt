package com.shahin.iran.ui

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.shahin.iran.KEY_EXTRA_PRAYER
import com.shahin.iran.entities.PrayTime
import com.shahin.iran.ui.athan.AthanActivity
import com.shahin.iran.ui.settings.agewidget.AgeWidgetConfigureActivity
import com.shahin.iran.ui.settings.wallpaper.DreamSettingsActivity
import com.shahin.iran.ui.settings.wallpaper.WallpaperSettingsActivity
import com.shahin.iran.ui.settings.widgetnotification.WidgetConfigurationActivity
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class ActivitiesSmokeTest {
    @Test
    fun test() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        ActivityScenario.launch<MainActivity>(Intent(context, MainActivity::class.java))
        ActivityScenario.launch<AthanActivity>(
            Intent(context, AthanActivity::class.java)
                .putExtra(KEY_EXTRA_PRAYER, PrayTime.ASR.name)
        )
        ActivityScenario.launch<WidgetConfigurationActivity>(
            Intent(context, WidgetConfigurationActivity::class.java)
        )
        ActivityScenario.launch<WallpaperSettingsActivity>(
            Intent(context, WallpaperSettingsActivity::class.java)
        )
        ActivityScenario.launch<DreamSettingsActivity>(
            Intent(context, DreamSettingsActivity::class.java)
        )
        ActivityScenario.launch<AgeWidgetConfigureActivity>(
            Intent(context, AgeWidgetConfigureActivity::class.java)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, 0)
        )
    }
}

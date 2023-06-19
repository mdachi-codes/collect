package org.odk.collect.androidshared.utils

import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import org.odk.collect.androidshared.R

object AppBarUtils {

    @JvmStatic
    fun setupAppBarLayout(activity: Activity, title: CharSequence) {
        val toolbar = activity.findViewById<Toolbar>(R.id.toolbar)
        if (toolbar != null && activity is AppCompatActivity) {
            toolbar.title = "  "+title
            toolbar.setLogo(R.drawable.pact_logo_medium)

            activity.setSupportActionBar(toolbar)
        }
    }
}

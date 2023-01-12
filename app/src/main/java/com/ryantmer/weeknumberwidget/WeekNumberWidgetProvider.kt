package com.ryantmer.weeknumberwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.edit
import java.time.LocalDateTime
import java.time.temporal.ChronoField

class WeekNumberWidgetProvider : AppWidgetProvider() {
    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        Log.d(TAG, "Updating widget ID $appWidgetId")

        val views = RemoteViews(context.packageName, R.layout.widget).apply {
            setTextViewText(
                R.id.week_number,
                LocalDateTime.now().get(ChronoField.ALIGNED_WEEK_OF_YEAR).toString()
            )
        }

        val intent = Intent(context, WeekNumberWidgetProvider::class.java)
        intent.action = CHECKBOX_ACTION
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        intent.data = intent.asUri()
        val checkboxPendingIntent = PendingIntent.getBroadcast(context, 0, intent, INTENT_FLAGS)
        views.setOnCheckedChangeResponse(
            R.id.checkbox,
            RemoteViews.RemoteResponse.fromPendingIntent(checkboxPendingIntent)
        )

        val prefsKey = PREFS_KEY_PREFIX + appWidgetId
        val lastResetString = context.getSharedPreferences(PREFS_NAME, 0).getString(prefsKey, null)
        if (lastResetString != null) {
            // When the UI gets refreshed, set the checkbox state based on whether:
            // - the checkbox has been checked before, and
            // - the checkbox was last checked yesterday (or before)
            val checkboxLastReset = LocalDateTime.parse(lastResetString)
            Log.d(TAG, "Checkbox was most recently reset on $checkboxLastReset")

            val now = LocalDateTime.now()
            if (checkboxLastReset.year == now.year && checkboxLastReset.dayOfYear == now.dayOfYear) {
                views.setCompoundButtonChecked(R.id.checkbox, true)
                views.setViewVisibility(R.id.checkbox, View.GONE)
                views.setViewVisibility(R.id.done_text, View.VISIBLE)
            } else {
                // Otherwise, reset the checkbox - it was last checked yesterday or before
                views.setCompoundButtonChecked(R.id.checkbox, false)
                views.setViewVisibility(R.id.checkbox, View.VISIBLE)
                views.setViewVisibility(R.id.done_text, View.GONE)
            }
        }

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        Log.d(TAG, "onUpdate")
        appWidgetIds.forEach { appWidgetId ->
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onDeleted(context: Context?, appWidgetIds: IntArray?) {
        super.onDeleted(context, appWidgetIds)

        context?.deleteSharedPreferences(PREFS_NAME)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        val action = intent.action
        Log.d(TAG, "onReceive $action")

        intent.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID)?.let { appWidgetId ->
            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                when (action) {
                    CHECKBOX_ACTION -> {
                        Log.d(TAG, "Checkbox tapped")

                        val prefsKey = PREFS_KEY_PREFIX + appWidgetId
                        val today = LocalDateTime.now()
                        context.getSharedPreferences(PREFS_NAME, 0).edit {
                            putString(prefsKey, today.toString())
                        }

                        Log.d(TAG, "Last-checked time updated to $today")

                        updateWidget(context, AppWidgetManager.getInstance(context), appWidgetId)
                    }
                    MANUAL_UPDATE_ACTION -> {
                        Log.d(TAG, "Forcibly updating widget")
                        updateWidget(context, AppWidgetManager.getInstance(context), appWidgetId)
                    }
                }
            }
        }
    }

    private fun Intent.asUri() = Uri.parse(this.toUri(Intent.URI_INTENT_SCHEME))

    companion object {
        private val TAG = WeekNumberWidgetProvider::class.simpleName
        private const val PREFS_NAME = "com.ryantmer.WeekNumberWidget"
        private const val PREFS_KEY_PREFIX = "mostRecentResetDay"
        private const val CHECKBOX_ACTION = "CHECKBOX_ACTION"
        private const val MANUAL_UPDATE_ACTION = "MANUAL_UPDATE_ACTION"
        private const val INTENT_FLAGS =
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    }
}
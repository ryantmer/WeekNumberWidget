package com.ryantmer.weeknumberwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.RemoteViews
import androidx.core.content.edit
import java.time.LocalDate
import java.time.temporal.ChronoField

class WeekNumberWidgetProvider : AppWidgetProvider() {
    private fun getBasicRemoteViews(context: Context): RemoteViews =
        RemoteViews(context.packageName, R.layout.widget).apply {
            setTextViewText(
                R.id.week_number,
                LocalDate.now().get(ChronoField.ALIGNED_WEEK_OF_YEAR).toString()
            )
        }

    private fun getPendingIntentForAction(
        context: Context,
        action: String,
        appWidgetId: Int
    ): PendingIntent {
        val intent = Intent(context, WeekNumberWidgetProvider::class.java)
        intent.action = action
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        intent.data = intent.asUri()
        return PendingIntent.getBroadcast(context, 0, intent, INTENT_FLAGS)
    }

    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        Log.d(TAG, "Updating widget ID $appWidgetId")

        val views = getBasicRemoteViews(context)

        val checkboxPendingIntent = getPendingIntentForAction(context, CHECKBOX_ACTION, appWidgetId)
        views.setOnCheckedChangeResponse(
            R.id.checkbox,
            RemoteViews.RemoteResponse.fromPendingIntent(checkboxPendingIntent)
        )

//        val manualRefreshPendingIntent =
//            getPendingIntentForAction(context, MANUAL_UPDATE_ACTION, appWidgetId)
//        views.setOnClickPendingIntent(R.id.refresh_button, manualRefreshPendingIntent)

        val prefsKey = PREFS_KEY_PREFIX + appWidgetId
        val mostRecentResetDay = LocalDate.parse(
            context.getSharedPreferences(PREFS_NAME, 0)
                .getString(prefsKey, LocalDate.MIN.toString())
        )
        val today = LocalDate.now()
        Log.d(TAG, "Checkbox was most recently reset on $mostRecentResetDay")
        if (mostRecentResetDay.isBefore(today)) {
            views.setCompoundButtonChecked(R.id.checkbox, false)
            context.getSharedPreferences(PREFS_NAME, 0).edit {
                putString(prefsKey, today.toString())
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
                        // TODO: When the checkbox is checked, disable it until it gets reset
                        // updateWidget(context, AppWidgetManager.getInstance(context), appWidgetId)
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
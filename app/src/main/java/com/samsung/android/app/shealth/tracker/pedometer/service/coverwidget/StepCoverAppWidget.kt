package com.samsung.android.app.shealth.tracker.pedometer.service.coverwidget

import android.app.ActivityOptions
import android.app.KeyguardManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.*
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import com.sec.android.app.shealth.AppLauncherService
import com.sec.android.app.shealth.DisplayListenerService
import com.sec.android.app.shealth.OffBroadcastReceiver
import com.sec.android.app.shealth.R


class StepCoverAppWidget: AppWidgetProvider() {

    private val onClickTag = "OnClickTag"
    private val coverLock = "cover_lock"

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            val isGridView = context.getSharedPreferences(
                "samsprung.launcher.PREFS", Context.MODE_PRIVATE
            ).getBoolean("gridview", true)
            val view = if (isGridView) R.id.widgetGridView else R.id.widgetListView
            val mgr = AppWidgetManager.getInstance(context.applicationContext)
            mgr.notifyAppWidgetViewDataChanged(mgr.getAppWidgetIds(ComponentName(
                context.applicationContext, StepCoverAppWidget::class.java)), view)
        }
        if (intent.action.equals(onClickTag)) {
            val launchPackage = intent.getStringExtra("launchPackage")
            val launchActivity = intent.getStringExtra("launchActivity")

            if (launchPackage == null || launchActivity == null) return

            @Suppress("DEPRECATION")
            val mKeyguardLock = (context.getSystemService(
                Context.KEYGUARD_SERVICE) as KeyguardManager).newKeyguardLock(coverLock)
            @Suppress("DEPRECATION") mKeyguardLock.disableKeyguard()

            val serviceIntent = Intent(context, DisplayListenerService::class.java)
            val extras = Bundle()
            extras.putString("launchPackage", launchPackage)
            extras.putString("launchActivity", launchActivity)
            context.startForegroundService(serviceIntent.putExtras(extras))

            val mReceiver: BroadcastReceiver = OffBroadcastReceiver(
                ComponentName(launchPackage, launchActivity)
            )
            IntentFilter(Intent.ACTION_SCREEN_OFF).also {
                context.applicationContext.registerReceiver(mReceiver, it)
            }

            val coverIntent = Intent(Intent.ACTION_MAIN)
            coverIntent.addCategory(Intent.CATEGORY_LAUNCHER)
            coverIntent.component = ComponentName(launchPackage, launchActivity)
            val options = ActivityOptions.makeBasic().setLaunchDisplayId(1)
            try {
                val applicationInfo: ApplicationInfo = context.packageManager.getApplicationInfo (
                    launchPackage, PackageManager.GET_META_DATA
                )
                applicationInfo.metaData.putString(
                    "com.samsung.android.activity.showWhenLocked", "true"
                )
            } catch (e: PackageManager.NameNotFoundException) {
                e.printStackTrace()
            }
            coverIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            coverIntent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
            coverIntent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
            coverIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            context.startActivity(coverIntent, options.toBundle())
        }
        super.onReceive(context, intent)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val isGridView = context.getSharedPreferences(
            "samsprung.launcher.PREFS", Context.MODE_PRIVATE
        ).getBoolean("gridview", true)
        val view = if (isGridView) R.id.widgetGridView else R.id.widgetListView

//        val mReceiver: BroadcastReceiver = OffBroadcastReceiver()
//        IntentFilter().apply {
//            addAction(Intent.ACTION_PACKAGE_ADDED)
//            addAction(Intent.ACTION_PACKAGE_REMOVED)
//            addDataScheme("package")
//        }.also {
//            context.applicationContext.registerReceiver(mReceiver, it)
//        }

        appWidgetIds.forEach { appWidgetId ->
            val views = RemoteViews(
                context.packageName, R.layout.step_widget_view
            )

            views.setViewVisibility(R.id.widgetListView,
                if (isGridView) View.GONE else View.VISIBLE)
            views.setViewVisibility(R.id.widgetGridView,
                if (isGridView) View.VISIBLE else View.GONE)

            val appintent = Intent(context, AppLauncherService::class.java)
            appintent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            appintent.data = Uri.parse(appintent.toUri(Intent.URI_INTENT_SCHEME))
            views.setRemoteAdapter(view, appintent)

            val itemIntent = Intent(context, StepCoverAppWidget::class.java)
            itemIntent.action = onClickTag
            itemIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            val itemPendingIntent = PendingIntent.getBroadcast(
                context, 0, itemIntent, PendingIntent.FLAG_UPDATE_CURRENT
            )
            views.setPendingIntentTemplate(view, itemPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}

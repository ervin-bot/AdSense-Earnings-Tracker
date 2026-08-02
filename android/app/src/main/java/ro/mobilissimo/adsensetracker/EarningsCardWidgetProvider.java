package ro.mobilissimo.adsensetracker;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.RemoteViews;

public class EarningsCardWidgetProvider extends AppWidgetProvider {
    private static final String TAG = "AdSenseCardWidget";
    private static final String PREFS = "adsense_tracker";
    private static final String PREF_WIDGET_TODAY_AMOUNT = "widgetTodayAmount";
    private static final String PREF_WIDGET_TODAY_CHANGE = "widgetTodayChange";
    private static final String PREF_WIDGET_PROJECTION_AMOUNT = "widgetProjectionAmount";
    private static final String PREF_WIDGET_PROJECTION_META = "widgetProjectionMeta";

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] widgetIds) {
        updateWidgetsSafely(context, manager, widgetIds);
        WidgetTodayRefreshJobService.enqueueRefresh(context);
    }

    @Override
    public void onEnabled(Context context) {
        WidgetTodayRefreshJobService.enqueueRefresh(context);
    }

    @Override
    public void onDisabled(Context context) {
        WidgetTodayRefreshJobService.cancelIfNoWidgets(context);
    }

    public static void updateAll(Context context) {
        try {
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            ComponentName component = new ComponentName(context, EarningsCardWidgetProvider.class);
            updateWidgetsSafely(context, manager, manager.getAppWidgetIds(component));
        } catch (RuntimeException error) {
            Log.e(TAG, "Could not query card widgets.", error);
        }
    }

    private static void updateWidgetsSafely(Context context, AppWidgetManager manager, int[] widgetIds) {
        try {
            updateWidgets(context, manager, widgetIds);
        } catch (RuntimeException error) {
            Log.e(TAG, "Could not update card widgets.", error);
        }
    }

    private static void updateWidgets(Context context, AppWidgetManager manager, int[] widgetIds) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String todayAmount = prefs.getString(PREF_WIDGET_TODAY_AMOUNT, "€0.00");
        String todayChange = prefs.getString(PREF_WIDGET_TODAY_CHANGE, "Open app to refresh");
        String projectionAmount = prefs.getString(PREF_WIDGET_PROJECTION_AMOUNT, "€0.00");
        String projectionMeta = prefs.getString(PREF_WIDGET_PROJECTION_META, "Based on daily average");

        for (int widgetId : widgetIds) {
            try {
                RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_earnings_card);
                views.setTextViewText(R.id.widget_today_amount, todayAmount);
                views.setTextViewText(R.id.widget_today_change, todayChange);
                views.setTextViewText(R.id.widget_projection_amount, projectionAmount);
                views.setTextViewText(R.id.widget_projection_meta, projectionMeta);
                views.setOnClickPendingIntent(R.id.widget_card, openAppIntent(context, 2001));
                manager.updateAppWidget(widgetId, views);
            } catch (RuntimeException error) {
                Log.e(TAG, "Could not update card widget " + widgetId + ".", error);
            }
        }
    }

    private static PendingIntent openAppIntent(Context context, int requestCode) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}

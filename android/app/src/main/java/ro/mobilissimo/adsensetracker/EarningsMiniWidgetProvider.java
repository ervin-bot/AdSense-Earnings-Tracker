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

public class EarningsMiniWidgetProvider extends AppWidgetProvider {
    private static final String TAG = "AdSenseMiniWidget";
    private static final String PREFS = "adsense_tracker";
    private static final String PREF_WIDGET_TODAY_AMOUNT = "widgetTodayAmount";

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
            ComponentName component = new ComponentName(context, EarningsMiniWidgetProvider.class);
            updateWidgetsSafely(context, manager, manager.getAppWidgetIds(component));
        } catch (RuntimeException error) {
            Log.e(TAG, "Could not query mini widgets.", error);
        }
    }

    private static void updateWidgetsSafely(Context context, AppWidgetManager manager, int[] widgetIds) {
        try {
            updateWidgets(context, manager, widgetIds);
        } catch (RuntimeException error) {
            Log.e(TAG, "Could not update mini widgets.", error);
        }
    }

    private static void updateWidgets(Context context, AppWidgetManager manager, int[] widgetIds) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String todayAmount = prefs.getString(PREF_WIDGET_TODAY_AMOUNT, "€0.00");

        for (int widgetId : widgetIds) {
            try {
                RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_earnings_mini);
                views.setTextViewText(R.id.widget_mini_amount, todayAmount);
                views.setOnClickPendingIntent(R.id.widget_mini_card, openAppIntent(context, 2002));
                manager.updateAppWidget(widgetId, views);
            } catch (RuntimeException error) {
                Log.e(TAG, "Could not update mini widget " + widgetId + ".", error);
            }
        }
    }

    private static PendingIntent openAppIntent(Context context, int requestCode) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}

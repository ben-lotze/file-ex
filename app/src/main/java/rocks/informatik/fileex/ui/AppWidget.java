package rocks.informatik.fileex.ui;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.widget.RemoteViews;

import rocks.informatik.fileex.R;

/**
 * Implementation of App Widget functionality.
 */
public class AppWidget extends AppWidgetProvider {

    // for refresh button (for free storage views)
//    private static String INTENT_EXTRA_APP_WIDGET_ID = "app_widget_id";
//    public static String WIDGET_BUTTON_NEXT = "com.example.lotze.intent.action.WIDGET_BUTTON_NEXT";
//    public static String WIDGET_BUTTON_PREVIOUS = "com.example.lotze.intent.action.WIDGET_BUTTON_PREVIOUS";

    public static final String INTENT_EXTRA_PATH_TO_OPEN = "INTENT_EXTRA_PATH_TO_OPEN";

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {

//        CharSequence widgetText = context.getString(R.string.appwidget_text);
        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.app_widget);
//        views.setTextViewText(R.id.appwidget_text, widgetText);


        Intent intentInternal = new Intent(context, MainActivity.class);
        intentInternal.putExtra(INTENT_EXTRA_PATH_TO_OPEN, Environment.getRootDirectory().getAbsolutePath());
        PendingIntent pendingIntentInternal = PendingIntent.getActivity(context, 0, intentInternal, 0);
        views.setOnClickPendingIntent(R.id.icon_widget_internal_phone_storage, pendingIntentInternal);

        Intent intentExternal = new Intent(context, MainActivity.class);
        intentInternal.putExtra(INTENT_EXTRA_PATH_TO_OPEN, Environment.getExternalStorageDirectory().getAbsolutePath());
        PendingIntent pendingIntentExternal = PendingIntent.getActivity(context, 0, intentExternal, 0);
        views.setOnClickPendingIntent(R.id.icon_widget_sd_card, pendingIntentExternal);

        Intent intentDownloads = new Intent(context, MainActivity.class);
        intentInternal.putExtra(INTENT_EXTRA_PATH_TO_OPEN, Environment.getDownloadCacheDirectory().getAbsolutePath());
        PendingIntent pendingIntentDownloads = PendingIntent.getActivity(context, 0, intentDownloads, 0);
        views.setOnClickPendingIntent(R.id.icon_widget_downloads, pendingIntentDownloads);

        Intent intentPhotons = new Intent(context, MainActivity.class);
        intentInternal.putExtra(INTENT_EXTRA_PATH_TO_OPEN, Environment.getExternalStorageDirectory().getAbsolutePath());
        PendingIntent pendingIntentPhotos = PendingIntent.getActivity(context, 0, intentPhotons, 0);
        views.setOnClickPendingIntent(R.id.icon_widget_internal_phone_storage, pendingIntentPhotos);

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }
}


package com.shortcutgrid.widget;

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public final class WidgetPinnedReceiver extends BroadcastReceiver {
    static final String EXTRA_CONFIGURATION = "configuration";

    @Override
    public void onReceive(Context context, Intent intent) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        int rows = GridWidget.providerRows(context, manager, id);
        String data = intent.getStringExtra(EXTRA_CONFIGURATION);
        if (rows == 0 || data == null) return;
        new GridStore(context).save(id, GridConfig.decode(data, rows));
        GridWidget.update(context, manager, id);
    }
}

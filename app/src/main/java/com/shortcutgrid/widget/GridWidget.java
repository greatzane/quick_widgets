package com.shortcutgrid.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.SizeF;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GridWidget extends AppWidgetProvider {
    static final String EXTRA_SLOT = "slot";

    static ComponentName component(Context context, int rows) {
        return new ComponentName(context, rows == 2 ? DoubleGridWidget.class : GridWidget.class);
    }

    static int providerRows(Context context, AppWidgetManager manager, int id) {
        AppWidgetProviderInfo info = manager.getAppWidgetInfo(id);
        if (info == null) return 0;
        if (info.provider.equals(component(context, 1))) return 1;
        if (info.provider.equals(component(context, 2))) return 2;
        return 0;
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        for (int id : ids) update(context, manager, id);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager manager, int id, Bundle options) {
        update(context, manager, id);
    }

    @Override
    public void onDeleted(Context context, int[] ids) {
        new GridStore(context).delete(ids);
    }

    @Override
    public void onRestored(Context context, int[] oldIds, int[] newIds) {
        new GridStore(context).remap(oldIds, newIds);
    }

    static void update(Context context, AppWidgetManager manager, int id) {
        int rows = providerRows(context, manager, id);
        if (rows == 0) return;
        GridConfig config = new GridStore(context).loadForUpdate(id, rows);
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_grid);
        int alpha = Color.alpha(config.background);
        if (alpha != 0) {
            views.setViewVisibility(R.id.widget_panel, View.VISIBLE);
            views.setInt(R.id.widget_panel, "setColorFilter", config.background | 0xFF000000);
            views.setInt(R.id.widget_panel, "setImageAlpha", alpha);
        } else {
            views.setViewVisibility(R.id.widget_panel, View.GONE);
        }
        int secondRowVisibility = config.rows == 2 ? View.VISIBLE : View.GONE;
        views.setViewVisibility(R.id.widget_top_gap, secondRowVisibility);
        views.setViewVisibility(R.id.widget_row_gap, secondRowVisibility);
        views.setViewVisibility(R.id.second_row, secondRowVisibility);
        views.setViewVisibility(R.id.widget_bottom_gap, secondRowVisibility);
        views.removeAllViews(R.id.first_row);
        views.removeAllViews(R.id.second_row);
        int iconSize = EditorUi.dp(context, config.iconSizeDp);
        for (int index = 0; index < config.visibleSlots(); index++) {
            RemoteViews slot = new RemoteViews(context.getPackageName(), R.layout.widget_slot);
            ShortcutEntry item = config.slots[index];
            if (item != null) {
                slot.setImageViewBitmap(R.id.widget_icon, ShortcutIcons.bitmap(context, item, iconSize));
                slot.setContentDescription(R.id.widget_slot, item.title());
                Uri identity = Uri.parse("shortcutgrid://widget/" + id + "/" + index);
                Intent open = new Intent(context, OpenShortcutActivity.class)
                        .setData(identity)
                        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                        .putExtra(EXTRA_SLOT, index);
                slot.setOnClickPendingIntent(R.id.widget_slot, PendingIntent.getActivity(
                        context, 0, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
            }
            views.addView(index < config.columns ? R.id.first_row : R.id.second_row, slot);
        }
        if (Build.VERSION.SDK_INT >= 31) {
            views = fitWidgetHeight(views, manager.getAppWidgetOptions(id),
                    GridConfig.panelHeightDp(config.rows, config.iconSizeDp));
        }
        manager.updateAppWidget(id, views);
    }

    @android.annotation.TargetApi(31)
    @SuppressWarnings("deprecation")
    private static RemoteViews fitWidgetHeight(RemoteViews views, Bundle options, int desiredDp) {
        List<SizeF> sizes = options.getParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES);
        if (sizes != null && sizes.size() > 1 && sizes.size() <= 16) {
            Map<SizeF, RemoteViews> layouts = new LinkedHashMap<>();
            for (SizeF size : sizes) {
                if (size.getWidth() <= 0 || size.getHeight() <= 0) continue;
                RemoteViews layout = new RemoteViews(views);
                layout.setViewLayoutHeight(R.id.widget_content,
                        Math.min(desiredDp, size.getHeight()), TypedValue.COMPLEX_UNIT_DIP);
                layouts.put(size, layout);
            }
            if (!layouts.isEmpty()) return new RemoteViews(layouts);
        }
        float availableDp = sizes != null && sizes.size() == 1 ? sizes.get(0).getHeight()
                : options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT);
        float height = availableDp > 0 ? Math.min(desiredDp, availableDp) : desiredDp;
        views.setViewLayoutHeight(R.id.widget_content, height, TypedValue.COMPLEX_UNIT_DIP);
        return views;
    }
}

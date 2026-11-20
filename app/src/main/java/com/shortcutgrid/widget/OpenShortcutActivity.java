package com.shortcutgrid.widget;

import android.app.Activity;
import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.os.Bundle;
import android.widget.Toast;

public final class OpenShortcutActivity extends Activity {
    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        AppWidgetManager manager = AppWidgetManager.getInstance(this);
        int id = getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);
        int rows = GridWidget.providerRows(this, manager, id);
        if (rows == 0) {
            finish();
            return;
        }
        GridConfig config = new GridStore(this).load(id, rows);
        int index = getIntent().getIntExtra(GridWidget.EXTRA_SLOT, -1);
        if (index < 0 || index >= config.visibleSlots() || config.slots[index] == null) {
            finish();
            return;
        }
        open(this, config.slots[index], this::finish);
    }

    static void open(Activity activity, ShortcutEntry entry, Runnable done) {
        ShortcutCatalog catalog = new ShortcutCatalog(activity);
        ShortcutCatalog.OpenResult result = catalog.open(entry);
        if (result == ShortcutCatalog.OpenResult.OPENED) {
            done.run();
            return;
        }
        AlertDialog.Builder dialog = new AlertDialog.Builder(activity).setTitle(entry.title());
        if (result == ShortcutCatalog.OpenResult.APP_MISSING) {
            dialog.setMessage("请先安装或启用对应的应用。").setPositiveButton("知道了", null);
        } else {
            dialog.setMessage("当前应用版本无法从桌面打开此入口。你可以在配置页更换快捷方式。")
                    .setNegativeButton("关闭", null);
            if (catalog.hasHome(entry.packageName())) {
                dialog.setPositiveButton("打开应用", (window, which) -> {
                    if (!catalog.openHome(entry.packageName())) {
                        Toast.makeText(activity, "应用暂时无法打开", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
        AlertDialog alert = dialog.create();
        alert.setOnDismissListener(window -> done.run());
        alert.show();
    }
}

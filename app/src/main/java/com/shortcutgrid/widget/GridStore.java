package com.shortcutgrid.widget;

import android.content.Context;
import android.content.SharedPreferences;

final class GridStore {
    private static final String DRAFT = "draft";

    private final SharedPreferences preferences;

    GridStore(Context context) {
        preferences = context.getSharedPreferences("shortcut_grids", Context.MODE_PRIVATE);
    }

    GridConfig draft() {
        return GridConfig.decode(preferences.getString(DRAFT, null), 2);
    }

    void saveDraft(GridConfig config) {
        preferences.edit().putString(DRAFT, config.encode()).apply();
    }

    GridConfig load(int id, int defaultRows) {
        String saved = preferences.getString(key(id), null);
        if (saved != null) return GridConfig.decode(saved, defaultRows);
        GridConfig config = draft();
        config.rows = defaultRows;
        return config;
    }

    GridConfig loadForUpdate(int id, int defaultRows) {
        GridConfig config = load(id, defaultRows);
        if (!preferences.contains(key(id))) save(id, config);
        return config;
    }

    void save(int id, GridConfig config) {
        preferences.edit().putString(key(id), config.encode()).apply();
    }

    void delete(int[] ids) {
        SharedPreferences.Editor edit = preferences.edit();
        for (int id : ids) edit.remove(key(id));
        edit.apply();
    }

    void remap(int[] oldIds, int[] newIds) {
        String[] records = new String[oldIds.length];
        SharedPreferences.Editor edit = preferences.edit();
        for (int i = 0; i < oldIds.length; i++) {
            records[i] = preferences.getString(key(oldIds[i]), null);
            edit.remove(key(oldIds[i]));
        }
        for (int i = 0; i < newIds.length; i++) {
            if (records[i] != null) edit.putString(key(newIds[i]), records[i]);
        }
        edit.apply();
    }

    private static String key(int id) {
        return "widget_" + id;
    }
}

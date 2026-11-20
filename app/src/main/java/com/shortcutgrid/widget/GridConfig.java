package com.shortcutgrid.widget;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

final class GridConfig {
    static final int MAX_SLOTS = 12;
    static final int DEFAULT_BACKGROUND = 0xFFF0F5EB;
    static final int MIN_ICON_DP = 32;
    static final int MAX_ICON_DP = 64;
    static final int DEFAULT_ICON_DP = 48;
    // Match the spacer weight in widget_grid.xml so the preview fits like the desktop widget.
    static final float ROW_GAP_WEIGHT = .30f;

    int rows;
    int columns = 4;
    int background = DEFAULT_BACKGROUND;
    int iconSizeDp = DEFAULT_ICON_DP;
    final ShortcutEntry[] slots = new ShortcutEntry[MAX_SLOTS];

    private GridConfig(int rows) {
        this.rows = rows;
    }

    static GridConfig defaults(int rows) {
        GridConfig config = new GridConfig(rows);
        Presets[] presets = {Presets.WECHAT_SCAN, Presets.WECHAT_PAY, Presets.ALIPAY_SCAN,
                Presets.ALIPAY_PAY, Presets.FEISHU_SCAN, Presets.ALIPAY_TRANSIT, Presets.UNIONPAY_TRANSIT};
        for (int index = 0; index < presets.length; index++) {
            config.slots[index] = presets[index].entry;
        }
        return config;
    }

    int visibleSlots() {
        return rows * columns;
    }

    static int panelHeightDp(int rows, int iconSizeDp) {
        float weight = rows == 2 ? rows + ROW_GAP_WEIGHT : rows;
        return Math.round(weight * Math.max(48, iconSizeDp + 10)) + 8;
    }

    boolean hasVisibleShortcut() {
        for (int index = 0; index < visibleSlots(); index++) {
            if (slots[index] != null) return true;
        }
        return false;
    }

    String encode() {
        try {
            JSONArray values = new JSONArray();
            for (ShortcutEntry entry : slots) {
                values.put(entry == null ? JSONObject.NULL : entry.toJson());
            }
            return new JSONObject().put("rows", rows).put("columns", columns)
                    .put("background", background).put("iconSizeDp", iconSizeDp)
                    .put("slots", values).toString();
        } catch (JSONException e) {
            throw new IllegalStateException(e);
        }
    }

    static GridConfig decode(String data, int defaultRows) {
        if (data == null) return defaults(defaultRows);
        try {
            JSONObject value = new JSONObject(data);
            int rows = Math.max(1, Math.min(2, value.optInt("rows", defaultRows)));
            GridConfig config = new GridConfig(rows);
            config.columns = Math.max(4, Math.min(6, value.optInt("columns", 4)));
            config.background = value.optInt("background", DEFAULT_BACKGROUND);
            config.iconSizeDp = Math.max(MIN_ICON_DP,
                    Math.min(MAX_ICON_DP, value.optInt("iconSizeDp", DEFAULT_ICON_DP)));
            JSONArray entries = value.getJSONArray("slots");
            for (int index = 0; index < Math.min(entries.length(), MAX_SLOTS); index++) {
                JSONObject entry = entries.optJSONObject(index);
                if (entry != null) config.slots[index] = ShortcutEntry.fromJson(entry);
            }
            return config;
        } catch (JSONException e) {
            return defaults(defaultRows);
        }
    }
}

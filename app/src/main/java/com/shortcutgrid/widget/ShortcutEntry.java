package com.shortcutgrid.widget;

import org.json.JSONException;
import org.json.JSONObject;

record ShortcutEntry(Kind kind, String title, String packageName, String target,
                     Glyph glyph, String iconResource) {
    enum Kind { PRESET, APP, LINK, ANDROID_SHORTCUT }
    enum Glyph { APP, SCAN, PAY, TRANSIT, LINK }

    ShortcutEntry withGlyph(Glyph value) {
        return new ShortcutEntry(kind, title, packageName, target, value, iconResource);
    }

    JSONObject toJson() throws JSONException {
        return new JSONObject()
                .put("kind", kind.name())
                .put("title", title)
                .put("package", packageName)
                .put("target", target)
                .put("glyph", glyph.name())
                .put("iconResource", iconResource);
    }

    static ShortcutEntry fromJson(JSONObject value) {
        try {
            Kind kind = Kind.valueOf(value.getString("kind"));
            String target = value.optString("target");
            if (kind == Kind.PRESET && Presets.find(target) == null) return null;
            return new ShortcutEntry(kind, value.getString("title"), value.getString("package"),
                    target, Glyph.valueOf(value.optString("glyph", "APP")),
                    value.optString("iconResource"));
        } catch (IllegalArgumentException | JSONException e) {
            return null;
        }
    }
}

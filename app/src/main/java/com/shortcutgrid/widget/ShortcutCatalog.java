package com.shortcutgrid.widget;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.net.Uri;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class ShortcutCatalog {
    private static final String ANDROID = "http://schemas.android.com/apk/res/android";
    private static final String SHORTCUT_METADATA = "android.app.shortcuts";
    private static final int MAX_TARGET_LENGTH = 4096;

    record App(String name, String packageName) {}
    record Creator(String name, ComponentName component) {}
    enum OpenResult { OPENED, APP_MISSING, SHORTCUT_UNAVAILABLE }

    private final Context context;
    private final PackageManager packages;

    ShortcutCatalog(Context context) {
        this.context = context;
        packages = context.getPackageManager();
    }

    List<App> apps() {
        Map<String, App> apps = new LinkedHashMap<>();
        List<ResolveInfo> candidates = new ArrayList<>(packages.queryIntentActivities(mainIntent(), 0));
        candidates.addAll(packages.queryIntentActivities(new Intent(Intent.ACTION_CREATE_SHORTCUT), 0));
        for (ResolveInfo candidate : candidates) {
            ActivityInfo activity = candidate.activityInfo;
            if (activity == null || !isPublic(activity)
                    || context.getPackageName().equals(activity.packageName)) continue;
            apps.putIfAbsent(activity.packageName, new App(
                    activity.applicationInfo.loadLabel(packages).toString(), activity.packageName));
        }
        List<App> sorted = new ArrayList<>(apps.values());
        Collator collator = Collator.getInstance();
        sorted.sort((first, second) -> collator.compare(first.name(), second.name()));
        return sorted;
    }

    List<Creator> creators(String packageName) {
        List<Creator> creators = new ArrayList<>();
        Intent request = new Intent(Intent.ACTION_CREATE_SHORTCUT).setPackage(packageName);
        for (ResolveInfo candidate : packages.queryIntentActivities(request, 0)) {
            ActivityInfo activity = candidate.activityInfo;
            if (activity != null && isPublic(activity)) {
                creators.add(new Creator(candidate.loadLabel(packages).toString(),
                        new ComponentName(activity.packageName, activity.name)));
            }
        }
        return creators;
    }

    List<ShortcutEntry> published(String packageName) {
        final Resources appResources;
        try {
            appResources = packages.getResourcesForApplication(packageName);
        } catch (PackageManager.NameNotFoundException e) {
            return Collections.emptyList();
        }
        Map<String, ShortcutEntry> entries = new LinkedHashMap<>();
        for (ResolveInfo resolved : packages.queryIntentActivities(
                mainIntent().setPackage(packageName), PackageManager.GET_META_DATA)) {
            if (resolved.activityInfo == null) continue;
            try (XmlResourceParser xml = resolved.activityInfo.loadXmlMetaData(packages, SHORTCUT_METADATA)) {
                if (xml == null) continue;
                while (xml.next() != XmlPullParser.END_DOCUMENT) {
                    if (xml.getEventType() == XmlPullParser.START_TAG && xml.getDepth() == 2
                            && "shortcut".equals(xml.getName())) {
                        ShortcutEntry item = readShortcut(xml, appResources, packageName);
                        if (item != null) entries.putIfAbsent(item.title() + "\n" + item.target(), item);
                    }
                }
            } catch (IOException | XmlPullParserException | RuntimeException ignored) {
                // Ignore this package's malformed or inaccessible resources.
            }
        }
        return new ArrayList<>(entries.values());
    }

    private ShortcutEntry readShortcut(XmlResourceParser xml, Resources resources, String packageName)
            throws IOException, XmlPullParserException {
        int depth = xml.getDepth();
        if (!xml.getAttributeBooleanValue(ANDROID, "enabled", true)) return null;
        int labelId = xml.getAttributeResourceValue(ANDROID, "shortcutShortLabel", 0);
        if (labelId == 0) return null;
        String label = resources.getText(labelId).toString();
        int iconId = xml.getAttributeResourceValue(ANDROID, "icon", 0);
        String icon = iconId == 0 ? "" : resources.getResourceName(iconId);
        Intent destination = null;
        while (xml.next() != XmlPullParser.END_DOCUMENT) {
            if (xml.getEventType() == XmlPullParser.END_TAG && xml.getDepth() == depth) break;
            if (xml.getEventType() == XmlPullParser.START_TAG && xml.getDepth() == depth + 1
                    && "intent".equals(xml.getName())) {
                destination = Intent.parseIntent(resources, xml, xml);
            }
        }
        return imported(packageName, label, destination, icon);
    }

    ShortcutEntry imported(String packageName, String label, Intent destination, String icon) {
        Intent clean = forPackage(destination, packageName);
        if (!canOpen(clean)) return null;
        String serialized = clean.toUri(Intent.URI_INTENT_SCHEME);
        if (serialized.length() > MAX_TARGET_LENGTH) return null;
        return new ShortcutEntry(ShortcutEntry.Kind.ANDROID_SHORTCUT, label, packageName,
                serialized, ShortcutEntry.Glyph.APP, icon);
    }

    ShortcutEntry app(App app) {
        return new ShortcutEntry(ShortcutEntry.Kind.APP, app.name(), app.packageName(),
                "", ShortcutEntry.Glyph.APP, "");
    }

    ShortcutEntry link(String packageName, String label, String url, ShortcutEntry.Glyph glyph) {
        Intent intent = linkIntent(packageName, url);
        if (!canOpen(intent)) return null;
        return new ShortcutEntry(ShortcutEntry.Kind.LINK, label, packageName, url, glyph, "");
    }

    boolean installed(String packageName) {
        try {
            return packages.getApplicationInfo(packageName, 0).enabled;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    boolean hasHome(String packageName) {
        return packages.getLaunchIntentForPackage(packageName) != null;
    }

    boolean openHome(String packageName) {
        return start(packages.getLaunchIntentForPackage(packageName));
    }

    OpenResult open(ShortcutEntry entry) {
        if (!installed(entry.packageName())) return OpenResult.APP_MISSING;
        boolean opened = switch (entry.kind()) {
            case APP -> openHome(entry.packageName());
            case LINK -> start(linkIntent(entry.packageName(), entry.target()));
            case ANDROID_SHORTCUT -> start(androidIntent(entry.packageName(), entry.target()));
            case PRESET -> openPreset(entry);
        };
        return opened ? OpenResult.OPENED : OpenResult.SHORTCUT_UNAVAILABLE;
    }

    private boolean openPreset(ShortcutEntry entry) {
        Presets preset = Presets.find(entry.target());
        if (preset == null || !preset.entry.packageName().equals(entry.packageName())) return false;
        for (ShortcutEntry shortcut : published(entry.packageName())) {
            if (matchesPreset(preset, shortcut.title())
                    && start(androidIntent(shortcut.packageName(), shortcut.target()))) return true;
        }
        for (String target : preset.targets) {
            Intent intent = target.startsWith("intent:")
                    ? androidIntent(entry.packageName(), target)
                    : linkIntent(entry.packageName(), target);
            if (start(intent)) return true;
        }
        return false;
    }

    private static boolean matchesPreset(Presets preset, String title) {
        String label = title.replace(" ", "");
        if (label.equals(preset.entry.title())) return true;
        return switch (preset.entry.glyph()) {
            case SCAN -> label.equals("扫一扫") || label.equals("扫码") || label.equals("扫码用车")
                    || label.equals("扫码骑行");
            case PAY -> label.equals("付款码") || label.equals("支付码") || label.equals("收付款")
                    || label.equals("收/付款");
            case TRANSIT -> label.equals("乘车码") || label.equals("出行码");
            default -> false;
        };
    }

    private boolean start(Intent intent) {
        if (!canOpen(intent)) return false;
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            return true;
        } catch (ActivityNotFoundException | SecurityException e) {
            return false;
        }
    }

    private boolean canOpen(Intent intent) {
        if (intent == null) return false;
        for (ResolveInfo resolve : packages.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)) {
            ActivityInfo activity = resolve.activityInfo;
            if (activity != null && isPublic(activity)
                    && !context.getPackageName().equals(activity.packageName)) return true;
        }
        return false;
    }

    private boolean isPublic(ActivityInfo activity) {
        return activity.exported && activity.enabled && (activity.permission == null
                || context.checkSelfPermission(activity.permission) == PackageManager.PERMISSION_GRANTED);
    }

    private static Intent mainIntent() {
        return new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
    }

    private static Intent linkIntent(String packageName, String url) {
        if (url.isEmpty() || url.length() > MAX_TARGET_LENGTH || url.matches("(?s).*\\s.*")) return null;
        if (url.startsWith("intent:")) {
            Intent intent = androidIntent(packageName, url);
            return intent != null && Intent.ACTION_VIEW.equals(intent.getAction()) ? intent : null;
        }
        Uri uri = Uri.parse(url);
        if (uri.getScheme() == null || !allowedScheme(uri)) return null;
        return new Intent(Intent.ACTION_VIEW, uri).setPackage(packageName);
    }

    private static Intent androidIntent(String packageName, String target) {
        try {
            return forPackage(Intent.parseUri(target, Intent.URI_INTENT_SCHEME), packageName);
        } catch (URISyntaxException | IllegalArgumentException e) {
            return null;
        }
    }

    private static Intent forPackage(Intent source, String packageName) {
        if (source == null || source.getAction() == null || source.getSelector() != null) return null;
        if (source.getComponent() != null
                && !source.getComponent().getPackageName().equals(packageName)) return null;
        if (source.getPackage() != null && !source.getPackage().equals(packageName)) return null;
        if (source.getData() != null && !allowedScheme(source.getData())) return null;
        Intent clean = new Intent(source).setPackage(packageName).setFlags(0);
        clean.setClipData(null);
        return clean;
    }

    private static boolean allowedScheme(Uri uri) {
        String scheme = uri.getScheme();
        if (scheme == null) return false;
        return switch (scheme.toLowerCase(Locale.ROOT)) {
            case "javascript", "file", "content", "data", "android.resource", "package", "intent" -> false;
            case "http", "https" -> uri.getHost() != null;
            default -> scheme.matches("[a-zA-Z][a-zA-Z0-9+.-]*");
        };
    }
}

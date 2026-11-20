package com.shortcutgrid.widget;

import static com.shortcutgrid.widget.EditorUi.*;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class EditorActivity extends Activity {
    private static final int NO_WIDGET = AppWidgetManager.INVALID_APPWIDGET_ID;
    private static final int CREATE_SHORTCUT = 100;
    private static final int SWATCHES_PER_ROW = 6;
    private record Background(int color, String label) {}
    private static final Background[] BACKGROUNDS = {
            new Background(Color.TRANSPARENT, "透明"),
            new Background(Color.WHITE, "白色"),
            new Background(0xFFF3F1E9, "米白"),
            new Background(GridConfig.DEFAULT_BACKGROUND, "浅绿"),
            new Background(0xFFDDE5EB, "雾灰"),
            new Background(0xFF263733, "深绿"),
            new Background(0xFFFFE9CD, "暖杏"),
            new Background(0xFFFFF2B8, "奶黄"),
            new Background(0xFFF7DCE4, "玫粉"),
            new Background(0xFFEAE0F6, "丁香紫"),
            new Background(0xFFDCEAFE, "晴空蓝"),
            new Background(0xFFCFEBE5, "海盐绿")
    };

    private AppWidgetManager widgets;
    private GridStore store;
    private ShortcutPicker picker;
    private boolean hostConfiguration;
    private int widgetId = NO_WIDGET;
    private GridConfig config;
    private int moving = -1;
    private int importing = -1;
    private String importPackage;
    private String importLabel;
    private OnBackInvokedCallback backCallback;

    private FrameLayout preview;
    private TextView hint;
    private LinearLayout rowControls;
    private LinearLayout columnControls;
    private LinearLayout backgroundControls;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        widgets = AppWidgetManager.getInstance(this);
        store = new GridStore(this);
        picker = new ShortcutPicker(this);
        hostConfiguration = AppWidgetManager.ACTION_APPWIDGET_CONFIGURE.equals(getIntent().getAction());
        if (hostConfiguration) {
            widgetId = getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, NO_WIDGET);
            setResult(RESULT_CANCELED, resultIntent());
            if (GridWidget.providerRows(this, widgets, widgetId) == 0) {
                finish();
                return;
            }
        }
        if (state != null) {
            if (!hostConfiguration) {
                int previousId = state.getInt("widget", NO_WIDGET);
                if (GridWidget.providerRows(this, widgets, previousId) != 0) widgetId = previousId;
            }
            config = GridConfig.decode(state.getString("configuration"), 2);
            moving = state.getInt("moving", -1);
            importing = state.getInt("importing", -1);
            importPackage = state.getString("importPackage");
            importLabel = state.getString("importLabel");
        } else {
            config = widgetId == NO_WIDGET ? store.draft()
                    : store.load(widgetId, GridWidget.providerRows(this, widgets, widgetId));
        }
        buildScreen();
    }

    private void buildScreen() {
        LinearLayout root = column(this);
        root.setBackgroundColor(PAGE);
        setContentView(root);
        applyInsets(root);

        LinearLayout header = row(this);
        padding(header, 18, 0);
        root.addView(header, new LinearLayout.LayoutParams(MATCH, dp(this, 66)));
        if (widgetId == NO_WIDGET) {
            ImageView logo = new ImageView(this);
            logo.setImageResource(R.drawable.ic_app);
            logo.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            header.addView(logo, new LinearLayout.LayoutParams(dp(this, 31), dp(this, 31)));
        } else {
            Button back = button(this, "‹", INK, 0xFFE8EEE8, 13);
            back.setTextSize(28);
            back.setContentDescription("返回");
            back.setOnClickListener(view -> navigateBack());
            header.addView(back, new LinearLayout.LayoutParams(dp(this, 40), dp(this, 44)));
        }

        TextView title = text(this, widgetId == NO_WIDGET ? "快捷宫格" : "设置小组件", 24, INK, true);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, WRAP, 1);
        titleParams.setMarginStart(dp(this, 10));
        header.addView(title, titleParams);
        if (widgetId == NO_WIDGET) {
            Button manage = button(this, "管理", ACCENT, 0xFFE6EFE8, 16);
            manage.setContentDescription("管理桌面小组件");
            manage.setOnClickListener(view -> manageWidgets());
            header.addView(manage, new LinearLayout.LayoutParams(dp(this, 66), dp(this, 44)));
        }

        ScrollView scroll = new ScrollView(this);
        scroll.setClipToPadding(false);
        scroll.setVerticalScrollBarEnabled(false);
        root.addView(scroll, new LinearLayout.LayoutParams(MATCH, 0, 1));
        LinearLayout body = column(this);
        padding(body, 18, 4);
        scroll.addView(body, new ScrollView.LayoutParams(MATCH, WRAP));

        body.addView(text(this, "桌面预览", 16, INK, true));
        gap(body, 9);
        preview = new FrameLayout(this);
        body.addView(preview, new LinearLayout.LayoutParams(MATCH, WRAP));
        gap(body, 7);
        hint = text(this, "", 12, MUTED, false);
        body.addView(hint);
        gap(body, 17);

        LinearLayout layout = card("布局");
        body.addView(layout, new LinearLayout.LayoutParams(MATCH, WRAP));
        gap(layout, 7);
        rowControls = row(this);
        layout.addView(rowControls, new LinearLayout.LayoutParams(MATCH, dp(this, 48)));
        gap(layout, 3);
        columnControls = row(this);
        layout.addView(columnControls, new LinearLayout.LayoutParams(MATCH, dp(this, 48)));
        gap(layout, 3);
        layout.addView(iconSizeControls(), new LinearLayout.LayoutParams(MATCH, dp(this, 48)));

        gap(body, 12);
        LinearLayout background = card(null);
        body.addView(background, new LinearLayout.LayoutParams(MATCH, WRAP));
        LinearLayout backgroundHeader = row(this);
        backgroundHeader.addView(text(this, "背景", 16, INK, true),
                new LinearLayout.LayoutParams(0, WRAP, 1));
        Button moreColors = button(this, "更多颜色", ACCENT, PAGE, 11);
        moreColors.setOnClickListener(view -> BackgroundColorDialog.show(this, config.background, color -> {
            config.background = color;
            changed();
        }));
        backgroundHeader.addView(moreColors, new LinearLayout.LayoutParams(dp(this, 86), dp(this, 44)));
        background.addView(backgroundHeader, new LinearLayout.LayoutParams(MATCH, WRAP));
        backgroundControls = column(this);
        background.addView(backgroundControls, new LinearLayout.LayoutParams(MATCH, WRAP));
        gap(body, 12);

        FrameLayout actionArea = new FrameLayout(this);
        padding(actionArea, 18, 9);
        actionArea.setBackgroundColor(PAGE);
        root.addView(actionArea, new LinearLayout.LayoutParams(MATCH, WRAP));
        Button action = button(this, widgetId == NO_WIDGET ? "＋  添加到桌面" : "保存到桌面",
                Color.WHITE, ACCENT, 18);
        action.setTextSize(16);
        action.setOnClickListener(view -> saveOrPin());
        actionArea.addView(action, new FrameLayout.LayoutParams(MATCH, dp(this, 52)));

        render();
    }

    private void applyInsets(View root) {
        Window window = getWindow();
        if (Build.VERSION.SDK_INT >= 30) {
            window.setDecorFitsSystemWindows(false);
            WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
                int lightBars = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                        | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
                controller.setSystemBarsAppearance(lightBars, lightBars);
            }
            root.setOnApplyWindowInsetsListener((view, insets) -> {
                android.graphics.Insets bars = insets.getInsets(
                        WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                return WindowInsets.CONSUMED;
            });
        } else {
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
            root.setOnApplyWindowInsetsListener((view, insets) -> {
                view.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                        insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
                return insets.consumeSystemWindowInsets();
            });
        }
        root.requestApplyInsets();
    }

    private LinearLayout card(String title) {
        LinearLayout card = column(this);
        card.setBackground(shape(this, Color.WHITE, 22));
        padding(card, 15, 12);
        if (title != null) card.addView(text(this, title, 16, INK, true));
        return card;
    }

    private void render() {
        renderPreview();
        layoutChoices(rowControls, "行数", 1, 2, true);
        layoutChoices(columnControls, "每行图标", 4, 6, false);
        backgroundChoices();
    }

    private void renderPreview() {
        preview.removeAllViews();
        PreviewSurface surface = new PreviewSurface(this, config, moving, this::tapSlot, this::holdSlot);
        preview.addView(surface, new FrameLayout.LayoutParams(MATCH, dp(this, PreviewSurface.heightDp(config.rows))));
        hint.setText(moving >= 0 ? "点按新位置以交换图标" : "点按更换快捷方式，长按可移动或移除");
        updateBackCallback();
    }

    private void layoutChoices(LinearLayout line, String label, int from, int to, boolean rows) {
        line.removeAllViews();
        TextView name = text(this, label, 14, MUTED, false);
        line.addView(name, new LinearLayout.LayoutParams(dp(this, 84), WRAP));
        for (int value = from; value <= to; value++) {
            int choice = value;
            boolean selected = choice == (rows ? config.rows : config.columns);
            Button chip = button(this, rows ? choice + " 行" : String.valueOf(choice),
                    selected ? ACCENT : INK, selected ? SELECTED : PAGE, 12);
            chip.setSelected(selected);
            if (!rows) chip.setContentDescription("每行 " + choice + " 个图标");
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(this, 44), 1);
            if (choice != from) params.setMarginStart(dp(this, 6));
            line.addView(chip, params);
            chip.setOnClickListener(view -> {
                if (rows) config.rows = choice;
                else config.columns = choice;
                moving = -1;
                changed();
            });
        }
    }

    private LinearLayout iconSizeControls() {
        LinearLayout line = row(this);
        line.addView(text(this, "图标大小", 14, MUTED, false),
                new LinearLayout.LayoutParams(dp(this, 75), WRAP));
        SeekBar size = new SeekBar(this);
        size.setMax(GridConfig.MAX_ICON_DP - GridConfig.MIN_ICON_DP);
        size.setProgress(config.iconSizeDp - GridConfig.MIN_ICON_DP);
        size.setKeyProgressIncrement(2);
        size.setContentDescription("图标大小");
        line.addView(size, new LinearLayout.LayoutParams(0, MATCH, 1));
        TextView value = text(this, config.iconSizeDp + " dp", 13, ACCENT, true);
        value.setGravity(android.view.Gravity.END | android.view.Gravity.CENTER_VERTICAL);
        line.addView(value, new LinearLayout.LayoutParams(dp(this, 48), MATCH));
        onSeek(size, progress -> {
            config.iconSizeDp = progress + GridConfig.MIN_ICON_DP;
            value.setText(config.iconSizeDp + " dp");
            if (widgetId == NO_WIDGET) store.saveDraft(config);
            renderPreview();
        });
        return line;
    }

    private void backgroundChoices() {
        backgroundControls.removeAllViews();
        LinearLayout line = null;
        for (int index = 0; index < BACKGROUNDS.length; index++) {
            if (index % SWATCHES_PER_ROW == 0) {
                line = row(this);
                backgroundControls.addView(line, new LinearLayout.LayoutParams(MATCH, dp(this, 44)));
            }
            Background option = BACKGROUNDS[index];
            BackgroundSwatch swatch = new BackgroundSwatch(this, option.color(),
                    option.label() + "背景", option.color() == config.background);
            line.addView(swatch, new LinearLayout.LayoutParams(0, MATCH, 1));
            swatch.setOnClickListener(view -> {
                config.background = option.color();
                changed();
            });
        }
    }

    private void tapSlot(int slot) {
        if (moving >= 0) {
            ShortcutEntry previous = config.slots[slot];
            config.slots[slot] = config.slots[moving];
            config.slots[moving] = previous;
            moving = -1;
            changed();
        } else {
            picker.show(slot);
        }
    }

    private void holdSlot(int slot) {
        if (moving >= 0) {
            tapSlot(slot);
            return;
        }
        ShortcutEntry current = config.slots[slot];
        if (current == null) return;
        String[] options = {"测试打开", "更换图标", "移动位置", "移除"};
        new AlertDialog.Builder(this).setTitle(current.title()).setItems(options, (dialog, index) -> {
            switch (index) {
                case 0 -> OpenShortcutActivity.open(this, current, () -> {});
                case 1 -> picker.changeIcon(slot, current);
                case 2 -> {
                    moving = slot;
                    renderPreview();
                }
                case 3 -> setSlot(slot, null);
                default -> {}
            }
        }).show();
    }

    void setSlot(int slot, ShortcutEntry entry) {
        if (slot < 0 || slot >= GridConfig.MAX_SLOTS) return;
        config.slots[slot] = entry;
        changed();
    }

    private void changed() {
        if (widgetId == NO_WIDGET) store.saveDraft(config);
        render();
    }

    void createShortcut(int slot, ShortcutCatalog.App app, ShortcutCatalog.Creator creator) {
        importing = slot;
        importPackage = app.packageName();
        importLabel = creator.name();
        try {
            startActivityForResult(new Intent(Intent.ACTION_CREATE_SHORTCUT)
                    .setComponent(creator.component()), CREATE_SHORTCUT);
        } catch (android.content.ActivityNotFoundException | SecurityException e) {
            importing = -1;
            toast("应用的快捷方式设置已不可用");
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request != CREATE_SHORTCUT || importing < 0) return;
        int slot = importing;
        importing = -1;
        if (result != RESULT_OK || data == null || importPackage == null) return;
        try {
            Intent destination = shortcutExtra(data, Intent.EXTRA_SHORTCUT_INTENT, Intent.class);
            Intent.ShortcutIconResource icon = shortcutExtra(data, Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                    Intent.ShortcutIconResource.class);
            String iconName = icon != null && icon.resourceName != null
                    && importPackage.equals(icon.packageName) ? icon.resourceName : "";
            String label = data.getStringExtra(Intent.EXTRA_SHORTCUT_NAME);
            ShortcutEntry entry = new ShortcutCatalog(this).imported(importPackage,
                    label == null || label.trim().isEmpty() ? importLabel : label, destination, iconName);
            if (entry != null) {
                setSlot(slot, entry);
                return;
            }
        } catch (RuntimeException ignored) {
            // Malformed shortcut data is handled the same as an unsupported result.
        }
        new AlertDialog.Builder(this).setTitle("无法导入快捷方式")
                .setMessage("此应用返回的入口只能由系统桌面打开，或已不可用。可以试试应用提供的功能链接。")
                .setPositiveButton("知道了", null).show();
    }

    @SuppressWarnings("deprecation")
    private static <T extends android.os.Parcelable> T shortcutExtra(Intent data, String key, Class<T> type) {
        return Build.VERSION.SDK_INT >= 33 ? data.getParcelableExtra(key, type)
                : type.cast(data.getParcelableExtra(key));
    }

    private void manageWidgets() {
        List<Integer> ids = new ArrayList<>();
        for (int rows = 1; rows <= 2; rows++) {
            for (int id : widgets.getAppWidgetIds(GridWidget.component(this, rows))) ids.add(id);
        }
        if (ids.isEmpty()) {
            toast("桌面上还没有添加快捷宫格");
            return;
        }
        String[] labels = new String[ids.size()];
        for (int index = 0; index < ids.size(); index++) {
            int id = ids.get(index);
            GridConfig saved = store.load(id, GridWidget.providerRows(this, widgets, id));
            labels[index] = (index + 1) + "   ·   " + saved.rows + " 行，每行 " + saved.columns + " 个位置";
        }
        new AlertDialog.Builder(this).setTitle("管理桌面小组件").setItems(labels, (dialog, index) -> {
            int id = ids.get(index);
            int rows = GridWidget.providerRows(this, widgets, id);
            if (rows == 0) {
                toast("这个小组件已从桌面移除");
                return;
            }
            widgetId = id;
            config = store.load(id, rows);
            moving = -1;
            buildScreen();
        }).show();
    }

    private Intent resultIntent() {
        return new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
    }

    private void saveOrPin() {
        if (!config.hasVisibleShortcut()) {
            toast("请先在预览中添加一个快捷方式");
            return;
        }
        if (widgetId != NO_WIDGET) {
            if (GridWidget.providerRows(this, widgets, widgetId) == 0) {
                toast("这个小组件已从桌面移除");
                navigateBack();
                return;
            }
            store.save(widgetId, config);
            GridWidget.update(this, widgets, widgetId);
            if (hostConfiguration) {
                setResult(RESULT_OK, resultIntent());
                finish();
            } else {
                toast("已更新桌面小组件");
                returnToDraft();
            }
            return;
        }
        store.saveDraft(config);
        if (widgets.isRequestPinAppWidgetSupported()) {
            Intent callback = new Intent(this, WidgetPinnedReceiver.class)
                    .setData(Uri.parse("shortcutgrid://pin/" + UUID.randomUUID()))
                    .putExtra(WidgetPinnedReceiver.EXTRA_CONFIGURATION, config.encode());
            int flags = PendingIntent.FLAG_ONE_SHOT;
            if (Build.VERSION.SDK_INT >= 31) flags |= PendingIntent.FLAG_MUTABLE;
            PendingIntent confirmation = PendingIntent.getBroadcast(this, 0, callback, flags);
            try {
                if (widgets.requestPinAppWidget(GridWidget.component(this, config.rows), null, confirmation)) return;
            } catch (IllegalStateException ignored) {
                // Launcher pinning is unavailable while the device or user profile is locked.
            }
            confirmation.cancel();
        }
        new AlertDialog.Builder(this).setTitle("从桌面添加")
                .setMessage("长按桌面空白处，打开「小组件」，选择「快捷宫格」单行或双行。")
                .setPositiveButton("知道了", null).show();
    }

    private void returnToDraft() {
        widgetId = NO_WIDGET;
        config = store.draft();
        moving = -1;
        buildScreen();
    }

    private void navigateBack() {
        if (moving >= 0) {
            moving = -1;
            renderPreview();
        } else if (!hostConfiguration && widgetId != NO_WIDGET) {
            returnToDraft();
        } else {
            finish();
        }
    }

    private boolean hasEditorBackAction() {
        return moving >= 0 || (!hostConfiguration && widgetId != NO_WIDGET);
    }

    private void updateBackCallback() {
        if (Build.VERSION.SDK_INT < 33) return;
        if (hasEditorBackAction() && backCallback == null) {
            backCallback = this::navigateBack;
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    OnBackInvokedDispatcher.PRIORITY_DEFAULT, backCallback);
        } else if (!hasEditorBackAction() && backCallback != null) {
            getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback(backCallback);
            backCallback = null;
        }
    }

    @Override
    @SuppressLint("GestureBackNavigation")
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        // Android 13 and newer use the dispatcher, while older releases call this override.
        if (hasEditorBackAction()) navigateBack();
        else super.onBackPressed();
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        if (config == null) return;
        state.putInt("widget", widgetId);
        state.putString("configuration", config.encode());
        state.putInt("moving", moving);
        state.putInt("importing", importing);
        state.putString("importPackage", importPackage);
        state.putString("importLabel", importLabel);
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}

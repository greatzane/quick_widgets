package com.shortcutgrid.widget;

import static com.shortcutgrid.widget.EditorUi.*;

import android.app.AlertDialog;
import android.graphics.Color;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class ShortcutPicker {
    private static final ShortcutEntry.Glyph[] GLYPHS = ShortcutEntry.Glyph.values();
    private static final String[] GLYPH_LABELS = {"应用图标", "扫一扫", "付款码", "乘车码", "功能链接"};

    private record Choice(String title, String detail, ShortcutEntry icon, Runnable select) {}

    private final EditorActivity activity;
    private final ShortcutCatalog catalog;

    ShortcutPicker(EditorActivity activity) {
        this.activity = activity;
        catalog = new ShortcutCatalog(activity);
    }

    void show(int slot) {
        show(slot, false);
    }

    private void show(int slot, boolean showApps) {
        LinearLayout content = column(activity);
        padding(content, 18, 6);
        LinearLayout tabs = row(activity);
        content.addView(tabs, new LinearLayout.LayoutParams(MATCH, dp(activity, 44)));
        Button common = button(activity, "常用功能", INK, PAGE, 12);
        Button apps = button(activity, "已安装应用", INK, PAGE, 12);
        tabs.addView(common, new LinearLayout.LayoutParams(0, MATCH, 1));
        LinearLayout.LayoutParams appsParams = new LinearLayout.LayoutParams(0, MATCH, 1);
        appsParams.setMarginStart(dp(activity, 6));
        tabs.addView(apps, appsParams);
        gap(content, 5);

        EditText search = field("搜索应用名称或包名", 100);
        content.addView(search, new LinearLayout.LayoutParams(MATCH, dp(activity, 49)));
        ListView list = new ListView(activity);
        list.setDivider(null);
        content.addView(list, new LinearLayout.LayoutParams(MATCH, listHeight(7)));
        TextView empty = text(activity, "没有找到应用", 14, MUTED, false);
        padding(empty, 10, 18);
        content.addView(empty, new LinearLayout.LayoutParams(MATCH, WRAP));
        list.setEmptyView(empty);
        content.setFocusableInTouchMode(true);
        content.requestFocus();

        AlertDialog dialog = new AlertDialog.Builder(activity).setTitle("选择快捷方式")
                .setView(content).setNegativeButton("取消", null).create();
        List<Choice> presets = new ArrayList<>();
        for (Presets preset : Presets.values()) {
            ShortcutEntry item = preset.entry;
            String availability = catalog.installed(item.packageName()) ? "" : "需安装对应应用";
            presets.add(new Choice(item.title(), availability, item, () -> activity.setSlot(slot, item)));
        }
        ChoiceAdapter adapter = new ChoiceAdapter();
        list.setAdapter(adapter);
        list.setOnItemClickListener((parent, view, position, id) -> {
            Choice item = adapter.getItem(position);
            dialog.dismiss();
            item.select().run();
        });

        Runnable updateApps = () -> {
            String filter = search.getText().toString().trim().toLowerCase(Locale.ROOT);
            List<Choice> filtered = new ArrayList<>();
            for (ShortcutCatalog.App app : catalog.apps()) {
                if (!app.name().toLowerCase(Locale.ROOT).contains(filter)
                        && !app.packageName().toLowerCase(Locale.ROOT).contains(filter)) continue;
                filtered.add(new Choice(app.name(), app.packageName(), catalog.app(app),
                        () -> appChoices(slot, app)));
            }
            adapter.replace(filtered);
        };
        common.setOnClickListener(view -> {
            search.setVisibility(View.GONE);
            content.requestFocus();
            InputMethodManager input = activity.getSystemService(InputMethodManager.class);
            if (input != null) input.hideSoftInputFromWindow(search.getWindowToken(), 0);
            setTab(common, true);
            setTab(apps, false);
            adapter.replace(presets);
            list.setSelection(0);
        });
        apps.setOnClickListener(view -> {
            search.setVisibility(View.VISIBLE);
            setTab(common, false);
            setTab(apps, true);
            updateApps.run();
            list.setSelection(0);
        });
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateApps.run();
                list.setSelection(0);
            }
            @Override public void afterTextChanged(Editable s) {}
        });
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                | WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        if (showApps) apps.performClick();
        else common.performClick();
        dialog.show();
    }

    private void setTab(Button button, boolean selected) {
        button.setSelected(selected);
        button.setBackground(ripple(activity, selected ? SELECTED : PAGE, 12));
        button.setTextColor(selected ? ACCENT : INK);
    }

    private void appChoices(int slot, ShortcutCatalog.App app) {
        List<Choice> choices = new ArrayList<>();
        for (ShortcutEntry entry : catalog.published(app.packageName())) {
            choices.add(select(slot, entry, "应用提供的快捷方式"));
        }
        for (Presets preset : Presets.values()) {
            if (app.packageName().equals(preset.entry.packageName())) {
                choices.add(select(slot, preset.entry, "常用功能"));
            }
        }
        ShortcutEntry appEntry = catalog.app(app);
        for (ShortcutCatalog.Creator creator : catalog.creators(app.packageName())) {
            choices.add(new Choice(creator.name(), "从应用导入", appEntry,
                    () -> activity.createShortcut(slot, app, creator)));
        }
        choices.add(new Choice("添加功能链接", "使用应用提供的深链接",
                appEntry.withGlyph(ShortcutEntry.Glyph.LINK), () -> customLink(slot, app)));
        if (catalog.hasHome(app.packageName())) {
            choices.add(new Choice("打开应用", "", appEntry, () -> activity.setSlot(slot, appEntry)));
        }

        ListView list = new ListView(activity);
        list.setDivider(null);
        ChoiceAdapter adapter = new ChoiceAdapter();
        list.setAdapter(adapter);
        adapter.replace(choices);
        LinearLayout content = column(activity);
        padding(content, 14, 5);
        content.addView(list, new LinearLayout.LayoutParams(MATCH, listHeight(choices.size())));
        AlertDialog dialog = new AlertDialog.Builder(activity).setTitle(app.name())
                .setView(content).setNegativeButton("返回", (window, which) -> show(slot, true)).create();
        list.setOnItemClickListener((parent, view, position, id) -> {
            Choice item = adapter.getItem(position);
            dialog.dismiss();
            item.select().run();
        });
        dialog.show();
    }

    private Choice select(int slot, ShortcutEntry entry, String detail) {
        return new Choice(entry.title(), detail, entry, () -> activity.setSlot(slot, entry));
    }

    void changeIcon(int slot, ShortcutEntry entry) {
        new AlertDialog.Builder(activity).setTitle("更换图标")
                .setSingleChoiceItems(GLYPH_LABELS, entry.glyph().ordinal(), (window, index) -> {
                    window.dismiss();
                    activity.setSlot(slot, entry.withGlyph(GLYPHS[index]));
                }).setNegativeButton("取消", null).show();
    }

    private void customLink(int slot, ShortcutCatalog.App app) {
        LinearLayout form = column(activity);
        padding(form, 22, 8);
        EditText label = field("快捷方式名称", 64);
        label.setText(app.name());
        form.addView(label, new LinearLayout.LayoutParams(MATCH, WRAP));
        EditText link = field("粘贴功能链接", 4096);
        link.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI
                | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        form.addView(link, new LinearLayout.LayoutParams(MATCH, WRAP));
        gap(form, 8);
        LinearLayout iconRow = row(activity);
        iconRow.addView(text(activity, "图标", 14, MUTED, false),
                new LinearLayout.LayoutParams(dp(activity, 52), WRAP));
        Spinner icons = new Spinner(activity);
        ArrayAdapter<String> labels = new ArrayAdapter<>(activity,
                android.R.layout.simple_spinner_item, GLYPH_LABELS);
        labels.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        icons.setAdapter(labels);
        iconRow.addView(icons, new LinearLayout.LayoutParams(0, dp(activity, 48), 1));
        form.addView(iconRow);

        AlertDialog dialog = new AlertDialog.Builder(activity).setTitle(app.name() + "功能链接")
                .setView(form).setNegativeButton("取消", null).setPositiveButton("添加", null).create();
        dialog.setOnShowListener(window -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
            String title = label.getText().toString().trim();
            String url = link.getText().toString().trim();
            if (title.isEmpty()) title = app.name();
            ShortcutEntry item = catalog.link(app.packageName(), title, url, GLYPHS[icons.getSelectedItemPosition()]);
            if (item == null) {
                link.setError("选定的应用无法打开此链接，请检查链接或尝试其他入口");
                return;
            }
            dialog.dismiss();
            activity.setSlot(slot, item);
        }));
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                | WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        dialog.show();
    }

    private EditText field(String placeholder, int maxLength) {
        EditText field = new EditText(activity);
        field.setSingleLine();
        field.setHint(placeholder);
        field.setTextSize(14);
        field.setTextColor(INK);
        field.setHintTextColor(MUTED);
        field.setFilters(new InputFilter[] {new InputFilter.LengthFilter(maxLength)});
        return field;
    }

    private int listHeight(int rows) {
        int available = activity.getResources().getDisplayMetrics().heightPixels;
        return Math.min(dp(activity, Math.min(rows * 58, 326)), Math.round(available * .50f));
    }

    private final class ChoiceAdapter extends BaseAdapter {
        private List<Choice> choices = new ArrayList<>();

        void replace(List<Choice> values) {
            choices = values;
            notifyDataSetChanged();
        }

        @Override public int getCount() { return choices.size(); }
        @Override public Choice getItem(int position) { return choices.get(position); }
        @Override public long getItemId(int position) { return position; }

        @Override
        public View getView(int position, View recycled, ViewGroup parent) {
            ChoiceRow row = recycled instanceof ChoiceRow item ? item : new ChoiceRow();
            row.bind(getItem(position));
            return row;
        }
    }

    private final class ChoiceRow extends LinearLayout {
        private final ImageView image;
        private final TextView name;
        private final TextView detail;

        ChoiceRow() {
            super(activity);
            setOrientation(HORIZONTAL);
            setGravity(android.view.Gravity.CENTER_VERTICAL);
            padding(this, 8, 5);
            setMinimumHeight(dp(activity, 56));
            image = new ImageView(activity);
            image.setScaleType(ImageView.ScaleType.FIT_CENTER);
            image.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            addView(image, new LayoutParams(dp(activity, 37), dp(activity, 37)));
            LinearLayout words = column(activity);
            LayoutParams textParams = new LayoutParams(0, WRAP, 1);
            textParams.setMarginStart(dp(activity, 12));
            addView(words, textParams);
            name = text(activity, "", 15, INK, true);
            name.setSingleLine();
            name.setEllipsize(TextUtils.TruncateAt.END);
            words.addView(name);
            detail = text(activity, "", 12, MUTED, false);
            detail.setSingleLine();
            detail.setEllipsize(TextUtils.TruncateAt.END);
            words.addView(detail);
        }

        void bind(Choice choice) {
            name.setText(choice.title());
            detail.setText(choice.detail());
            detail.setVisibility(choice.detail().isEmpty() ? GONE : VISIBLE);
            image.setImageBitmap(ShortcutIcons.bitmap(activity, choice.icon(), dp(activity, 37)));
        }
    }
}

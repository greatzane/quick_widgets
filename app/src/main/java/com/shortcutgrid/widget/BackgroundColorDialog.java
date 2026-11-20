package com.shortcutgrid.widget;

import static com.shortcutgrid.widget.EditorUi.*;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.function.IntConsumer;

final class BackgroundColorDialog {
    private static final int[] HUE_COLORS = {
            Color.RED, Color.YELLOW, Color.GREEN, Color.CYAN, Color.BLUE, Color.MAGENTA, Color.RED
    };

    private BackgroundColorDialog() {}

    static void show(Activity activity, int initial, IntConsumer apply) {
        int startingColor = Color.alpha(initial) == 0 ? GridConfig.DEFAULT_BACKGROUND : initial;
        float[] hsv = new float[3];
        Color.colorToHSV(startingColor, hsv);
        int[] selected = {startingColor};

        ScrollView scroll = new ScrollView(activity);
        LinearLayout content = column(activity);
        padding(content, 23, 12);
        scroll.addView(content);

        LinearLayout preview = row(activity);
        preview.setBackground(shape(activity, PAGE, 14));
        BackgroundSwatch sample = new BackgroundSwatch(activity, startingColor, "颜色预览", false);
        sample.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        preview.addView(sample, new LinearLayout.LayoutParams(dp(activity, 52), dp(activity, 49)));
        preview.addView(text(activity, "颜色预览", 14, INK, true));
        content.addView(preview, new LinearLayout.LayoutParams(MATCH, WRAP));
        gap(content, 12);

        SeekBar hue = addSlider(content, "色调", 359, Math.round(hsv[0]) % 360);
        GradientDrawable rainbow = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, HUE_COLORS);
        rainbow.setCornerRadius(dp(activity, 3));
        rainbow.setSize(dp(activity, 200), dp(activity, 6));
        hue.setProgressDrawable(rainbow);
        hue.setProgressTintList(null);
        hue.setProgressBackgroundTintList(null);
        if (Build.VERSION.SDK_INT >= 29) {
            hue.setMinHeight(dp(activity, 6));
            hue.setMaxHeight(dp(activity, 6));
        }
        hue.setThumbTintList(ColorStateList.valueOf(hueColor(hsv[0])));

        SeekBar saturation = addSlider(content, "饱和度", 100, Math.round(hsv[1] * 100));
        SeekBar brightness = addSlider(content, "亮度", 100, Math.round(hsv[2] * 100));
        SeekBar opacity = addSlider(content, "不透明度", 255, Color.alpha(startingColor));

        Runnable update = () -> {
            hsv[0] = hue.getProgress();
            hsv[1] = saturation.getProgress() / 100f;
            hsv[2] = brightness.getProgress() / 100f;
            selected[0] = Color.HSVToColor(opacity.getProgress(), hsv);
            sample.setColor(selected[0]);
            hue.setThumbTintList(ColorStateList.valueOf(hueColor(hsv[0])));
        };
        onSeek(hue, progress -> update.run());
        onSeek(saturation, progress -> update.run());
        onSeek(brightness, progress -> update.run());
        onSeek(opacity, progress -> update.run());
        new AlertDialog.Builder(activity)
                .setTitle("自定义背景色")
                .setView(scroll)
                .setNegativeButton("取消", null)
                .setPositiveButton("应用", (dialog, which) -> apply.accept(
                        Color.alpha(selected[0]) == 0 ? Color.TRANSPARENT : selected[0]))
                .show();
    }

    private static int hueColor(float hue) {
        return Color.HSVToColor(new float[] {hue, .8f, .66f});
    }

    private static SeekBar addSlider(LinearLayout content, String title, int max, int progress) {
        Context context = content.getContext();
        TextView label = text(context, title, 13, MUTED, true);
        content.addView(label, new LinearLayout.LayoutParams(MATCH, WRAP));
        SeekBar slider = new SeekBar(context);
        slider.setMax(max);
        slider.setProgress(progress);
        slider.setContentDescription(title);
        content.addView(slider, new LinearLayout.LayoutParams(MATCH, dp(context, 43)));
        return slider;
    }
}

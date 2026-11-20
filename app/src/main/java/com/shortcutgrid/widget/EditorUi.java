package com.shortcutgrid.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.function.IntConsumer;

final class EditorUi {
    static final int PAGE = 0xFFF5F6F2;
    static final int INK = 0xFF18332C;
    static final int MUTED = 0xFF52665D;
    static final int ACCENT = 0xFF006553;
    static final int SELECTED = 0xFFD9EBE1;
    static final int OUTLINE = 0xFFDCE3DB;
    static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT;
    static final int WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;

    private EditorUi() {}

    static int dp(Context context, float value) {
        return Math.round(context.getResources().getDisplayMetrics().density * value);
    }

    static void padding(View view, int horizontal, int vertical) {
        Context context = view.getContext();
        view.setPadding(dp(context, horizontal), dp(context, vertical),
                dp(context, horizontal), dp(context, vertical));
    }

    static LinearLayout column(Context context) {
        LinearLayout view = new LinearLayout(context);
        view.setOrientation(LinearLayout.VERTICAL);
        return view;
    }

    static LinearLayout row(Context context) {
        LinearLayout view = new LinearLayout(context);
        view.setOrientation(LinearLayout.HORIZONTAL);
        view.setGravity(Gravity.CENTER_VERTICAL);
        return view;
    }

    static void gap(LinearLayout parent, int height) {
        parent.addView(new View(parent.getContext()), new LinearLayout.LayoutParams(
                1, dp(parent.getContext(), height)));
    }

    static void onSeek(SeekBar slider, IntConsumer change) {
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onStartTrackingTouch(SeekBar bar) {}
            @Override public void onStopTrackingTouch(SeekBar bar) {}
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                if (fromUser) change.accept(progress);
            }
        });
    }

    static TextView text(Context context, String value, int sp, int color, boolean emphasis) {
        TextView view = new TextView(context);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(color);
        if (emphasis) view.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        return view;
    }

    static Button button(Context context, String label, int textColor, int fill, int radius) {
        Button view = new Button(context, null, android.R.attr.borderlessButtonStyle);
        view.setAllCaps(false);
        view.setMinWidth(0);
        view.setMinimumWidth(0);
        view.setMinHeight(0);
        view.setMinimumHeight(0);
        view.setStateListAnimator(null);
        view.setText(label);
        view.setTextSize(14);
        view.setTextColor(textColor);
        view.setLetterSpacing(0);
        view.setGravity(Gravity.CENTER);
        view.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        view.setBackground(ripple(context, fill, radius));
        padding(view, 6, 0);
        return view;
    }

    static GradientDrawable shape(Context context, int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(context, radius));
        return drawable;
    }

    static RippleDrawable ripple(Context context, int color, int radius) {
        return new RippleDrawable(ColorStateList.valueOf(0x25006653),
                shape(context, color, radius), shape(context, Color.WHITE, radius));
    }
}

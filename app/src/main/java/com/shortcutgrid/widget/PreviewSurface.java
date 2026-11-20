package com.shortcutgrid.widget;

import static com.shortcutgrid.widget.EditorUi.*;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.function.IntConsumer;

final class PreviewSurface extends FrameLayout {
    static int heightDp(int rows) {
        return GridConfig.panelHeightDp(rows, GridConfig.MAX_ICON_DP) + 24;
    }

    PreviewSurface(Context context, GridConfig config, int moving,
                   IntConsumer onSelect, IntConsumer onHold) {
        super(context);
        setBackground(shape(context, 0xFFB4D0C2, 24));
        setClipToOutline(true);
        addView(new Wallpaper(context), new LayoutParams(MATCH, MATCH));

        LinearLayout widget = column(context);
        widget.setBackground(shape(context, config.background, 22));
        int horizontal = context.getResources().getDimensionPixelSize(R.dimen.widget_inset_horizontal);
        int vertical = context.getResources().getDimensionPixelSize(R.dimen.widget_inset_vertical);
        widget.setPadding(horizontal, vertical, horizontal, vertical);
        int widgetHeight = dp(context, GridConfig.panelHeightDp(config.rows, config.iconSizeDp));
        LayoutParams params = new LayoutParams(MATCH, widgetHeight, Gravity.CENTER);
        params.setMargins(dp(context, 12), 0, dp(context, 12), 0);
        addView(widget, params);
        int iconSize = dp(context, config.iconSizeDp);
        int iconInset = context.getResources().getDimensionPixelSize(R.dimen.widget_icon_inset);
        int emptySize = dp(context, 30);
        if (config.rows == 2) addVerticalGap(widget);
        for (int rowIndex = 0; rowIndex < config.rows; rowIndex++) {
            LinearLayout line = row(context);
            widget.addView(line, new LinearLayout.LayoutParams(MATCH, 0, 1));
            for (int column = 0; column < config.columns; column++) {
                int index = rowIndex * config.columns + column;
                ShortcutEntry entry = config.slots[index];
                FrameLayout tile = new FrameLayout(context);
                line.addView(tile, new LinearLayout.LayoutParams(0, MATCH, 1));
                if (entry != null) {
                    ImageButton icon = new ImageButton(context);
                    icon.setPadding(iconInset, iconInset, iconInset, iconInset);
                    icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                    icon.setImageBitmap(ShortcutIcons.bitmap(context, entry, iconSize));
                    icon.setBackground(ripple(context, index == moving ? 0x55006653 : Color.TRANSPARENT, 12));
                    icon.setContentDescription(entry.title());
                    icon.setOnClickListener(view -> onSelect.accept(index));
                    icon.setOnLongClickListener(view -> {
                        onHold.accept(index);
                        return true;
                    });
                    tile.addView(icon, new LayoutParams(MATCH, MATCH));
                } else {
                    TextView plus = text(context, "＋", 21, 0xFF355A4E, false);
                    plus.setGravity(Gravity.CENTER);
                    GradientDrawable outline = shape(context, 0x55FFFFFF, 11);
                    outline.setStroke(dp(context, 1), 0x88567C6C, dp(context, 3), dp(context, 2));
                    plus.setBackground(outline);
                    plus.setContentDescription("添加快捷方式，位置 " + (index + 1));
                    tile.addView(plus, new LayoutParams(emptySize, emptySize, Gravity.CENTER));
                    tile.setContentDescription(plus.getContentDescription());
                    plus.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
                    tile.setOnClickListener(view -> onSelect.accept(index));
                }
            }
            if (config.rows == 2) addVerticalGap(widget);
        }
    }

    private static void addVerticalGap(LinearLayout widget) {
        widget.addView(new View(widget.getContext()),
                new LinearLayout.LayoutParams(MATCH, 0, GridConfig.VERTICAL_GAP_WEIGHT));
    }

    private static final class Wallpaper extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();
        private Shader gradient;

        Wallpaper(Context context) {
            super(context);
            setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        }

        @Override
        protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
            if (width > 0 && height > 0) {
                gradient = new LinearGradient(0, 0, width, height,
                        0xFFD7E6D5, 0xFF93B9A9, Shader.TileMode.CLAMP);
            }
        }

        @Override
        protected void onDraw(Canvas canvas) {
            float width = getWidth();
            float height = getHeight();
            paint.setShader(gradient);
            canvas.drawRect(0, 0, width, height, paint);
            paint.setShader(null);
            paint.setColor(0x54F4E8B9);
            canvas.drawOval(-width * .19f, -height * .88f, width * .75f, height * .48f, paint);
            paint.setColor(0x47639180);
            path.reset();
            path.moveTo(0, height * .87f);
            path.cubicTo(width * .29f, height * .19f, width * .73f, height * 1.22f, width, height * .29f);
            path.lineTo(width, height);
            path.lineTo(0, height);
            path.close();
            canvas.drawPath(path, paint);
        }
    }
}

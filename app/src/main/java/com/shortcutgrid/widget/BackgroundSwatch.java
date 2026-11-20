package com.shortcutgrid.widget;

import static com.shortcutgrid.widget.EditorUi.*;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.widget.Button;

final class BackgroundSwatch extends Button {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path clip = new Path();
    private int color;

    BackgroundSwatch(Context context, int color, String label, boolean selected) {
        super(context, null, android.R.attr.borderlessButtonStyle);
        this.color = color;
        setContentDescription(label);
        setSelected(selected);
        setMinWidth(0);
        setMinimumWidth(0);
        setMinHeight(0);
        setMinimumHeight(0);
        setBackground(ripple(context, Color.TRANSPARENT, 12));
    }

    void setColor(int color) {
        this.color = color;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float x = getWidth() / 2f;
        float y = getHeight() / 2f;
        float radius = Math.min(dp(getContext(), 15), x - dp(getContext(), 4));
        if (radius < 1) return;
        paint.setColor(isSelected() ? ACCENT : OUTLINE);
        paint.setStrokeWidth(dp(getContext(), isSelected() ? 2 : 1));
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawCircle(x, y, radius + dp(getContext(), 2), paint);
        paint.setStyle(Paint.Style.FILL);

        canvas.save();
        clip.reset();
        clip.addCircle(x, y, radius, Path.Direction.CW);
        canvas.clipPath(clip);
        paint.setColor(Color.WHITE);
        canvas.drawCircle(x, y, radius, paint);
        if (Color.alpha(color) < 255) {
            float size = radius / 2f;
            paint.setColor(0xFFD1DBD5);
            for (int cell = 0; cell < 16; cell++) {
                if (((cell / 4) + cell) % 2 != 0) continue;
                float left = x - radius + cell % 4 * size;
                float top = y - radius + cell / 4 * size;
                canvas.drawRect(left, top, left + size, top + size, paint);
            }
        }
        paint.setColor(color);
        canvas.drawCircle(x, y, radius, paint);
        canvas.restore();
    }
}

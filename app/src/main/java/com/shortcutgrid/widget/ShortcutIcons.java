package com.shortcutgrid.widget;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.Drawable;

final class ShortcutIcons {
    private record Tone(int tile, int ink, int border, int logo) {}

    private static final Tone WECHAT = new Tone(0xFFE3F4E9, 0xFF087044, 0xFFB9DDC8, R.drawable.logo_wechat);
    private static final Tone ALIPAY = new Tone(0xFFE5F0FF, 0xFF145AA8, 0xFFC1D8F4, R.drawable.logo_alipay);
    private static final Tone FEISHU = new Tone(0xFFEDE9FB, 0xFF5142A5, 0xFFD5CDEF, R.drawable.logo_feishu);
    private static final Tone UNIONPAY = new Tone(0xFFFBE8ED, 0xFFA32A49, 0xFFF1C9D5, R.drawable.logo_unionpay);
    private static final Tone MEITUAN = new Tone(0xFFFFF1C2, 0xFF775307, 0xFFEBD283, R.drawable.logo_meituan);
    private static final Tone DIDI = new Tone(0xFFFCE9DC, 0xFFA74813, 0xFFF3CAAF, R.drawable.logo_didi);
    private static final Tone GENERIC = new Tone(0xFFE8EFEB, 0xFF345D4C, 0xFFCCDBD2, 0);

    private ShortcutIcons() {}

    static Bitmap bitmap(Context context, ShortcutEntry entry, int sizePx) {
        Bitmap bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        bitmap.setDensity(context.getResources().getDisplayMetrics().densityDpi);
        Canvas canvas = new Canvas(bitmap);
        ShortcutEntry.Glyph glyph = entry.glyph();
        if (glyph == ShortcutEntry.Glyph.APP) {
            Drawable icon = appIcon(context, entry.packageName(), entry.iconResource());
            if (icon != null) {
                icon.setBounds(0, 0, sizePx, sizePx);
                icon.draw(canvas);
                return bitmap;
            }
        }
        float scale = sizePx / 48f;
        canvas.save();
        canvas.scale(scale, scale);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Tone tone = switch (entry.packageName()) {
            case "com.tencent.mm" -> WECHAT;
            case "com.eg.android.AlipayGphone" -> ALIPAY;
            case "com.ss.android.lark" -> FEISHU;
            case "com.unionpay" -> UNIONPAY;
            case "com.sankuai.meituan" -> MEITUAN;
            case "com.sdu.didi.psnger", "com.qingqikeji.blackhorse.passenger" -> DIDI;
            default -> GENERIC;
        };
        paint.setColor(tone.tile());
        canvas.drawRoundRect(.5f, .5f, 47.5f, 47.5f, 13, 13, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(tone.border());
        paint.setStrokeWidth(1);
        canvas.drawRoundRect(.5f, .5f, 47.5f, 47.5f, 13, 13, paint);
        paint.setColor(tone.ink());
        paint.setStrokeWidth(2.4f);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        boolean hasLogo = tone.logo() != 0
                && (glyph == ShortcutEntry.Glyph.SCAN || glyph == ShortcutEntry.Glyph.PAY);
        switch (glyph) {
            case SCAN -> scan(canvas, paint, hasLogo);
            case PAY -> pay(canvas, paint, hasLogo);
            case TRANSIT -> transit(canvas, paint);
            case APP, LINK -> link(canvas, paint);
        }
        canvas.restore();
        if (hasLogo) {
            Drawable logo = context.getDrawable(tone.logo()).mutate();
            logo.setTint(tone.ink());
            int start = Math.round(27 * scale);
            int end = Math.round(41 * scale);
            logo.setBounds(start, start, end, end);
            logo.draw(canvas);
        }
        return bitmap;
    }

    static Drawable appIcon(Context context, String packageName, String resource) {
        PackageManager packages = context.getPackageManager();
        if (!resource.isEmpty()) {
            try {
                Resources resources = packages.getResourcesForApplication(packageName);
                int id = resources.getIdentifier(resource, null, packageName);
                if (id != 0) return resources.getDrawable(id, null);
            } catch (PackageManager.NameNotFoundException | Resources.NotFoundException ignored) {
                // The app icon still works if a published shortcut changes its artwork.
            }
        }
        try {
            return packages.getApplicationIcon(packageName);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    private static void scan(Canvas canvas, Paint paint, boolean hasLogo) {
        Path path = new Path();
        path.moveTo(11, 19);
        path.lineTo(11, 15);
        path.quadTo(11, 11, 15, 11);
        path.lineTo(19, 11);
        path.moveTo(29, 11);
        path.lineTo(33, 11);
        path.quadTo(37, 11, 37, 15);
        path.lineTo(37, 19);
        if (!hasLogo) {
            path.moveTo(37, 29);
            path.lineTo(37, 33);
            path.quadTo(37, 37, 33, 37);
            path.lineTo(29, 37);
        }
        path.moveTo(19, 37);
        path.lineTo(15, 37);
        path.quadTo(11, 37, 11, 33);
        path.lineTo(11, 29);
        canvas.drawPath(path, paint);
        canvas.drawLine(14, 24, 34, 24, paint);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(19, 17, 23, 21, 1, 1, paint);
        float lowerDotX = hasLogo ? 21.5f : 25;
        canvas.drawRoundRect(lowerDotX, 27, lowerDotX + 4, 31, 1, 1, paint);
    }

    private static void pay(Canvas canvas, Paint paint, boolean hasLogo) {
        drawQrFinder(canvas, paint, 11, 11);
        drawQrFinder(canvas, paint, 27, 11);
        drawQrFinder(canvas, paint, 11, 27);

        if (hasLogo) {
            canvas.drawRoundRect(23, 23, 26.8f, 26.8f, .6f, .6f, paint);
            canvas.drawRoundRect(29.4f, 23.5f, 33.4f, 25.5f, .6f, .6f, paint);
            canvas.drawRoundRect(23.6f, 30, 26, 35.2f, .6f, .6f, paint);
            return;
        }

        Path modules = new Path();
        modules.moveTo(27, 27);
        modules.lineTo(31, 27);
        modules.lineTo(31, 31);
        modules.lineTo(37, 31);
        modules.lineTo(37, 38);
        modules.lineTo(33, 38);
        modules.lineTo(33, 35);
        modules.lineTo(30, 35);
        modules.lineTo(30, 32);
        modules.lineTo(27, 32);
        modules.close();
        canvas.drawPath(modules, paint);
        canvas.drawRoundRect(34, 27, 38, 29.5f, .6f, .6f, paint);
        canvas.drawRoundRect(27, 35.5f, 30.5f, 38.5f, .6f, .6f, paint);
    }

    private static void drawQrFinder(Canvas canvas, Paint paint, float x, float y) {
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawRoundRect(x, y, x + 10, y + 10, 2, 2, paint);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(x + 3, y + 3, x + 7, y + 7, .65f, .65f, paint);
    }

    private static void transit(Canvas canvas, Paint paint) {
        canvas.drawRoundRect(12, 11, 36, 34, 5, 5, paint);
        canvas.drawLine(12, 23, 36, 23, paint);
        canvas.drawLine(24, 12, 24, 23, paint);
        canvas.drawLine(16, 33, 16, 37, paint);
        canvas.drawLine(32, 33, 32, 37, paint);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(18, 28, 1.65f, paint);
        canvas.drawCircle(30, 28, 1.65f, paint);
    }

    private static void link(Canvas canvas, Paint paint) {
        Path frame = new Path();
        frame.moveTo(34, 26);
        frame.lineTo(34, 31);
        frame.quadTo(34, 36, 29, 36);
        frame.lineTo(17, 36);
        frame.quadTo(12, 36, 12, 31);
        frame.lineTo(12, 19);
        frame.quadTo(12, 14, 17, 14);
        frame.lineTo(22, 14);
        canvas.drawPath(frame, paint);
        canvas.drawLine(22, 26, 36, 12, paint);
        canvas.drawLine(36, 12, 29, 12, paint);
        canvas.drawLine(36, 12, 36, 19, paint);
    }
}

package com.leafiq.app.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;

/**
 * Converts emoji Unicode characters to BitmapDrawable for use in UI components
 * like BottomNavigationView that require drawable icons.
 */
public class EmojiDrawableFactory {

    /**
     * Render an emoji string as a BitmapDrawable at the specified dp size.
     *
     * @param context Context for display metrics and resources
     * @param emoji   The emoji character(s) to render (e.g., "🪴")
     * @param sizeDp  The icon size in dp (24dp is standard for BottomNavigationView)
     * @return BitmapDrawable with the emoji rendered and centered
     */
    public static BitmapDrawable createEmojiDrawable(Context context, String emoji, int sizeDp) {
        float density = context.getResources().getDisplayMetrics().density;
        int sizePx = (int) (sizeDp * density);

        Bitmap bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTextSize(sizePx * 0.8f);
        paint.setTextAlign(Paint.Align.CENTER);

        Paint.FontMetrics metrics = paint.getFontMetrics();
        float x = sizePx / 2f;
        float y = (sizePx - metrics.ascent - metrics.descent) / 2f;

        canvas.drawText(emoji, x, y, paint);

        return new BitmapDrawable(context.getResources(), bitmap);
    }
}

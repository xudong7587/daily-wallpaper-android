package cn.sunny.wallpaper;

import android.content.*;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.*;
import android.view.*;
import android.widget.*;

final class Ui {
    static final int INK = 0xff17372b, MUTED = 0xff66776f, GREEN = 0xff246b53;
    static int dp(Context c, int value) { return Math.round(value * c.getResources().getDisplayMetrics().density); }
    static TextView text(Context c, String value, float size, int color) {
        TextView v = new TextView(c); v.setText(value); v.setTextSize(size); v.setTextColor(color);
        v.setLineSpacing(0, 1.08f); return v;
    }
    static TextView title(Context c, String value, float size) {
        TextView v = text(c, value, size, INK); v.setTypeface(Typeface.create("sans-serif", Typeface.BOLD)); return v;
    }
    static LinearLayout card(Context c) {
        LinearLayout card = new LinearLayout(c); card.setOrientation(LinearLayout.VERTICAL);
        int p = dp(c, 18); card.setPadding(p, p, p, p); card.setBackground(shape(0xffffffff, 22, 0xffe2e9e5, 1, c));
        card.setElevation(dp(c, 2)); return card;
    }
    static void addCard(LinearLayout root, View card, Context c) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2); lp.setMargins(0, 0, 0, dp(c, 14)); root.addView(card, lp);
    }
    static Button button(Context c, String label, boolean primary) {
        Button b = new Button(c); b.setText(label); b.setTextSize(15); b.setAllCaps(false); b.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        b.setTextColor(primary ? Color.WHITE : INK); b.setGravity(Gravity.CENTER); b.setMinHeight(0); b.setMinimumHeight(0);
        b.setPadding(dp(c, 16), 0, dp(c, 16), 0); b.setStateListAnimator(null);
        int normal = primary ? GREEN : 0xffedf3ef, pressed = primary ? 0xff194c3b : 0xffdfe9e3;
        b.setBackground(ripple(c, normal, pressed, 15)); return b;
    }
    static void addButton(LinearLayout parent, Button button, Context c) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(c, 50)); lp.setMargins(0, dp(c, 10), 0, 0); parent.addView(button, lp);
    }
    static TextView sectionTitle(Context c, String title, String caption, LinearLayout parent) {
        TextView t = Ui.title(c, title, 18); parent.addView(t);
        if (caption != null && !caption.isEmpty()) {
            TextView note = text(c, caption, 13, MUTED); LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2); lp.setMargins(0, dp(c, 5), 0, dp(c, 12)); parent.addView(note, lp);
        }
        return t;
    }
    static Drawable shape(int fill, int radius, int stroke, int strokeWidth, Context c) {
        GradientDrawable g = new GradientDrawable(); g.setColor(fill); g.setCornerRadius(dp(c, radius));
        if (strokeWidth > 0) g.setStroke(dp(c, strokeWidth), stroke); return g;
    }
    static Drawable ripple(Context c, int normal, int pressed, int radius) {
        Drawable content = shape(normal, radius, 0, 0, c);
        return new RippleDrawable(ColorStateList.valueOf(pressed), content, shape(Color.WHITE, radius, 0, 0, c));
    }
}

package cn.sunny.wallpaper;

import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;
import java.io.File;

public class WallpaperWidget extends AppWidgetProvider {
    public static class OneByOne extends WallpaperWidget { }
    public static class OneByTwo extends WallpaperWidget { }
    public static class TwoByFour extends WallpaperWidget { }
    public static class TwoByFive extends WallpaperWidget { }
    private static final Class<?>[] TYPES = {OneByOne.class, OneByTwo.class, TwoByFour.class, TwoByFive.class};
    @Override public void onUpdate(Context c, AppWidgetManager manager, int[] ids) { updateAll(c); }
    @Override public void onAppWidgetOptionsChanged(Context c, AppWidgetManager m, int id, Bundle options) { updateAll(c); }
    static void updateAll(Context c) {
        AppWidgetManager manager = AppWidgetManager.getInstance(c);
        android.content.SharedPreferences p = Schedule.prefs(c);
        boolean loading = p.getLong("widget_busy_until", 0) > System.currentTimeMillis();
        String phase = p.getString("widget_phase", "idle");
        if (!loading && "loading".equals(phase)) phase = "error";
        boolean motion = Settings.Global.getFloat(c.getContentResolver(), Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f;
        Bitmap preview = null;
        File file = new File(c.getFilesDir(), "preview.jpg");
        if (file.exists()) preview = BitmapFactory.decodeFile(file.getPath());
        Intent action = new Intent(c, WidgetTapReceiver.class).setAction("cn.sunny.wallpaper.WIDGET_CHANGE");
        PendingIntent tap = PendingIntent.getBroadcast(c, 205, action, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        for (Class<?> type : TYPES) {
            boolean compact = type == OneByOne.class, bar = type == OneByTwo.class;
            for (int id : manager.getAppWidgetIds(new ComponentName(c, type))) {
                RemoteViews views = new RemoteViews(c.getPackageName(), compact ? R.layout.widget_small : bar ? R.layout.widget_bar : R.layout.widget_wide);
                for (int clickable : new int[]{R.id.widget_root, R.id.widget_button, R.id.widget_refresh, R.id.widget_spinner, R.id.widget_title}) views.setOnClickPendingIntent(clickable, tap);
                views.setViewVisibility(R.id.widget_spinner, loading && motion ? View.VISIBLE : View.GONE);
                views.setViewVisibility(R.id.widget_refresh, loading && motion ? View.INVISIBLE : View.VISIBLE);
                views.setImageViewResource(R.id.widget_refresh, !loading && "error".equals(phase) ? R.drawable.widget_error : R.drawable.widget_refresh_blue);
                String title = loading ? "更换中" : "error".equals(phase) ? "重试" : compact ? ("same".equals(phase) ? "图片未变" : "换壁纸") : bar ? "换一张" : "换个风景";
                views.setTextViewText(R.id.widget_title, title);
                String detail = loading ? "正在准备图片" : p.getString("widget_detail", "点击更换一次壁纸");
                views.setContentDescription(R.id.widget_root, detail);
                if (!compact) {
                    views.setOnClickPendingIntent(R.id.widget_status, tap);
                    views.setTextViewText(R.id.widget_status, loading ? "正在换一张…" : "error".equals(phase) ? "未完成，轻点重试" : p.getString("widget_result", bar ? "随手换个风景" : "轻点，换一张壁纸"));
                    if (!bar) { views.setOnClickPendingIntent(R.id.widget_image, tap); if (preview != null) views.setImageViewBitmap(R.id.widget_image, preview); }
                }
                if (compact && Build.VERSION.SDK_INT >= 31) {
                    int size = Math.max(44, Math.min(64, p.getInt("widget_icon_dp", 52)));
                    views.setViewLayoutWidth(R.id.widget_button, size, TypedValue.COMPLEX_UNIT_DIP);
                    views.setViewLayoutHeight(R.id.widget_button, size, TypedValue.COMPLEX_UNIT_DIP);
                }
                try { manager.updateAppWidget(id, views); } catch (RuntimeException e) { android.util.Log.w("WallpaperWidget", "Host rejected widget " + id, e); }
            }
        }
    }
}

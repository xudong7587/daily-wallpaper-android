package cn.sunny.wallpaper;

import android.app.*;
import android.app.job.*;
import android.content.*;
import android.os.*;
import java.util.UUID;

/** One explicit widget request, shared by foreground and fallback execution. */
public final class WidgetRun {
    static final int JOB = 203;
    static final long TIMEOUT = 180000;
    private static String claimed = "";
    static synchronized String begin(Context c) {
        SharedPreferences p = Schedule.prefs(c);
        if (p.getLong("widget_busy_until", 0) > System.currentTimeMillis()) return null;
        String token = UUID.randomUUID().toString();
        p.edit().putString("widget_token", token).putLong("widget_busy_until", System.currentTimeMillis() + TIMEOUT)
            .putString("widget_phase", "loading").putString("widget_result", "正在换一张").putString("widget_detail", "正在获取图片").commit();
        Intent timeout = new Intent(c, WidgetTapReceiver.class).setAction("cn.sunny.wallpaper.WIDGET_TIMEOUT").putExtra("token", token);
        PendingIntent pi = PendingIntent.getBroadcast(c, 204, timeout, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        c.getSystemService(AlarmManager.class).setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + TIMEOUT, pi);
        update(c); return token;
    }
    static boolean current(Context c, String token) {
        return token != null && token.equals(Schedule.prefs(c).getString("widget_token", ""))
            && Schedule.prefs(c).getLong("widget_busy_until", 0) > System.currentTimeMillis();
    }
    static synchronized boolean claim(Context c, String token) {
        if (!current(c, token) || token.equals(claimed)) return false;
        claimed = token; return true;
    }
    static synchronized void release(String token) { if (token != null && token.equals(claimed)) claimed = ""; }
    static void update(Context c) { try { WallpaperWidget.updateAll(c); } catch (RuntimeException e) { android.util.Log.w("WallpaperWidget", "Widget host update failed", e); } }
    static synchronized void finish(Context c, String token, String phase, String summary, String detail) {
        SharedPreferences p = Schedule.prefs(c);
        if (token == null || !token.equals(p.getString("widget_token", ""))) return;
        p.edit().putLong("widget_busy_until", 0).putString("widget_token", "").putString("widget_phase", phase)
            .putString("widget_result", summary).putString("widget_detail", detail).apply();
        if ("error".equals(phase)) p.edit().putString("status", detail).apply();
        release(token); update(c);
    }
    static void fallback(Context c, String token) {
        if (!current(c, token)) return;
        PersistableBundle extras = new PersistableBundle(); extras.putString("token", token);
        JobScheduler scheduler = c.getSystemService(JobScheduler.class);
        int result = JobScheduler.RESULT_FAILURE;
        if (Build.VERSION.SDK_INT >= 31) {
            try { result = scheduler.schedule(new JobInfo.Builder(JOB, new ComponentName(c, WidgetWorkJob.class)).setExtras(extras).setExpedited(true).build()); }
            catch (RuntimeException ignored) { }
        }
        if (result != JobScheduler.RESULT_SUCCESS) {
            try { result = scheduler.schedule(new JobInfo.Builder(JOB, new ComponentName(c, WidgetWorkJob.class)).setExtras(extras).setOverrideDeadline(0).build()); }
            catch (RuntimeException ignored) { }
        }
        if (result != JobScheduler.RESULT_SUCCESS) finish(c, token, "error", "点此重试", "系统未允许执行，请在应用设置中允许后台运行");
    }
    static void perform(Context c, String token) {
        SharedPreferences p = Schedule.prefs(c);
        String previous = p.getString("last_image", "");
        try {
            WallpaperEngine.widgetGuard.set(() -> current(c, token));
            WallpaperEngine.apply(c, p.getString("url", WallpaperEngine.DEFAULT_URL), p.getInt("target", 1), p.getLong("revision", 0), false, p.getInt("mode", 0));
            if (!current(c, token)) return;
            boolean same = !previous.isEmpty() && previous.equals(p.getString("last_image", ""));
            finish(c, token, same ? "same" : "success", same ? "图片未变" : "已换好", p.getString("status", "壁纸已更换") + (same ? "；所选图片与上一张相同。" : ""));
        } catch (Exception e) {
            if (current(c, token)) finish(c, token, "error", "点此重试", "更换失败：" + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()));
        } finally { WallpaperEngine.widgetGuard.remove(); release(token); }
    }
}

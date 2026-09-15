package cn.sunny.wallpaper;

import android.app.job.JobScheduler;
import android.content.*;
import android.os.Build;

public class WidgetTapReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent i) {
        if ("cn.sunny.wallpaper.WIDGET_TIMEOUT".equals(i.getAction())) {
            String token = i.getStringExtra("token");
            if (token != null && token.equals(Schedule.prefs(c).getString("widget_token", ""))) {
                WidgetRun.finish(c, token, "error", "点此重试", "换图超时，请检查网络和后台运行权限");
                c.stopService(new Intent(c, WidgetChangeService.class));
                c.getSystemService(JobScheduler.class).cancel(WidgetRun.JOB);
            }
            return;
        }
        if (!"cn.sunny.wallpaper.WIDGET_CHANGE".equals(i.getAction())) return;
        String token = WidgetRun.begin(c); if (token == null) return;
        Intent work = new Intent(c, WidgetChangeService.class).putExtra("token", token);
        try { if (Build.VERSION.SDK_INT >= 26) c.startForegroundService(work); else c.startService(work); }
        catch (RuntimeException e) { WidgetRun.fallback(c, token); }
    }
}

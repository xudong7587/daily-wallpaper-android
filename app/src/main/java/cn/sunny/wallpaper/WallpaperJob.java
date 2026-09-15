package cn.sunny.wallpaper;

import android.app.job.*;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import java.util.concurrent.*;

public class WallpaperJob extends JobService {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private Future<?> work;
    private volatile Object activeRun;
    @Override public boolean onStartJob(JobParameters params) {
        SharedPreferences p = Schedule.prefs(this);
        if (!p.getBoolean("enabled", false)) return false;
        if (!Schedule.activeNow(this)) {
            p.edit().putString("status", "已跳过允许时段外的自动更换").apply();
            return false;
        }
        final Object run = new Object();
        activeRun = run;
        String url = p.getString("url", WallpaperEngine.DEFAULT_URL);
        int target = p.getInt("target", 1);
        long revision = p.getLong("revision", 0);
        int mode = p.getInt("mode", 0);
        work = executor.submit(() -> {
            boolean retry = false;
            try {
                WallpaperEngine.apply(this, url, target, revision, true, mode);
                p.edit().putInt("failures", 0).apply();
            } catch (Exception e) {
                if (activeRun == run && p.getLong("revision", 0) == revision) {
                    int failures = p.getInt("failures", 0) + 1;
                    retry = failures < 4 && p.getBoolean("enabled", false);
                    p.edit().putInt("failures", failures).putString("status", "更换失败：" + e.getMessage() + (retry ? "；稍后自动重试" : "；请检查链接或网络")).apply();
                }
            }
            final boolean again = retry;
            new Handler(Looper.getMainLooper()).post(() -> { if (activeRun == run) jobFinished(params, again); });
        });
        return true;
    }
    @Override public boolean onStopJob(JobParameters params) {
        activeRun = null;
        if (work != null) work.cancel(true);
        return Schedule.prefs(this).getBoolean("enabled", false);
    }
    @Override public void onDestroy() { executor.shutdownNow(); super.onDestroy(); }
}

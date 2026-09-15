package cn.sunny.wallpaper;
import android.content.*;
public class RestoreReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent i) {
        if (Intent.ACTION_MY_PACKAGE_REPLACED.equals(i.getAction())) {
            android.app.job.JobScheduler jobs = c.getSystemService(android.app.job.JobScheduler.class);
            if (jobs.getPendingJob(Schedule.JOB_ID) != null) {
                jobs.cancel(Schedule.JOB_ID);
                if (Schedule.prefs(c).getBoolean("enabled", false)) Schedule.enqueue(c);
            }
        }
        Schedule.refresh(c, Intent.ACTION_BOOT_COMPLETED.equals(i.getAction()), false);
        if (Intent.ACTION_BOOT_COMPLETED.equals(i.getAction()) || Intent.ACTION_MY_PACKAGE_REPLACED.equals(i.getAction()))
            Schedule.prefs(c).edit().putLong("widget_busy_until", 0).putString("widget_token", "").putString("widget_phase", "idle").apply();
        try { WallpaperWidget.updateAll(c); } catch (RuntimeException ignored) { }
    }
}

package cn.sunny.wallpaper;

import android.app.*;
import android.app.job.*;
import android.content.*;
import android.os.Build;
import android.os.SystemClock;

public final class Schedule {
    static final int JOB_ID = 100;
    static SharedPreferences prefs(Context c) { return c.getSharedPreferences("wallpaper", 0); }
    static boolean exact(Context c) {
        return Build.VERSION.SDK_INT < 31 || c.getSystemService(AlarmManager.class).canScheduleExactAlarms();
    }
    static int startMinute(SharedPreferences p) { return p.getInt("active_start", 8 * 60); }
    static int endMinute(SharedPreferences p) { return p.getInt("active_end", 22 * 60); }
    static boolean activeNow(Context c) {
        SharedPreferences p = prefs(c);
        return ActiveHours.contains(System.currentTimeMillis(), startMinute(p), endMinute(p));
    }
    static void refresh(Context c) {
        refresh(c, false, false);
    }
    static void refresh(Context c, boolean restart, boolean consumed) {
        SharedPreferences p = prefs(c);
        AlarmManager am = c.getSystemService(AlarmManager.class);
        PendingIntent pi = PendingIntent.getBroadcast(c, 1, new Intent(c, AlarmReceiver.class), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        am.cancel(pi);
        if (!p.getBoolean("enabled", false)) {
            c.getSystemService(JobScheduler.class).cancel(JOB_ID);
            p.edit().remove("next").remove("next_elapsed").apply();
            return;
        }
        long at = DailyTime.next(System.currentTimeMillis(), p.getInt("hour", 8), p.getInt("minute", 0));
        int type = AlarmManager.RTC_WAKEUP;
        if (p.getBoolean("interval_mode", false)) {
            type = AlarmManager.ELAPSED_REALTIME_WAKEUP;
            at = IntervalTime.next(SystemClock.elapsedRealtime(), restart ? 0 : p.getLong("next_elapsed", 0), IntervalTime.milliseconds(p.getLong("interval_minutes", 60)), consumed);
            p.edit().putLong("next_elapsed", at).apply();
        }
        long display = type == AlarmManager.ELAPSED_REALTIME_WAKEUP ? System.currentTimeMillis() + Math.max(0, at - SystemClock.elapsedRealtime()) : at;
        long allowedDisplay = ActiveHours.clamp(display, startMinute(p), endMinute(p));
        if (type == AlarmManager.ELAPSED_REALTIME_WAKEUP) {
            at += allowedDisplay - display;
            p.edit().putLong("next_elapsed", at).apply();
        } else at = allowedDisplay;
        try {
            if (exact(c)) am.setExactAndAllowWhileIdle(type, at, pi);
            else am.setAndAllowWhileIdle(type, at, pi);
        } catch (SecurityException e) { am.setAndAllowWhileIdle(type, at, pi); }
        display = allowedDisplay;
        p.edit().putLong("next", display).apply();
    }
    static void enqueue(Context c) {
        // A short interval must not cancel an ongoing download or its retry.
        if (c.getSystemService(JobScheduler.class).getPendingJob(JOB_ID) != null) return;
        prefs(c).edit().putInt("failures", 0).apply();
        JobInfo info = new JobInfo.Builder(JOB_ID, new ComponentName(c, WallpaperJob.class))
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_NONE)
            .setBackoffCriteria(60_000, JobInfo.BACKOFF_POLICY_EXPONENTIAL)
            .setOverrideDeadline(0).build();
        if (c.getSystemService(JobScheduler.class).schedule(info) != JobScheduler.RESULT_SUCCESS)
            prefs(c).edit().putString("status", "系统暂未接受后台任务，请打开应用重试").apply();
    }
}

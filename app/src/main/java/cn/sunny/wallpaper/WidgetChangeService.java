package cn.sunny.wallpaper;

import android.app.*;
import android.content.*;
import android.os.*;
import java.util.concurrent.*;

public class WidgetChangeService extends Service {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private PowerManager.WakeLock wakeLock;
    private String token;
    private boolean fallback;
    @Override public IBinder onBind(Intent i) { return null; }
    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (token != null && WidgetRun.current(this, token)) return START_NOT_STICKY;
        handler.removeCallbacksAndMessages(null);
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
        String request = intent == null ? null : intent.getStringExtra("token");
        if (request == null) request = WidgetRun.begin(this); // old pending intents after an upgrade
        if (request == null || !WidgetRun.current(this, request)) { stopSelf(); return START_NOT_STICKY; }
        token = request;
        fallback = false;
        try {
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (Build.VERSION.SDK_INT >= 26) nm.createNotificationChannel(new NotificationChannel("widget_change", "桌面组件更换壁纸", NotificationManager.IMPORTANCE_LOW));
            Notification.Builder n = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(this, "widget_change") : new Notification.Builder(this);
            n.setSmallIcon(R.drawable.ic_refresh).setContentTitle("正在换一张壁纸").setOngoing(true).setContentIntent(PendingIntent.getActivity(this, 201, new Intent(this, MainActivity.class), PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT));
            startForeground(200, n.build());
            wakeLock = getSystemService(PowerManager.class).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "DailyWallpaper:Widget"); wakeLock.acquire(WidgetRun.TIMEOUT);
        } catch (RuntimeException e) {
            fallback = true; WidgetRun.fallback(this, token); stopSelf(); return START_NOT_STICKY;
        }
        if (!WidgetRun.claim(this, token)) { fallback = true; stopForeground(true); stopSelf(); return START_NOT_STICKY; }
        final String active = token;
        handler.postDelayed(() -> {
            if (!active.equals(token)) return;
            executor.shutdownNow(); WidgetRun.finish(this, active, "error", "点此重试", "换图超时，请检查网络和后台运行权限"); stopForeground(true); stopSelf();
        }, WidgetRun.TIMEOUT);
        executor.submit(() -> {
            WidgetRun.perform(getApplicationContext(), active);
            handler.post(() -> { if (active.equals(token)) { handler.removeCallbacksAndMessages(null); stopForeground(true); stopSelfResult(startId); } });
        });
        return START_NOT_STICKY;
    }
    @Override public void onTimeout(int id, int type) {
        executor.shutdownNow(); WidgetRun.finish(this, token, "error", "点此重试", "系统暂停了换图，请稍后重试"); stopForeground(true); stopSelf();
    }
    @Override public void onDestroy() {
        handler.removeCallbacksAndMessages(null); executor.shutdownNow();
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
        if (!fallback && WidgetRun.current(this, token)) WidgetRun.finish(this, token, "error", "点此重试", "换图被系统中断，请允许后台运行后重试");
        if (!fallback) WidgetRun.release(token);
        super.onDestroy();
    }
}

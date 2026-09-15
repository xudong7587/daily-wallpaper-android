package cn.sunny.wallpaper;

import android.app.job.*;
import android.os.*;
import java.util.concurrent.*;

/** The job path is only used if the foreground launch was rejected. */
public class WidgetWorkJob extends JobService {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Future<?> work;
    private String token;
    @Override public boolean onStartJob(JobParameters params) {
        String request = params.getExtras().getString("token");
        if (!WidgetRun.claim(this, request)) return false;
        token = request;
        handler.postDelayed(() -> {
            if (request.equals(token)) {
                if (work != null) work.cancel(true);
                WidgetRun.finish(this, request, "error", "点此重试", "换图超时，请检查网络"); token = null; jobFinished(params, false);
            }
        }, WidgetRun.TIMEOUT);
        work = executor.submit(() -> {
            WidgetRun.perform(this, request);
            handler.post(() -> { if (request.equals(token)) { token = null; handler.removeCallbacksAndMessages(null); jobFinished(params, false); } });
        });
        return true;
    }
    @Override public boolean onStopJob(JobParameters params) {
        String request = token; token = null;
        handler.removeCallbacksAndMessages(null); if (work != null) work.cancel(true);
        if (request != null) WidgetRun.finish(this, request, "error", "点此重试", "系统暂停了换图，请允许后台运行后重试");
        return false;
    }
    @Override public void onDestroy() { handler.removeCallbacksAndMessages(null); executor.shutdownNow(); super.onDestroy(); }
}

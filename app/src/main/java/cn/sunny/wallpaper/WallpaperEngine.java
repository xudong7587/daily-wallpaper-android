package cn.sunny.wallpaper;

import android.app.WallpaperManager;
import android.content.Context;
import android.graphics.Bitmap;
import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;

public final class WallpaperEngine {
    public static final String DEFAULT_URL = "https://dailybing.com/api/v1/today/zh-cn/UHD";
    static final Object LOCK = new Object();
    static final ThreadLocal<java.util.function.BooleanSupplier> widgetGuard = new ThreadLocal<>();
    static boolean valid(String value) {
        try { URL u = new URL(value); return (u.getProtocol().equals("https") || u.getProtocol().equals("http")) && !u.getHost().isEmpty(); }
        catch (Exception e) { return false; }
    }
    static void check() throws InterruptedIOException {
        if (Thread.currentThread().isInterrupted() || (widgetGuard.get() != null && !widgetGuard.get().getAsBoolean())) throw new InterruptedIOException("任务已取消");
    }
    static File refresh(Context context, String address) throws Exception {
        File temp = File.createTempFile("wallpaper-", ".img", context.getCacheDir());
        try {
            URL url = new URL(address); boolean downloaded = false;
            long deadline = System.nanoTime() + 10_000_000_000L;
            for (int redirects = 0; redirects < 8; redirects++) {
                check(); if (!valid(url.toString())) throw new IOException("只支持 HTTP 或 HTTPS 图片链接");
                if (System.nanoTime() >= deadline) throw new IOException("链接响应超时");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(4000); conn.setReadTimeout(4000); conn.setInstanceFollowRedirects(false); conn.setUseCaches(false);
                conn.setRequestProperty("Cache-Control", "no-cache"); conn.setRequestProperty("User-Agent", "DailyWallpaper/1.1 Android");
                try {
                    int code = conn.getResponseCode();
                    if (code == 301 || code == 302 || code == 303 || code == 307 || code == 308) {
                        String location = conn.getHeaderField("Location"); if (location == null) throw new IOException("重定向缺少目标链接");
                        url = new URL(url, location); continue;
                    }
                    if (code != 200) throw new IOException("下载失败：HTTP " + code);
                    if (conn.getContentLengthLong() > 32L * 1024 * 1024) throw new IOException("图片超过 32 MB");
                    try (InputStream in = conn.getInputStream(); OutputStream out = new FileOutputStream(temp)) {
                        byte[] buffer = new byte[16384]; long total = 0; int count;
                        while ((count = in.read(buffer)) != -1) {
                            check(); total += count;
                            if (total > 32L * 1024 * 1024) throw new IOException("图片超过 32 MB");
                            if (System.nanoTime() >= deadline) throw new IOException("下载超时，使用本地图库");
                            out.write(buffer, 0, count);
                        }
                    }
                    downloaded = true; break;
                } finally { conn.disconnect(); }
            }
            if (!downloaded) throw new IOException("链接重定向次数过多");
            check(); return Library.add(context, temp, "每日壁纸 " + new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(new Date()), "链接下载");
        } finally { temp.delete(); }
    }
    static void apply(Context context, String address, int target, long revision, boolean automatic, int mode) throws Exception {
        synchronized (LOCK) {
            Diagnostics.record(context, "start mode=" + mode + " target=" + target + " automatic=" + automatic);
            WallpaperFlow.run(mode, Schedule.prefs(context).getString("last_image", ""), new Random(), new WallpaperFlow.Source() {
                public void check() throws Exception { WallpaperEngine.check(); }
                public String refresh() throws Exception { return WallpaperEngine.refresh(context, address).getName(); }
                public List<String> images() {
                    List<String> ids = new ArrayList<>(); for (File f : Library.list(context)) ids.add(f.getName()); return ids;
                }
                public void apply(String id, boolean networkFailed) throws Exception {
                    applyFile(context, new File(Library.folder(context), id), target, revision, automatic, networkFailed ? "（链接不可用，已使用本地图库）" : mode == 0 ? "（当前链接图片）" : "（图库随机）");
                }
            });
        }
    }
    static void applyFile(Context context, File file, int target, long revision, boolean automatic, String note) throws Exception {
        synchronized (LOCK) {
            check(); Bitmap bitmap = Library.decode(file, 8_500_000L);
            try {
                check();
                synchronized (Schedule.class) {
                    if (Schedule.prefs(context).getLong("revision", 0) != revision || (automatic && !Schedule.prefs(context).getBoolean("enabled", false))) throw new InterruptedIOException("设置已变更，已取消旧任务");
                    WallpaperManager wm = WallpaperManager.getInstance(context);
                    if (!wm.isWallpaperSupported() || !wm.isSetWallpaperAllowed()) throw new IOException("此设备或系统策略不允许更换壁纸");
                    int beforeHome = wm.getWallpaperId(WallpaperManager.FLAG_SYSTEM), beforeLock = wm.getWallpaperId(WallpaperManager.FLAG_LOCK);
                    Diagnostics.record(context, "setBitmap image=" + file.getName() + " size=" + bitmap.getWidth() + "x" + bitmap.getHeight() + " target=" + target + " beforeHome=" + beforeHome + " beforeLock=" + beforeLock);
                    int result = wm.setBitmap(bitmap, null, false, target);
                    Diagnostics.record(context, "setBitmap result=" + result + " afterHome=" + wm.getWallpaperId(WallpaperManager.FLAG_SYSTEM) + " afterLock=" + wm.getWallpaperId(WallpaperManager.FLAG_LOCK));
                    if (result <= 0) throw new IOException("系统拒绝设置壁纸，请在本应用的系统权限中允许“设置壁纸”后重试");
                    int home = wm.getWallpaperId(WallpaperManager.FLAG_SYSTEM), lock = wm.getWallpaperId(WallpaperManager.FLAG_LOCK);
                    for (int attempt = 0; !WallpaperResult.confirmed(result, target, home, lock) && attempt < 8; attempt++) {
                        Thread.sleep(200); check();
                        home = wm.getWallpaperId(WallpaperManager.FLAG_SYSTEM); lock = wm.getWallpaperId(WallpaperManager.FLAG_LOCK);
                    }
                    if (!WallpaperResult.confirmed(result, target, home, lock)) throw new IOException("系统未确认壁纸更新，请检查系统壁纸设置后重试");
                }
                try (OutputStream out = new FileOutputStream(new File(context.getFilesDir(), "preview.jpg"))) {
                    Bitmap thumb = Bitmap.createScaledBitmap(bitmap, Math.max(1, bitmap.getWidth() * 480 / Math.max(bitmap.getWidth(), bitmap.getHeight())), Math.max(1, bitmap.getHeight() * 480 / Math.max(bitmap.getWidth(), bitmap.getHeight())), true);
                    thumb.compress(Bitmap.CompressFormat.JPEG, 85, out); if (thumb != bitmap) thumb.recycle();
                } catch (IOException ignored) { }
                Schedule.prefs(context).edit().putString("last_image", file.getName()).putString("status", "更换成功 · " + DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(new Date()) + note).apply();
                try { WallpaperWidget.updateAll(context); } catch (RuntimeException ignored) { }
            } finally { bitmap.recycle(); }
        }
    }
}

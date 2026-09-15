package cn.sunny.wallpaper;

import java.io.InterruptedIOException;
import java.util.concurrent.atomic.AtomicReference;

/** Exercises the actual worker guard without invoking Android services. */
public class WidgetCancellationTest {
    static void rejected() throws Exception {
        try { WallpaperEngine.check(); throw new AssertionError("Cancelled work can still continue"); }
        catch (InterruptedIOException expected) { }
    }
    public static void main(String[] args) throws Exception {
        WallpaperEngine.check();
        WallpaperEngine.widgetGuard.set(() -> true); WallpaperEngine.check();
        WallpaperEngine.widgetGuard.set(() -> false); rejected();
        AtomicReference<Throwable> error = new AtomicReference<>();
        Thread unrelated = new Thread(() -> { try { WallpaperEngine.check(); } catch (Throwable e) { error.set(e); } });
        unrelated.start(); unrelated.join();
        if (error.get() != null) throw new AssertionError("Widget cancellation leaked to a different task", error.get());
        WallpaperEngine.widgetGuard.remove(); WallpaperEngine.check();
        Thread.currentThread().interrupt(); rejected(); Thread.interrupted(); WallpaperEngine.check();
        System.out.println("PASS: live request, cancelled/expired request, thread isolation, guard removal, interruption");
    }
}

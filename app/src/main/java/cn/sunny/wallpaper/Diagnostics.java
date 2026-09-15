package cn.sunny.wallpaper;

import android.content.Context;
import java.io.*;

final class Diagnostics {
    static synchronized void record(Context c, String message) {
        android.util.Log.i("DailyWallpaper", message);
        File f = new File(c.getFilesDir(), "wallpaper-diagnostic.txt");
        try (FileWriter out = new FileWriter(f, f.length() < 65536)) {
            out.write(new java.util.Date() + " " + message + "\n");
        } catch (IOException ignored) { }
    }
}

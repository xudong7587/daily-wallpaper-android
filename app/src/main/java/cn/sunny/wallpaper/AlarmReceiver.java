package cn.sunny.wallpaper;
import android.content.*;
public class AlarmReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent i) {
        if (!Schedule.prefs(c).getBoolean("enabled", false)) return;
        if (Schedule.activeNow(c)) Schedule.enqueue(c);
        Schedule.refresh(c, false, true);
    }
}

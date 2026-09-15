package cn.sunny.wallpaper;

import java.util.Calendar;

public final class DailyTime {
    public static long next(long now, int hour, int minute) {
        Calendar time = Calendar.getInstance();
        time.setTimeInMillis(now);
        time.set(Calendar.HOUR_OF_DAY, hour);
        time.set(Calendar.MINUTE, minute);
        time.set(Calendar.SECOND, 0);
        time.set(Calendar.MILLISECOND, 0);
        if (time.getTimeInMillis() <= now) time.add(Calendar.DAY_OF_YEAR, 1);
        return time.getTimeInMillis();
    }
}

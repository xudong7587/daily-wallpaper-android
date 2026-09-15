package cn.sunny.wallpaper;

import java.util.Calendar;

/** Local-time window in which automatic wallpaper changes may run. */
public final class ActiveHours {
    public static boolean contains(long when, int startMinute, int endMinute) {
        validate(startMinute); validate(endMinute);
        if (startMinute == endMinute) return true;
        Calendar c = Calendar.getInstance(); c.setTimeInMillis(when);
        int minute = c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE);
        return startMinute < endMinute ? minute >= startMinute && minute < endMinute
            : minute >= startMinute || minute < endMinute;
    }
    public static long clamp(long candidate, int startMinute, int endMinute) {
        validate(startMinute); validate(endMinute);
        if (contains(candidate, startMinute, endMinute)) return candidate;
        Calendar start = Calendar.getInstance(); start.setTimeInMillis(candidate);
        start.set(Calendar.HOUR_OF_DAY, startMinute / 60);
        start.set(Calendar.MINUTE, startMinute % 60);
        start.set(Calendar.SECOND, 0); start.set(Calendar.MILLISECOND, 0);
        if (start.getTimeInMillis() <= candidate) start.add(Calendar.DAY_OF_MONTH, 1);
        return start.getTimeInMillis();
    }
    private static void validate(int minute) {
        if (minute < 0 || minute >= 1440) throw new IllegalArgumentException("Invalid minute of day");
    }
}

package cn.sunny.wallpaper;

public final class IntervalTime {
    public static final long MIN_MINUTES = 1, MAX_MINUTES = 43200;
    public static long milliseconds(long minutes) {
        if (minutes < MIN_MINUTES || minutes > MAX_MINUTES) throw new IllegalArgumentException("间隔请输入 1 分钟至 30 天");
        return minutes * 60000L;
    }
    public static long next(long now, long previous, long period, boolean consumed) {
        if (period <= 0) throw new IllegalArgumentException("Invalid period");
        if (previous <= 0) return now + period;
        if (!consumed) return previous; // Opening the app must not postpone a pending alarm.
        return previous + (Math.max(0, now - previous) / period + 1) * period;
    }
}

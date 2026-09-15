package cn.sunny.wallpaper;

import java.util.Calendar;
import java.util.TimeZone;

public class DailyTimeTest {
    static long at(int year, int month, int day, int hour, int minute) {
        Calendar c = Calendar.getInstance(); c.clear(); c.set(year, month - 1, day, hour, minute); return c.getTimeInMillis();
    }
    static void equal(long actual, long expected) { if (actual != expected) throw new AssertionError(actual + " != " + expected); }
    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"));
        equal(DailyTime.next(at(2026, 9, 8, 7, 59), 8, 0), at(2026, 9, 8, 8, 0));
        equal(DailyTime.next(at(2026, 9, 8, 8, 0), 8, 0), at(2026, 9, 9, 8, 0));
        equal(DailyTime.next(at(2026, 12, 31, 23, 59), 0, 0), at(2027, 1, 1, 0, 0));
        equal(DailyTime.next(at(2028, 2, 28, 10, 0), 8, 0), at(2028, 2, 29, 8, 0));
        TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"));
        equal(DailyTime.next(at(2026, 3, 7, 9, 0), 8, 0), at(2026, 3, 8, 8, 0));
        equal(DailyTime.next(at(2026, 10, 31, 9, 0), 8, 0), at(2026, 11, 1, 8, 0));
        System.out.println("PASS: same-day, next-day, year rollover, leap day, DST start/end (6 cases)");
    }
}

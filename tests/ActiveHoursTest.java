package cn.sunny.wallpaper;

import java.util.Calendar;

public class ActiveHoursTest {
    static long at(int year, int month, int day, int hour, int minute) {
        Calendar c = Calendar.getInstance(); c.clear(); c.set(year, month - 1, day, hour, minute); return c.getTimeInMillis();
    }
    static void require(boolean value) { if (!value) throw new AssertionError(); }
    static void same(long actual, long expected) { if (actual != expected) throw new AssertionError(actual + " != " + expected); }
    public static void main(String[] args) {
        int eight = 8 * 60, ten = 22 * 60;
        require(ActiveHours.contains(at(2026, 9, 15, 8, 0), eight, ten));
        require(ActiveHours.contains(at(2026, 9, 15, 21, 59), eight, ten));
        require(!ActiveHours.contains(at(2026, 9, 15, 22, 0), eight, ten));
        same(ActiveHours.clamp(at(2026, 9, 15, 23, 0), eight, ten), at(2026, 9, 16, 8, 0));
        same(ActiveHours.clamp(at(2026, 9, 15, 7, 0), eight, ten), at(2026, 9, 15, 8, 0));
        require(ActiveHours.contains(at(2026, 9, 15, 23, 0), 22 * 60, 7 * 60));
        require(ActiveHours.contains(at(2026, 9, 16, 6, 59), 22 * 60, 7 * 60));
        require(!ActiveHours.contains(at(2026, 9, 16, 7, 0), 22 * 60, 7 * 60));
        same(ActiveHours.clamp(at(2026, 9, 16, 12, 0), 22 * 60, 7 * 60), at(2026, 9, 16, 22, 0));
        require(ActiveHours.contains(at(2026, 9, 15, 3, 0), 0, 0));
        System.out.println("PASS: daytime, boundaries, overnight, clamping and all-day active hours");
    }
}

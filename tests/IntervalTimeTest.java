package cn.sunny.wallpaper;
public class IntervalTimeTest {
    static void same(long a, long b) { if (a != b) throw new AssertionError(a + " != " + b); }
    public static void main(String[] args) {
        long minute = IntervalTime.milliseconds(1);
        same(minute, 60000); same(IntervalTime.milliseconds(43200), 2592000000L);
        same(IntervalTime.next(1000, 0, minute, false), 61000);
        same(IntervalTime.next(2000, 61000, minute, false), 61000);
        same(IntervalTime.next(62000, 61000, minute, false), 61000);
        same(IntervalTime.next(61000, 61000, minute, true), 121000);
        same(IntervalTime.next(250000, 61000, minute, true), 301000);
        same(IntervalTime.next(500, 0, minute, false), 60500);
        for (long invalid : new long[]{-1, 0, 43201, Long.MAX_VALUE}) {
            try { IntervalTime.milliseconds(invalid); throw new AssertionError("Invalid interval accepted"); } catch (IllegalArgumentException expected) { }
        }
        System.out.println("PASS: interval bounds, initial schedule, preserve deadline, overdue alarm, advancement, missed periods, reboot reset");
    }
}

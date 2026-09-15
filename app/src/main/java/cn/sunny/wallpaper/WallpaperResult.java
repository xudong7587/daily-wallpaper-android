package cn.sunny.wallpaper;

/** WallpaperManager returns zero for a rejected request without throwing. */
public final class WallpaperResult {
    public static boolean confirmed(int result, int target, int home, int lock) {
        if (result <= 0) return false;
        boolean homeOk = (target & 1) == 0 || home == result;
        boolean lockOk = (target & 2) == 0 || lock == result || (target == 3 && lock < 0 && home == result);
        return homeOk && lockOk;
    }
}

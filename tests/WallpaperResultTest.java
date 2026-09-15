package cn.sunny.wallpaper;

public class WallpaperResultTest {
    static void require(boolean value) { if (!value) throw new AssertionError(); }
    public static void main(String[] args) {
        require(!WallpaperResult.confirmed(0, 3, 106, -1));
        require(!WallpaperResult.confirmed(-1, 2, 106, -1));
        require(!WallpaperResult.confirmed(107, 1, 106, -1));
        require(WallpaperResult.confirmed(107, 1, 107, 105));
        require(WallpaperResult.confirmed(107, 2, 106, 107));
        require(WallpaperResult.confirmed(107, 3, 107, -1));
        require(WallpaperResult.confirmed(107, 3, 107, 107));
        require(!WallpaperResult.confirmed(107, 3, 107, 105));
        require(!WallpaperResult.confirmed(107, 3, 106, 107));
        System.out.println("PASS: silent denial, stale wallpaper IDs, home/lock/both confirmation and partial failure");
    }
}

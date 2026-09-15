package cn.sunny.wallpaper;
import java.util.*;
import java.io.IOException;

/** Shared policy for manual, widget and scheduled changes. */
public final class WallpaperFlow {
    public interface Source {
        String refresh() throws Exception;
        List<String> images() throws Exception;
        void check() throws Exception;
        void apply(String id, boolean networkFailed) throws Exception;
    }
    public static void run(int mode, String previous, Random random, Source source) throws Exception {
        source.check(); Exception failure = null; String fresh = null;
        if (mode != 1) {
            try { fresh = source.refresh(); }
            catch (Exception e) { source.check(); failure = e; }
        }
        source.check();
        if (mode == 0 && failure == null) { source.apply(fresh, false); return; }
        List<String> images = source.images();
        if (images.isEmpty()) throw new IOException(failure == null ? "图库为空，请先添加图片" : "链接暂时不可用，图库也没有图片；请先导入本地图片后重试", failure);
        source.apply(Selection.choose(images, previous, random), failure != null);
    }
}

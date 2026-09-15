package cn.sunny.wallpaper;
import java.util.*;
import java.io.*;
public class WallpaperFlowTest {
    static class Source implements WallpaperFlow.Source {
        List<String> ids = new ArrayList<>(); String selected; boolean offline, fallback, cancelled; int downloads;
        public void check() throws Exception { if (cancelled) throw new InterruptedIOException("cancelled"); }
        public String refresh() throws Exception { downloads++; if (offline) throw new IOException("proxy failure"); ids.add("bing-today"); return "bing-today"; }
        public List<String> images() { return ids; }
        public void apply(String id, boolean failed) { selected = id; fallback = failed; }
    }
    static void require(boolean b) { if (!b) throw new AssertionError(); }
    public static void main(String[] args) throws Exception {
        for (int mode : new int[]{0,2}) {
            Source s = new Source(); s.offline = true; s.ids.addAll(Arrays.asList("local-a","local-b"));
            WallpaperFlow.run(mode,"local-a",new Random(1),s); require("local-b".equals(s.selected) && s.fallback);
            Source online = new Source(); online.ids.add("local-a");
            WallpaperFlow.run(mode,"local-a",new Random(1),online); require("bing-today".equals(online.selected) && !online.fallback);
        }
        Source today = new Source(); today.ids.addAll(Arrays.asList("local-a", "bing-yesterday"));
        WallpaperFlow.run(0,"bing-today",new Random(1),today); require("bing-today".equals(today.selected) && today.ids.contains("bing-today"));
        Source mixed = new Source(); mixed.ids.add("bing-yesterday");
        WallpaperFlow.run(2,"bing-today",new Random(1),mixed); require("bing-yesterday".equals(mixed.selected) && mixed.ids.contains("bing-today"));
        Source local = new Source(); local.offline = true; local.ids.add("bing-yesterday");
        WallpaperFlow.run(1,"",new Random(1),local); require(local.downloads == 0 && "bing-yesterday".equals(local.selected));
        Source empty = new Source(); empty.offline = true;
        try { WallpaperFlow.run(0,"",new Random(),empty); throw new AssertionError(); } catch (IOException expected) { require(empty.selected == null); }
        Source cancelled = new Source(); cancelled.cancelled = true;
        try { WallpaperFlow.run(0,"",new Random(),cancelled); throw new AssertionError(); } catch (InterruptedIOException expected) { require(cancelled.downloads == 0 && cancelled.selected == null); }
        System.out.println("PASS: link/proxy failure falls back in all online modes; new and historical Bing photos join random pool; offline library skips network; empty library and cancelled tasks never apply");
    }
}

package cn.sunny.wallpaper;

import android.content.*;
import android.database.Cursor;
import android.graphics.*;
import android.net.Uri;
import android.provider.DocumentsContract;
import java.io.*;
import java.util.*;

public final class Library {
    static File folder(Context c) throws IOException {
        File dir = new File(c.getFilesDir(), "library");
        if (!dir.isDirectory() && !dir.mkdirs()) throw new IOException("无法创建图库，请检查手机剩余空间");
        return dir;
    }
    static SharedPreferences meta(Context c) { return c.getSharedPreferences("library_meta", 0); }
    static File[] list(Context c) {
        File[] files = new File(c.getFilesDir(), "library").listFiles((dir, name) -> name.matches("[a-f0-9]{64}\\.img"));
        if (files == null) return new File[0];
        Arrays.sort(files, (a, b) -> Long.compare(b.lastModified(), a.lastModified())); return files;
    }
    static BitmapFactory.Options bounds(File f) throws IOException {
        BitmapFactory.Options o = new BitmapFactory.Options(); o.inJustDecodeBounds = true; BitmapFactory.decodeFile(f.getPath(), o);
        if (o.outWidth <= 0 || o.outHeight <= 0) throw new IOException("文件不是可读取的图片"); return o;
    }
    static Bitmap decode(File f, long pixels) throws IOException {
        BitmapFactory.Options o = bounds(f); o.inSampleSize = 1;
        while ((long)(o.outWidth / o.inSampleSize) * (o.outHeight / o.inSampleSize) > pixels) o.inSampleSize *= 2;
        o.inJustDecodeBounds = false; Bitmap image = BitmapFactory.decodeFile(f.getPath(), o);
        if (image == null) throw new IOException("图片解码失败"); return image;
    }
    static void copy(InputStream input, OutputStream output) throws IOException {
        byte[] buf = new byte[16384]; long count = 0; int n;
        while ((n = input.read(buf)) != -1) {
            WallpaperEngine.check(); count += n; if (count > 32L * 1024 * 1024) throw new IOException("图片超过 32 MB"); output.write(buf, 0, n);
        }
    }
    static synchronized File add(Context c, File input, String name, String source) throws Exception {
        return addResult(c, input, name, source).file;
    }
    static synchronized ContentStore.Result addResult(Context c, File input, String name, String source) throws Exception {
        bounds(input);
        ContentStore.Result result = ContentStore.put(folder(c), input);
        if (result.added) meta(c).edit().putString(result.file.getName() + ".name", name).putString(result.file.getName() + ".source", source).apply();
        return result;
    }
    static String title(Context c, File f) { return meta(c).getString(f.getName() + ".name", "壁纸"); }
    static synchronized void remove(Context c, File f) throws IOException {
        if (!f.getCanonicalFile().getParentFile().equals(folder(c).getCanonicalFile())) throw new IOException("无效图片");
        if (f.exists() && !f.delete()) throw new IOException("移除失败");
        meta(c).edit().remove(f.getName() + ".name").remove(f.getName() + ".source").apply();
    }
    interface Progress { void update(String message); }
    static String importTree(Context c, Uri tree, Progress progress) throws Exception {
        ArrayDeque<String> queue = new ArrayDeque<>(); Set<String> visited = new HashSet<>(); queue.add(DocumentsContract.getTreeDocumentId(tree));
        int added = 0, duplicates = 0, failed = 0, scanned = 0; final int limit = 5000;
        while (!queue.isEmpty() && scanned < limit) {
            WallpaperEngine.check(); String parent = queue.removeFirst(); if (!visited.add(parent)) continue;
            Uri children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, parent);
            try (Cursor cursor = c.getContentResolver().query(children, new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_MIME_TYPE, DocumentsContract.Document.COLUMN_DISPLAY_NAME}, null, null, null)) {
                if (cursor == null) throw new IOException("无法读取目录");
                while (cursor.moveToNext() && scanned < limit) {
                    WallpaperEngine.check(); scanned++; String id = cursor.getString(0), mime = cursor.getString(1), name = cursor.getString(2);
                    if (DocumentsContract.Document.MIME_TYPE_DIR.equals(mime)) { queue.add(id); continue; }
                    String lower = name == null ? "" : name.toLowerCase(Locale.ROOT);
                    if (!(mime != null && mime.startsWith("image/")) && !lower.matches(".*\\.(jpg|jpeg|png|webp|bmp|gif|heic|heif|avif)$")) continue;
                    File temp = File.createTempFile("import-", ".img", c.getCacheDir());
                    try {
                        try (InputStream in = c.getContentResolver().openInputStream(DocumentsContract.buildDocumentUriUsingTree(tree, id)); OutputStream out = new FileOutputStream(temp)) {
                            if (in == null) throw new IOException("读取失败"); copy(in, out);
                        }
                        if (addResult(c, temp, name == null ? "本地图片" : name, "本地目录").added) added++; else duplicates++;
                    } catch (InterruptedIOException e) { throw e; }
                    catch (Exception e) { failed++; }
                    finally { temp.delete(); }
                    progress.update("正在导入：新增 " + added + " · 重复 " + duplicates + " · 失败 " + failed);
                }
            } catch (SecurityException e) { throw new IOException("目录权限失效，请重新选择目录", e); }
        }
        return "导入完成：新增 " + added + " · 重复 " + duplicates + " · 失败 " + failed + (scanned >= limit ? "（达到单次 5000 项扫描上限，请选择更小的子目录）" : "");
    }
}

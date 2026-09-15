package cn.sunny.wallpaper;

import java.io.*;
import java.security.*;
import java.util.Locale;

/** Content-addressed storage, publishing only complete files. */
public final class ContentStore {
    public static final long MAX_BYTES = 32L * 1024 * 1024;
    public static final class Result {
        public final File file;
        public final boolean added;
        Result(File file, boolean added) { this.file = file; this.added = added; }
    }
    public static synchronized Result put(File directory, File source) throws IOException {
        if (!directory.isDirectory() && !directory.mkdirs()) throw new IOException("无法创建图库");
        File pending = File.createTempFile("import-", ".tmp", directory);
        try {
            MessageDigest digest;
            try { digest = MessageDigest.getInstance("SHA-256"); } catch (NoSuchAlgorithmException e) { throw new IOException(e); }
            try (InputStream in = new FileInputStream(source); OutputStream out = new FileOutputStream(pending)) {
                byte[] buffer = new byte[16384]; long total = 0; int n;
                while ((n = in.read(buffer)) != -1) {
                    if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException("任务已取消");
                    total += n; if (total > MAX_BYTES) throw new IOException("图片超过 32 MB");
                    digest.update(buffer, 0, n); out.write(buffer, 0, n);
                }
                if (total == 0) throw new IOException("图片文件为空");
            }
            StringBuilder hash = new StringBuilder(); for (byte b : digest.digest()) hash.append(String.format(Locale.ROOT, "%02x", b & 255));
            File result = new File(directory, hash + ".img");
            if (result.exists()) return new Result(result, false);
            if (!pending.renameTo(result)) throw new IOException("保存图片失败，请检查手机剩余空间");
            return new Result(result, true);
        } finally { pending.delete(); }
    }
}

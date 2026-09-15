package cn.sunny.wallpaper;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.net.Uri;
import android.os.Bundle;
import android.util.LruCache;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.text.DateFormat;
import java.util.*;
import java.util.concurrent.*;

public class GalleryActivity extends Activity {
    private static final int PICK_TREE = 20, EXPORT = 21;
    private final ExecutorService operations = Executors.newSingleThreadExecutor();
    private final ExecutorService thumbnails = Executors.newFixedThreadPool(2);
    private final LruCache<String, Bitmap> cache = new LruCache<String, Bitmap>(8 * 1024 * 1024) {
        @Override protected int sizeOf(String key, Bitmap value) { return value.getAllocationByteCount(); }
    };
    private File[] files = new File[0];
    private final ImageAdapter adapter = new ImageAdapter();
    private TextView summary;
    private Button importButton, rescanButton;
    private boolean busy;
    private volatile boolean closed;
    private String exportId;
    private int dp(int n) { return Ui.dp(this, n); }
    private TextView text(String value, int size) {
        TextView t = Ui.text(this, value, size, size >= 16 ? Ui.INK : Ui.MUTED); t.setPadding(dp(4), dp(6), dp(4), dp(6)); return t;
    }
    private Button button(String label, LinearLayout parent) {
        Button b = Ui.button(this, label, label.startsWith("＋") || label.equals("设为壁纸")); Ui.addButton(parent, b, this); return b;
    }
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        if (state != null) exportId = state.getString("exportId");
        getWindow().setStatusBarColor(0xfff5f7f3); getWindow().setNavigationBarColor(0xfff5f7f3);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(0xfff5f7f3);
        root.setPadding(dp(20), dp(14), dp(20), dp(18)); setContentView(root);
        root.setOnApplyWindowInsetsListener((v, i) -> {
            v.setPadding(dp(20) + i.getSystemWindowInsetLeft(), dp(14) + i.getSystemWindowInsetTop(), dp(20) + i.getSystemWindowInsetRight(), dp(18) + i.getSystemWindowInsetBottom()); return i.consumeSystemWindowInsets();
        });
        button("‹ 返回壁纸设置", root).setOnClickListener(v -> finish());
        TextView heading = Ui.title(this, "我的图库", 29); LinearLayout.LayoutParams headingLp = new LinearLayout.LayoutParams(-1, -2); headingLp.setMargins(dp(3), dp(8), 0, 0); root.addView(heading, headingLp);
        TextView intro = text("下载的 Bing 原图和本地图片，都安静地收在这里。", 13); LinearLayout.LayoutParams introLp = new LinearLayout.LayoutParams(-1, -2); introLp.setMargins(0, 0, 0, dp(8)); root.addView(intro, introLp);
        importButton = button("＋ 添加本地文件夹", root);
        importButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            try { startActivityForResult(intent, PICK_TREE); } catch (ActivityNotFoundException e) { toast("系统未提供文件夹选择器"); }
        });
        rescanButton = button("重新扫描已选目录", root);
        rescanButton.setOnClickListener(v -> scan(new ArrayList<>(folders())));
        rescanButton.setOnLongClickListener(v -> { manageFolders(); return true; });
        root.addView(text("导入包含子目录并按内容去重。长按“重新扫描”可管理目录；卸载前请先导出想长期保留的图片。", 11));
        summary = text("", 13); root.addView(summary);
        FrameLayout frame = new FrameLayout(this); root.addView(frame, new LinearLayout.LayoutParams(-1, 0, 1));
        GridView grid = new GridView(this); grid.setNumColumns(2); grid.setHorizontalSpacing(dp(10)); grid.setVerticalSpacing(dp(10)); grid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH); grid.setPadding(0, dp(6), 0, 0); grid.setClipToPadding(false);
        frame.addView(grid, new FrameLayout.LayoutParams(-1, -1));
        TextView empty = text("这里还没有图片\n\n添加一个本地文件夹，或在首页保存 Bing 图片。", 16); empty.setGravity(Gravity.CENTER);
        frame.addView(empty, new FrameLayout.LayoutParams(-1, -1)); grid.setEmptyView(empty); grid.setAdapter(adapter);
        grid.setOnItemClickListener((p, v, position, id) -> detail(files[position]));
        reload();
    }
    private Set<String> folders() { return new HashSet<>(Library.meta(this).getStringSet("folders", Collections.emptySet())); }
    private void toast(String message) { Toast.makeText(this, message, Toast.LENGTH_LONG).show(); }
    private void reload() {
        files = Library.list(this); adapter.notifyDataSetChanged();
        long bytes = 0; for (File f : files) bytes += f.length();
        summary.setText(String.format(Locale.getDefault(), "%d 张 · %.1f MB", files.length, bytes / 1048576.0));
        rescanButton.setText("重新扫描已选目录 · " + folders().size());
    }
    private void setBusy(boolean value) { busy = value; importButton.setEnabled(!value); rescanButton.setEnabled(!value); }
    private void scan(List<String> trees) {
        if (busy) return;
        if (trees.isEmpty()) { toast("请先添加本地文件夹"); return; }
        setBusy(true); summary.setText("正在读取目录…");
        operations.submit(() -> {
            StringBuilder results = new StringBuilder(); final long[] last = {0};
            for (String tree : trees) {
                if (Thread.currentThread().isInterrupted()) break;
                try {
                    String message = Library.importTree(getApplicationContext(), Uri.parse(tree), progress -> {
                        long now = System.currentTimeMillis(); if (now - last[0] < 200) return; last[0] = now;
                        runOnUiThread(() -> { if (!closed) summary.setText(progress); });
                    }); results.append(message).append('\n');
                } catch (Exception e) { results.append("导入未完成：").append(e.getMessage()).append('\n'); }
            }
            runOnUiThread(() -> { if (!closed) { setBusy(false); reload(); new AlertDialog.Builder(this).setTitle("目录导入结果").setMessage(results.toString().trim()).setPositiveButton("知道了", null).show(); } });
        });
    }
    private void manageFolders() {
        if (busy) return;
        ArrayList<String> trees = new ArrayList<>(folders()); if (trees.isEmpty()) { toast("尚未添加目录"); return; }
        String[] names = new String[trees.size()];
        for (int i = 0; i < names.length; i++) names[i] = Uri.decode(Uri.parse(trees.get(i)).getLastPathSegment());
        new AlertDialog.Builder(this).setTitle("移除目录记录（保留已导入图片）").setItems(names, (d, which) -> {
            Set<String> remaining = folders(); String tree = trees.get(which); remaining.remove(tree);
            Library.meta(this).edit().putStringSet("folders", remaining).apply();
            try { getContentResolver().releasePersistableUriPermission(Uri.parse(tree), Intent.FLAG_GRANT_READ_URI_PERMISSION); } catch (SecurityException ignored) { }
            reload();
        }).setNegativeButton("取消", null).show();
    }
    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (result != RESULT_OK || data == null || data.getData() == null) return;
        if (request == PICK_TREE) {
            Uri tree = data.getData();
            try {
                getContentResolver().takePersistableUriPermission(tree, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                Set<String> saved = folders(); saved.add(tree.toString()); Library.meta(this).edit().putStringSet("folders", saved).apply();
            } catch (SecurityException e) { toast("此目录不能保留长期授权，本次仍可导入"); }
            scan(Collections.singletonList(tree.toString()));
        } else if (request == EXPORT && exportId != null) {
            final String id = exportId; exportId = null; Uri destination = data.getData();
            operations.submit(() -> {
                String message;
                try (InputStream in = new FileInputStream(new File(Library.folder(this), id)); OutputStream out = getContentResolver().openOutputStream(destination)) {
                    if (out == null) throw new IOException("无法写入目标位置"); Library.copy(in, out); message = "原图已导出";
                } catch (Exception e) { message = "导出失败：" + e.getMessage(); }
                final String value = message; runOnUiThread(() -> { if (!closed) toast(value); });
            });
        }
    }
    private void detail(File file) {
        LinearLayout body = new LinearLayout(this); body.setOrientation(LinearLayout.VERTICAL); body.setPadding(dp(12), dp(8), dp(12), dp(8));
        ImageView image = new ImageView(this); image.setScaleType(ImageView.ScaleType.FIT_CENTER); image.setContentDescription(Library.title(this, file));
        body.addView(image, new LinearLayout.LayoutParams(-1, Math.min(dp(320), getResources().getDisplayMetrics().heightPixels / 2)));
        body.addView(text(Library.meta(this).getString(file.getName() + ".source", "图库") + " · " + DateFormat.getDateInstance().format(new Date(file.lastModified())), 12));
        Button use = button("设为壁纸", body), export = button("导出原图", body), remove = button("移出图库", body);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle(Library.title(this, file)).setView(body).setNegativeButton("关闭", null).create();
        thumbnails.submit(() -> {
            try {
                Bitmap bitmap = Library.decode(file, 2_000_000);
                runOnUiThread(() -> { if (!closed && dialog.isShowing()) image.setImageBitmap(bitmap); else bitmap.recycle(); });
            } catch (Exception ignored) { runOnUiThread(() -> { if (!closed) toast("图片读取失败"); }); }
        });
        use.setOnClickListener(v -> new AlertDialog.Builder(this).setTitle("设置到").setItems(new String[]{"桌面", "锁屏", "桌面与锁屏"}, (d, which) -> {
            use.setEnabled(false); long rev = Schedule.prefs(this).getLong("revision", 0);
            operations.submit(() -> {
                String result;
                try { WallpaperEngine.applyFile(getApplicationContext(), file, which + 1, rev, false, ""); result = "壁纸已更换"; }
                catch (Exception e) { result = "设置失败：" + e.getMessage(); }
                final String value = result; runOnUiThread(() -> { if (!closed) { use.setEnabled(true); toast(value); } });
            });
        }).show());
        export.setOnClickListener(v -> {
            try {
                String mime = Library.bounds(file).outMimeType; if (mime == null) mime = "image/jpeg";
                String extension = android.webkit.MimeTypeMap.getSingleton().getExtensionFromMimeType(mime); if (extension == null) extension = "img";
                exportId = file.getName(); startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType(mime).putExtra(Intent.EXTRA_TITLE, "Wallpaper-" + file.getName().substring(0, 12) + "." + extension), EXPORT);
            } catch (Exception e) { toast("无法导出：" + e.getMessage()); }
        });
        remove.setOnClickListener(v -> new AlertDialog.Builder(this).setTitle("移出这张图片？").setMessage("只删除图库中的副本，不影响原文件夹或已导出的图片。").setNegativeButton("取消", null).setPositiveButton("移出", (d, which) -> {
            try { Library.remove(this, file); cache.remove(file.getName()); dialog.dismiss(); reload(); }
            catch (Exception e) { toast(e.getMessage()); }
        }).show());
        dialog.show();
    }
    class ImageAdapter extends BaseAdapter {
        @Override public int getCount() { return files.length; }
        @Override public Object getItem(int position) { return files[position]; }
        @Override public long getItemId(int position) { return position; }
        @Override public View getView(int position, View recycled, android.view.ViewGroup parent) {
            LinearLayout cell;
            if (recycled == null) {
                cell = new LinearLayout(GalleryActivity.this); cell.setOrientation(LinearLayout.VERTICAL); cell.setPadding(dp(5), dp(5), dp(5), dp(5)); cell.setBackground(Ui.shape(0xffffffff, 16, 0xffe2e9e5, 1, GalleryActivity.this)); cell.setElevation(dp(1));
                ImageView picture = new ImageView(GalleryActivity.this); picture.setScaleType(ImageView.ScaleType.CENTER_CROP);
                cell.addView(picture, new LinearLayout.LayoutParams(-1, dp(150)));
                TextView label = text("", 12); label.setMaxLines(1); label.setEllipsize(android.text.TextUtils.TruncateAt.END); label.setPadding(dp(7), dp(7), dp(7), dp(4)); cell.addView(label);
            } else cell = (LinearLayout) recycled;
            File file = files[position]; ImageView picture = (ImageView)cell.getChildAt(0); ((TextView)cell.getChildAt(1)).setText(Library.title(GalleryActivity.this, file));
            picture.setTag(file.getName()); picture.setContentDescription(Library.title(GalleryActivity.this, file)); picture.setImageDrawable(null); picture.setBackgroundColor(0xffdce8dd);
            Bitmap cached = cache.get(file.getName());
            if (cached != null) picture.setImageBitmap(cached);
            else thumbnails.submit(() -> {
                if (closed) return;
                try {
                    Bitmap b = Library.decode(file, 120_000); cache.put(file.getName(), b);
                    runOnUiThread(() -> { if (!closed && file.getName().equals(picture.getTag())) picture.setImageBitmap(b); });
                } catch (Exception ignored) { }
            });
            return cell;
        }
    }
    @Override protected void onSaveInstanceState(Bundle out) { super.onSaveInstanceState(out); out.putString("exportId", exportId); }
    @Override protected void onDestroy() { closed = true; operations.shutdownNow(); thumbnails.shutdownNow(); cache.evictAll(); super.onDestroy(); }
}

package cn.sunny.wallpaper;

import android.app.*;
import android.content.*;
import android.content.res.ColorStateList;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import java.io.File;
import java.text.DateFormat;
import java.util.*;
import java.util.concurrent.*;

public class MainActivity extends Activity {
    private EditText url, intervalValue;
    private Switch enabled;
    private Spinner target, scheduleMode, intervalUnit;
    private LinearLayout intervalRow;
    private CheckBox useUrl, useLibrary;
    private Button libraryButton, download, time, activeStart, activeEnd, apply, saveButton;
    private TextView status, timing, permissionState;
    private TextView pageTitle;
    private ImageView preview;
    private ScrollView scrollView;
    private LinearLayout bottomNav;
    private LinearLayout[] panels;
    private Button[] tabs;
    private int currentTab;
    private int hour, minute, startMinute, endMinute;
    private boolean busy;
    private volatile boolean closed;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final android.content.SharedPreferences.OnSharedPreferenceChangeListener listener = (p, key) -> runOnUiThread(this::updateStatus);

    private int dp(int n) { return Ui.dp(this, n); }
    private int contentWidthDp() {
        android.util.DisplayMetrics metrics = getResources().getDisplayMetrics();
        return Math.min(720, Math.max(280, Math.round(metrics.widthPixels / metrics.density) - 40));
    }
    private int homePreviewHeightDp() {
        android.util.DisplayMetrics metrics = getResources().getDisplayMetrics();
        int height = Math.round(metrics.heightPixels / metrics.density);
        int width = contentWidthDp();
        if (getResources().getConfiguration().smallestScreenWidthDp >= 600) {
            return Math.max(320, Math.min(460, Math.round(width * 0.62f)));
        }
        return Math.max(280, Math.min(420, Math.round(height * 0.42f)));
    }
    private TextView note(String value) { return Ui.text(this, value, 13, Ui.MUTED); }
    private void addGap(LinearLayout parent, int height) { Space s = new Space(this); parent.addView(s, new LinearLayout.LayoutParams(1, dp(height))); }
    private void addControl(LinearLayout parent, View view, int height) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(height)); lp.setMargins(0, dp(9), 0, 0); parent.addView(view, lp);
    }
    private Spinner spinner(String[] options) {
        Spinner s = new Spinner(this); s.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, options));
        s.setPadding(dp(12), 0, dp(8), 0); s.setBackground(Ui.ripple(this, 0xffedf3ef, 0xffdfe9e3, 14)); return s;
    }
    private EditText input(String hint) {
        EditText e = new EditText(this); e.setHint(hint); e.setTextSize(14); e.setTextColor(Ui.INK); e.setHintTextColor(0xff89978f);
        e.setPadding(dp(14), 0, dp(14), 0); e.setBackground(Ui.shape(0xfff3f6f4, 14, 0xffdce5e0, 1, this)); return e;
    }
    private void styleCheck(CheckBox box) {
        box.setTextSize(15); box.setTextColor(Ui.INK); box.setButtonTintList(new ColorStateList(new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}}, new int[]{Ui.GREEN, 0xff8b9a92}));
        box.setMinHeight(dp(44));
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        android.content.SharedPreferences p = Schedule.prefs(this);
        if (!p.getBoolean("onboarding_complete", false) && !p.contains("revision")) {
            startActivity(new Intent(this, OnboardingActivity.class)); finish(); return;
        }
        getWindow().setStatusBarColor(0xfff5f7f3); getWindow().setNavigationBarColor(0xfff5f7f3);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        LinearLayout shell = new LinearLayout(this); shell.setOrientation(LinearLayout.VERTICAL); shell.setBackgroundColor(0xfff5f7f3);
        scrollView = new ScrollView(this); scrollView.setFillViewport(true); scrollView.setBackgroundColor(0xfff5f7f3);
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(20), dp(22), dp(20), dp(34));
        FrameLayout.LayoutParams rootLp = new FrameLayout.LayoutParams(dp(contentWidthDp() + 40), -2, Gravity.TOP | Gravity.CENTER_HORIZONTAL); scrollView.addView(root, rootLp);
        shell.addView(scrollView, new LinearLayout.LayoutParams(-1, 0, 1));
        bottomNav = new LinearLayout(this); bottomNav.setGravity(Gravity.CENTER); bottomNav.setPadding(dp(6), dp(4), dp(6), dp(4)); bottomNav.setBackgroundColor(Color.WHITE); bottomNav.setElevation(dp(12));
        shell.addView(bottomNav, new LinearLayout.LayoutParams(-1, dp(68))); setContentView(shell);
        shell.setOnApplyWindowInsetsListener((v, insets) -> {
            scrollView.setPadding(0, insets.getSystemWindowInsetTop(), 0, 0);
            bottomNav.setPadding(dp(6) + insets.getSystemWindowInsetLeft(), dp(4), dp(6) + insets.getSystemWindowInsetRight(), dp(4) + insets.getSystemWindowInsetBottom());
            bottomNav.getLayoutParams().height = dp(68) + insets.getSystemWindowInsetBottom(); bottomNav.requestLayout();
            return insets.consumeSystemWindowInsets();
        });

        TextView eyebrow = Ui.text(this, "每日换壁纸", 13, Ui.GREEN); root.addView(eyebrow);
        pageTitle = Ui.title(this, "今天，换一处风景", 30); LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(-1, -2); titleLp.setMargins(0, dp(5), 0, dp(13)); root.addView(pageTitle, titleLp);
        String[] tabLabels = {"首页", "自动化", "图片", "权限"}; int[] tabIcons = {R.drawable.nav_home, R.drawable.nav_schedule, R.drawable.nav_image, R.drawable.nav_permissions}; tabs = new Button[tabLabels.length];
        for (int i = 0; i < tabs.length; i++) {
            final int selected = i; tabs[i] = new Button(this); tabs[i].setText(tabLabels[i]); tabs[i].setTextSize(11); tabs[i].setAllCaps(false); tabs[i].setGravity(Gravity.CENTER); tabs[i].setCompoundDrawablesWithIntrinsicBounds(0, tabIcons[i], 0, 0); tabs[i].setCompoundDrawablePadding(dp(2)); tabs[i].setStateListAnimator(null); tabs[i].setMinHeight(0); tabs[i].setMinimumHeight(0); tabs[i].setPadding(0, dp(3), 0, dp(2)); tabs[i].setOnClickListener(v -> selectTab(selected));
            bottomNav.addView(tabs[i], new LinearLayout.LayoutParams(0, -1, 1));
        }
        LinearLayout content = new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL); root.addView(content, new LinearLayout.LayoutParams(-1, -2));
        panels = new LinearLayout[4];
        for (int i = 0; i < panels.length; i++) { panels[i] = new LinearLayout(this); panels[i].setOrientation(LinearLayout.VERTICAL); content.addView(panels[i], new LinearLayout.LayoutParams(-1, -2)); }

        LinearLayout hero = Ui.card(this); hero.setPadding(0, 0, 0, dp(16));
        preview = new ImageView(this); preview.setScaleType(ImageView.ScaleType.CENTER_CROP);
        GradientDrawable previewBg = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{0xff244b40, 0xffb6ceb0}); previewBg.setCornerRadii(new float[]{dp(22),dp(22),dp(22),dp(22),0,0,0,0});
        preview.setBackground(previewBg); preview.setClipToOutline(true); hero.addView(preview, new LinearLayout.LayoutParams(-1, dp(homePreviewHeightDp())));
        status = Ui.title(this, "准备好换一张", 17); LinearLayout.LayoutParams statusLp = new LinearLayout.LayoutParams(-1, -2); statusLp.setMargins(dp(18), dp(15), dp(18), 0); hero.addView(status, statusLp);
        timing = note(""); LinearLayout.LayoutParams timingLp = new LinearLayout.LayoutParams(-1, -2); timingLp.setMargins(dp(18), dp(6), dp(18), 0); hero.addView(timing, timingLp);
        Ui.addCard(panels[0], hero, this);

        LinearLayout quick = new LinearLayout(this); quick.setWeightSum(2);
        apply = Ui.button(this, "换一张", true); quick.addView(apply, new LinearLayout.LayoutParams(0, dp(52), 1));
        libraryButton = Ui.button(this, "我的图库", false); LinearLayout.LayoutParams libraryLp = new LinearLayout.LayoutParams(0, dp(52), 1); libraryLp.setMargins(dp(10), 0, 0, 0); quick.addView(libraryButton, libraryLp);
        LinearLayout.LayoutParams quickLp = new LinearLayout.LayoutParams(-1, -2); quickLp.setMargins(0, 0, 0, dp(14)); panels[0].addView(quick, quickLp);
        apply.setOnClickListener(v -> changeNow()); libraryButton.setOnClickListener(v -> startActivity(new Intent(this, GalleryActivity.class)));

        LinearLayout automation = Ui.card(this);
        LinearLayout autoHead = new LinearLayout(this); autoHead.setGravity(Gravity.CENTER_VERTICAL); automation.addView(autoHead);
        LinearLayout autoWords = new LinearLayout(this); autoWords.setOrientation(LinearLayout.VERTICAL); autoHead.addView(autoWords, new LinearLayout.LayoutParams(0, -2, 1));
        autoWords.addView(Ui.title(this, "自动更换", 19)); autoWords.addView(note("只在你允许的时间里运行"));
        enabled = new Switch(this); enabled.setShowText(false); enabled.setButtonTintList(ColorStateList.valueOf(Ui.GREEN)); autoHead.addView(enabled);
        scheduleMode = spinner(new String[]{"每天固定时间", "每隔一段时间"}); addControl(automation, scheduleMode, 50);
        time = Ui.button(this, "", false); addControl(automation, time, 50);
        time.setOnClickListener(v -> new TimePickerDialog(this, (picker, h, m) -> { hour = h; minute = m; showTime(); }, hour, minute, true).show());
        intervalRow = new LinearLayout(this);
        intervalValue = input("间隔"); intervalValue.setInputType(android.text.InputType.TYPE_CLASS_NUMBER); intervalRow.addView(intervalValue, new LinearLayout.LayoutParams(0, dp(50), 1));
        intervalUnit = spinner(new String[]{"分钟", "小时", "天"}); LinearLayout.LayoutParams unitLp = new LinearLayout.LayoutParams(dp(112), dp(50)); unitLp.setMargins(dp(8), 0, 0, 0); intervalRow.addView(intervalUnit, unitLp);
        addControl(automation, intervalRow, 50);
        scheduleMode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) { time.setVisibility(position == 0 ? View.VISIBLE : View.GONE); intervalRow.setVisibility(position == 1 ? View.VISIBLE : View.GONE); }
            public void onNothingSelected(AdapterView<?> parent) { }
        });
        addGap(automation, 12); automation.addView(Ui.title(this, "允许时段", 14));
        LinearLayout activeRow = new LinearLayout(this);
        activeStart = Ui.button(this, "", false); activeRow.addView(activeStart, new LinearLayout.LayoutParams(0, dp(50), 1));
        activeEnd = Ui.button(this, "", false); LinearLayout.LayoutParams endLp = new LinearLayout.LayoutParams(0, dp(50), 1); endLp.setMargins(dp(8), 0, 0, 0); activeRow.addView(activeEnd, endLp); addControl(automation, activeRow, 50);
        activeStart.setOnClickListener(v -> new TimePickerDialog(this, (picker, h, m) -> { startMinute = h * 60 + m; showActiveHours(); }, startMinute / 60, startMinute % 60, true).show());
        activeEnd.setOnClickListener(v -> new TimePickerDialog(this, (picker, h, m) -> { endMinute = h * 60 + m; showActiveHours(); }, endMinute / 60, endMinute % 60, true).show());
        TextView activeNote = note("小组件和手动换图不受时段限制"); LinearLayout.LayoutParams activeNoteLp = new LinearLayout.LayoutParams(-1, -2); activeNoteLp.setMargins(0, dp(7), 0, 0); automation.addView(activeNote, activeNoteLp);
        addGap(automation, 12); automation.addView(Ui.title(this, "设置到", 14));
        target = spinner(new String[]{"桌面壁纸", "锁屏壁纸", "桌面与锁屏"}); addControl(automation, target, 50);
        saveButton = Ui.button(this, "保存自动化设置", true); Ui.addButton(automation, saveButton, this); saveButton.setOnClickListener(v -> { if (save()) Toast.makeText(this, "设置已保存", Toast.LENGTH_SHORT).show(); });
        Ui.addCard(panels[1], automation, this);

        LinearLayout sources = Ui.card(this); Ui.sectionTitle(this, "图片来源", "链接与图库可以同时使用", sources);
        useUrl = new CheckBox(this); useUrl.setText("当天 Bing 图片"); styleCheck(useUrl); sources.addView(useUrl);
        useLibrary = new CheckBox(this); useLibrary.setText("从已保存图库随机选择"); styleCheck(useLibrary); sources.addView(useLibrary);
        TextView sourceNote = note("链接不可用时会自动使用图库；下载成功的 Bing 图片也会存入图库。起码选择一项。默认仅使用当天 Bing 图片。"); LinearLayout.LayoutParams sourceNoteLp = new LinearLayout.LayoutParams(-1, -2); sourceNoteLp.setMargins(0, dp(5), 0, 0); sources.addView(sourceNote, sourceNoteLp);
        Ui.addCard(panels[2], sources, this);

        LinearLayout link = Ui.card(this); Ui.sectionTitle(this, "图片链接", "默认地址每天返回一张 Bing 壁纸", link);
        url = input("HTTP / HTTPS 图片直链"); url.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_URI); url.setMaxLines(3); addControl(link, url, 64);
        Button defaultUrl = Ui.button(this, "恢复默认 Bing 链接", false); Ui.addButton(link, defaultUrl, this); defaultUrl.setOnClickListener(v -> url.setText(WallpaperEngine.DEFAULT_URL));
        download = Ui.button(this, "仅刷新并保存到图库", false); Ui.addButton(link, download, this); download.setOnClickListener(v -> downloadOnly());
        Ui.addCard(panels[2], link, this);

        LinearLayout permissions = Ui.card(this); Ui.sectionTitle(this, "权限与后台", "权限不完整时，系统可能静默拒绝换图", permissions);
        permissionState = note(""); permissions.addView(permissionState);
        Button permissionGuide = Ui.button(this, "查看完整权限引导", true); Ui.addButton(permissions, permissionGuide, this); permissionGuide.setOnClickListener(v -> startActivity(new Intent(this, OnboardingActivity.class).putExtra("return_only", true)));
        Button wallpaperPermission = Ui.button(this, "检查“设置壁纸”权限", false); Ui.addButton(permissions, wallpaperPermission, this); wallpaperPermission.setOnClickListener(v -> openAppSettings());
        Button exactPermission = Ui.button(this, "检查精确定时权限", false); Ui.addButton(permissions, exactPermission, this); exactPermission.setOnClickListener(v -> openAlarmSettings());
        Button backgroundPermission = Ui.button(this, "后台运行与电池设置", false); Ui.addButton(permissions, backgroundPermission, this); backgroundPermission.setOnClickListener(v -> openAppSettings());
        if (Build.VERSION.SDK_INT >= 31) {
            Button iconSize = Ui.button(this, "调整桌面小组件图标", false); Ui.addButton(permissions, iconSize, this); iconSize.setOnClickListener(v -> showIconSize());
        }
        Ui.addCard(panels[3], permissions, this);

        TextView footer = note("无账号 · 无广告 · 设置与图库仅保存在本机"); footer.setGravity(Gravity.CENTER); panels[3].addView(footer);

        url.setText(p.getString("url", WallpaperEngine.DEFAULT_URL));
        hour = p.getInt("hour", 8); minute = p.getInt("minute", 0); showTime();
        startMinute = Schedule.startMinute(p); endMinute = Schedule.endMinute(p); showActiveHours();
        target.setSelection(p.getInt("target", 1) - 1); enabled.setChecked(p.getBoolean("enabled", false));
        int mode = p.getInt("mode", 0); useUrl.setChecked(mode != 1); useLibrary.setChecked(mode != 0);
        scheduleMode.setSelection(p.getBoolean("interval_mode", false) ? 1 : 0);
        long savedMinutes = p.getLong("interval_minutes", 60); int unit = savedMinutes % 1440 == 0 ? 2 : savedMinutes % 60 == 0 ? 1 : 0;
        intervalUnit.setSelection(unit); intervalValue.setText(Long.toString(savedMinutes / (unit == 2 ? 1440 : unit == 1 ? 60 : 1)));
        if (state != null) restoreDraft(state);
        selectTab(state == null ? 0 : state.getInt("draft_tab", 0));
        p.registerOnSharedPreferenceChangeListener(listener); updateStatus();
    }

    private void selectTab(int selected) {
        currentTab = Math.max(0, Math.min(panels.length - 1, selected));
        String[] titles = {"今天，换一处风景", "安排你的节奏", "选择喜欢的风景", "让换图稳定运行"};
        pageTitle.setText(titles[currentTab]);
        for (int i = 0; i < panels.length; i++) {
            boolean active = i == currentTab; panels[i].setVisibility(active ? View.VISIBLE : View.GONE);
            tabs[i].setTextColor(active ? Color.WHITE : Ui.INK);
            tabs[i].setCompoundDrawableTintList(ColorStateList.valueOf(active ? Ui.GREEN : 0xff788880));
            tabs[i].setTextColor(active ? Ui.GREEN : 0xff788880);
            tabs[i].setBackground(Ui.ripple(this, 0x00ffffff, 0xffe8efeb, 13));
        }
        if (scrollView != null) scrollView.post(() -> scrollView.smoothScrollTo(0, 0));
    }

    private void restoreDraft(Bundle state) {
        url.setText(state.getString("draft_url", url.getText().toString()));
        hour = state.getInt("draft_hour", hour); minute = state.getInt("draft_minute", minute); showTime();
        startMinute = state.getInt("draft_active_start", startMinute); endMinute = state.getInt("draft_active_end", endMinute); showActiveHours();
        target.setSelection(state.getInt("draft_target", target.getSelectedItemPosition())); enabled.setChecked(state.getBoolean("draft_enabled", enabled.isChecked()));
        useUrl.setChecked(state.getBoolean("draft_use_url", useUrl.isChecked())); useLibrary.setChecked(state.getBoolean("draft_use_library", useLibrary.isChecked()));
        scheduleMode.setSelection(state.getInt("draft_schedule", scheduleMode.getSelectedItemPosition())); intervalUnit.setSelection(state.getInt("draft_unit", intervalUnit.getSelectedItemPosition()));
        intervalValue.setText(state.getString("draft_interval", intervalValue.getText().toString()));
    }
    private void openAppSettings() { try { startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName()))); } catch (ActivityNotFoundException e) { Toast.makeText(this, "请在系统设置中打开本应用的权限页面", Toast.LENGTH_LONG).show(); } }
    private void openAlarmSettings() {
        Intent i = Build.VERSION.SDK_INT >= 31 ? new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + getPackageName())) : new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName()));
        try { startActivity(i); } catch (ActivityNotFoundException e) { openAppSettings(); }
    }
    private void showTime() { if (time != null) time.setText(String.format(Locale.getDefault(), "每天  %02d:%02d", hour, minute)); }
    private void showActiveHours() {
        if (activeStart == null) return;
        activeStart.setText(String.format(Locale.getDefault(), "开始  %02d:%02d", startMinute / 60, startMinute % 60));
        activeEnd.setText(String.format(Locale.getDefault(), "结束  %02d:%02d", endMinute / 60, endMinute % 60));
    }
    private boolean save() {
        String value = url.getText().toString().trim(); boolean interval = scheduleMode.getSelectedItemPosition() == 1;
        long minutes = Schedule.prefs(this).getLong("interval_minutes", 60);
        if (interval) {
            try { long number = Long.parseLong(intervalValue.getText().toString().trim()); minutes = Math.multiplyExact(number, intervalUnit.getSelectedItemPosition() == 2 ? 1440L : intervalUnit.getSelectedItemPosition() == 1 ? 60L : 1L); IntervalTime.milliseconds(minutes); }
            catch (Exception e) { intervalValue.setError("请输入 1 分钟至 30 天的有效整数"); return false; }
        }
        if (!useUrl.isChecked() && !useLibrary.isChecked()) { useUrl.setChecked(true); value = WallpaperEngine.DEFAULT_URL; url.setText(value); }
        if (useUrl.isChecked() && !WallpaperEngine.valid(value)) { url.setError("请输入完整的 HTTP / HTTPS 图片链接"); return false; }
        boolean restart;
        synchronized (Schedule.class) {
            android.content.SharedPreferences p = Schedule.prefs(this);
            restart = p.getBoolean("interval_mode", false) != interval || p.getLong("interval_minutes", 60) != minutes || p.getBoolean("enabled", false) != enabled.isChecked() || Schedule.startMinute(p) != startMinute || Schedule.endMinute(p) != endMinute;
            p.edit().putBoolean("interval_mode", interval).putLong("interval_minutes", minutes).putInt("active_start", startMinute).putInt("active_end", endMinute).apply();
            p.edit().putString("url", value).putInt("mode", useUrl.isChecked() ? (useLibrary.isChecked() ? 2 : 0) : 1).putInt("hour", hour).putInt("minute", minute).putInt("target", target.getSelectedItemPosition() + 1).putBoolean("enabled", enabled.isChecked()).putLong("revision", p.getLong("revision", 0) + 1).putInt("failures", 0).apply();
        }
        getSystemService(android.app.job.JobScheduler.class).cancel(Schedule.JOB_ID); Schedule.refresh(this, restart, false); updateStatus(); return true;
    }
    private void setBusy(boolean value) {
        busy = value; apply.setEnabled(!value); download.setEnabled(!value); saveButton.setEnabled(!value);
        apply.setText(value ? "正在更换…" : "换一张");
    }
    private void changeNow() {
        if (busy || !save()) return; setBusy(true);
        android.content.SharedPreferences p = Schedule.prefs(this); String address = p.getString("url", WallpaperEngine.DEFAULT_URL); int destination = p.getInt("target", 1); long rev = p.getLong("revision", 0); int mode = p.getInt("mode", 0);
        executor.submit(() -> {
            try { WallpaperEngine.apply(getApplicationContext(), address, destination, rev, false, mode); }
            catch (Exception e) { if (p.getLong("revision", 0) == rev) p.edit().putString("status", "更换失败：" + e.getMessage()).apply(); }
            runOnUiThread(() -> { if (!closed) { setBusy(false); updateStatus(); } });
        });
    }
    private void downloadOnly() {
        if (busy) return; String address = url.getText().toString().trim();
        if (!WallpaperEngine.valid(address)) { url.setError("请输入完整的 HTTP / HTTPS 图片链接"); return; }
        setBusy(true); download.setText("正在获取图片…");
        executor.submit(() -> {
            String result; try { WallpaperEngine.refresh(getApplicationContext(), address); result = "图片已保存到图库（相同图片自动去重）"; } catch (Exception e) { result = "保存失败：" + e.getMessage(); }
            final String message = result; runOnUiThread(() -> { if (!closed) { setBusy(false); download.setText("仅刷新并保存到图库"); updateStatus(); Toast.makeText(this, message, Toast.LENGTH_LONG).show(); } });
        });
    }
    private void showIconSize() {
        LinearLayout content = new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL); content.setPadding(dp(24), dp(12), dp(24), dp(8));
        TextView value = Ui.text(this, "", 15, Ui.INK); content.addView(value); SeekBar slider = new SeekBar(this); slider.setMax(20); slider.setProgress(Math.max(0, Math.min(20, Schedule.prefs(this).getInt("widget_icon_dp", 52) - 44))); content.addView(slider);
        value.setText("图标大小 " + Math.round((slider.getProgress() + 44) * 100f / 52) + "%"); slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int n, boolean user) { value.setText("图标大小 " + Math.round((n + 44) * 100f / 52) + "%"); } public void onStartTrackingTouch(SeekBar s) { } public void onStopTrackingTouch(SeekBar s) { }
        });
        new AlertDialog.Builder(this).setTitle("桌面小组件图标").setView(content).setNegativeButton("取消", null).setNeutralButton("恢复标准", (d, w) -> { Schedule.prefs(this).edit().putInt("widget_icon_dp", 52).apply(); WidgetRun.update(this); }).setPositiveButton("应用", (d, w) -> { Schedule.prefs(this).edit().putInt("widget_icon_dp", slider.getProgress() + 44).apply(); WidgetRun.update(this); }).show();
    }
    private void updateStatus() {
        if (status == null) return; android.content.SharedPreferences p = Schedule.prefs(this);
        libraryButton.setText("图库 · " + Library.list(this).length + " 张"); status.setText(p.getString("status", "尚未更换壁纸"));
        long next = p.getLong("next", 0); String hours = String.format(Locale.getDefault(), "%02d:%02d–%02d:%02d", Schedule.startMinute(p) / 60, Schedule.startMinute(p) % 60, Schedule.endMinute(p) / 60, Schedule.endMinute(p) % 60);
        timing.setText(p.getBoolean("enabled", false) && next > 0 ? "自动更换已开启 · " + hours + "\n下次 " + DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(new Date(next)) : "自动更换已关闭");
        permissionState.setText((Schedule.exact(this) ? "精确定时已开启" : "精确定时尚未开启") + "\n部分 vivo 手机还需手动允许“设置壁纸”权限。");
        File image = new File(getFilesDir(), "preview.jpg"); if (image.exists()) preview.setImageBitmap(BitmapFactory.decodeFile(image.getPath()));
    }
    @Override protected void onResume() { super.onResume(); if (status != null) { Schedule.refresh(this); updateStatus(); try { WallpaperWidget.updateAll(this); } catch (RuntimeException ignored) { } } }
    @Override protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out); out.putString("draft_url", url.getText().toString()); out.putInt("draft_hour", hour); out.putInt("draft_minute", minute); out.putInt("draft_active_start", startMinute); out.putInt("draft_active_end", endMinute);
        out.putInt("draft_tab", currentTab);
        out.putInt("draft_target", target.getSelectedItemPosition()); out.putBoolean("draft_enabled", enabled.isChecked()); out.putBoolean("draft_use_url", useUrl.isChecked()); out.putBoolean("draft_use_library", useLibrary.isChecked()); out.putInt("draft_schedule", scheduleMode.getSelectedItemPosition()); out.putInt("draft_unit", intervalUnit.getSelectedItemPosition()); out.putString("draft_interval", intervalValue.getText().toString());
    }
    @Override protected void onDestroy() { closed = true; Schedule.prefs(this).unregisterOnSharedPreferenceChangeListener(listener); executor.shutdownNow(); super.onDestroy(); }
}

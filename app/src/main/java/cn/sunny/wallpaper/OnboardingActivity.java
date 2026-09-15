package cn.sunny.wallpaper;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;

public class OnboardingActivity extends Activity {
    private LinearLayout root;
    private TextView alarmState;
    private int dp(int n) { return Ui.dp(this, n); }
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(0xfff5f7f3); getWindow().setNavigationBarColor(0xfff5f7f3);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true); scroll.setBackgroundColor(0xfff5f7f3);
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(22), dp(28), dp(22), dp(28)); scroll.addView(root); setContentView(scroll);
        scroll.setOnApplyWindowInsetsListener((v, insets) -> { v.setPadding(0, insets.getSystemWindowInsetTop(), 0, insets.getSystemWindowInsetBottom()); return insets.consumeSystemWindowInsets(); });

        TextView eyebrow = Ui.text(this, "欢迎使用", 13, Ui.GREEN); eyebrow.setAllCaps(false); root.addView(eyebrow);
        TextView title = Ui.title(this, "先让每一次换图都可靠", 30); LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(-1, -2); titleLp.setMargins(0, dp(7), 0, dp(8)); root.addView(title, titleLp);
        TextView intro = Ui.text(this, "完成下面三项设置后，桌面小组件和定时任务才能稳定工作。大约需要一分钟。", 15, Ui.MUTED); root.addView(intro);

        permissionCard("1", "允许设置壁纸", "这是换图的核心权限。vivo 等手机会把它放在“所有权限”或“其他权限”里。", "打开应用权限", v -> openAppSettings());
        LinearLayout alarm = permissionCard("2", "允许准时执行", "开启精确定时，让每日任务更接近你设置的时间。", "打开定时权限", v -> openAlarmSettings());
        alarmState = Ui.text(this, "", 12, Ui.MUTED); alarm.addView(alarmState, 2);
        permissionCard("3", "允许后台运行", "允许自启动，并把电池策略设为“不限制”，避免锁屏后任务被暂停。", "打开后台设置", v -> openAppSettings());

        Button done = Ui.button(this, "进入应用", true); Ui.addButton(root, done, this);
        done.setOnClickListener(v -> {
            Schedule.prefs(this).edit().putBoolean("onboarding_complete", true).apply();
            if (!getIntent().getBooleanExtra("return_only", false)) startActivity(new Intent(this, MainActivity.class));
            finish();
        });
        TextView later = Ui.text(this, "权限也可以稍后在首页继续设置", 12, Ui.MUTED); later.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams laterLp = new LinearLayout.LayoutParams(-1, -2); laterLp.setMargins(0, dp(10), 0, 0); root.addView(later, laterLp);
        updateState();
    }
    private LinearLayout permissionCard(String number, String title, String body, String action, View.OnClickListener listener) {
        LinearLayout card = Ui.card(this); LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2); lp.setMargins(0, dp(14), 0, 0); root.addView(card, lp);
        LinearLayout head = new LinearLayout(this); head.setGravity(Gravity.CENTER_VERTICAL); card.addView(head);
        TextView badge = Ui.title(this, number, 14); badge.setGravity(Gravity.CENTER); badge.setTextColor(Color.WHITE); badge.setBackground(Ui.shape(Ui.GREEN, 12, 0, 0, this)); head.addView(badge, new LinearLayout.LayoutParams(dp(34), dp(34)));
        TextView heading = Ui.title(this, title, 17); LinearLayout.LayoutParams headingLp = new LinearLayout.LayoutParams(0, -2, 1); headingLp.setMargins(dp(12), 0, 0, 0); head.addView(heading, headingLp);
        TextView description = Ui.text(this, body, 13, Ui.MUTED); LinearLayout.LayoutParams descLp = new LinearLayout.LayoutParams(-1, -2); descLp.setMargins(0, dp(11), 0, 0); card.addView(description, descLp);
        Button button = Ui.button(this, action, false); Ui.addButton(card, button, this); button.setOnClickListener(listener); return card;
    }
    private void openAppSettings() { startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName()))); }
    private void openAlarmSettings() {
        Intent i = Build.VERSION.SDK_INT >= 31 ? new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + getPackageName())) : new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName()));
        try { startActivity(i); } catch (ActivityNotFoundException e) { openAppSettings(); }
    }
    private void updateState() { if (alarmState != null) alarmState.setText(Schedule.exact(this) ? "已开启精确定时" : "尚未开启精确定时"); }
    @Override protected void onResume() { super.onResume(); updateState(); }
}

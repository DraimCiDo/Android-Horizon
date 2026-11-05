package dev.horizonlab.horizon;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.StatFs;
import android.provider.DocumentsContract;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Portrait launcher around the existing Android runtime, setup service and profile. */
public class LauncherActivity extends Activity {
    private static final int SETUP = 1, BACKUP = 2, RESTORE = 3;
    private LinearLayout card;
    private TextView message;
    private String tab = "play", notice = "";
    private boolean busy;
    private long lastProgress;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (state != null) tab = state.getString("tab", "play");
        draw();
    }

    @Override protected void onResume() { super.onResume(); if (!busy) draw(); }
    @Override protected void onSaveInstanceState(Bundle state) { super.onSaveInstanceState(state); state.putString("tab", tab); }

    private void draw() {
        card = Ui.card(this);
        card.addView(Ui.text(this, "HORIZON", 32, Ui.TEXT, true));
        card.addView(Ui.text(this, "Android · " + version(), 14, Ui.MUTED, false));
        LinearLayout tabs = new LinearLayout(this);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        String[] ids = {"play", "files", "build"};
        String[] labels = {Ui.t("Играть", "Play"), Ui.t("Файлы", "Files"), Ui.t("Сборка", "Build")};
        for (int i = 0; i < ids.length; i++) {
            final String id = ids[i];
            Button button = Ui.button(this, labels[i], tab.equals(id));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
            lp.setMargins(i == 0 ? 0 : Ui.dp(this, 6), 0, 0, 0);
            tabs.addView(button, lp);
            button.setOnClickListener(v -> { tab = id; notice = ""; draw(); });
        }
        card.addView(tabs, Ui.gap(this, 18));
        if (tab.equals("play")) playPage();
        else if (tab.equals("files")) filesPage();
        else buildPage();
        message = Ui.text(this, notice, 14, Ui.WARN, false);
        message.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        card.addView(message, Ui.gap(this, 14));
    }

    private void playPage() {
        boolean ready = SetupActivity.nativeReady(SetupActivity.home(this)) == 1;
        Ui.section(card, ready ? Ui.t("ГОТОВО К ИГРЕ", "READY TO PLAY") : Ui.t("ПЕРВЫЙ ЗАПУСК", "FIRST LAUNCH"));
        card.addView(Ui.text(this, "Minecraft: Java Edition 26.3", 21, Ui.TEXT, true));
        card.addView(Ui.text(this, ready ? Ui.t("Файлы подготовлены. Можно запускать игру.", "Game files are ready. You can play.")
                : Ui.t("Файлы уже в APK. Подготовка распакует их на телефон.", "Game files are in the APK. Setup unpacks them on your phone."), 14, Ui.MUTED, false));
        button(HorizonActivity.active ? Ui.t("Продолжить игру", "Resume game") : ready ? Ui.t("Играть", "Play") : Ui.t("Подготовить игру", "Prepare game"), true, () -> {
            if (ready) startActivity(new Intent(this, HorizonActivity.class));
            else startActivityForResult(new Intent(this, SetupActivity.class), SETUP);
        });
        Ui.section(card, Ui.t("АККАУНТ И УПРАВЛЕНИЕ", "ACCOUNT AND CONTROLS"));
        String account = Msa.signedIn(this);
        card.addView(Ui.text(this, account.isEmpty() ? Ui.t("Microsoft не подключён", "Microsoft is not connected") : "Microsoft · " + account, 17, Ui.TEXT, true));
        card.addView(Ui.text(this, Ui.t("Профиль игрока, скин, вход Microsoft, моды и драйвер Vulkan.",
                "Player profile, skin, Microsoft sign-in, mods and Vulkan driver."), 14, Ui.MUTED, false));
        button(Ui.t("Открыть настройки профиля", "Open profile settings"), false,
                () -> startActivity(new Intent(this, ProfileActivity.class)));
        if (android.os.Build.MODEL.startsWith("SM-S938")) {
            Ui.section(card, "GALAXY S25 ULTRA");
            card.addView(Ui.text(this, Ui.t("Профиль устройства: до 60 FPS, дальность 8 чанков, масштаб 75%. Ваши изменения в игре сохраняются.",
                    "Device defaults: up to 60 FPS, 8 chunks, 75% resolution. Your in-game changes are preserved."), 14, Ui.MUTED, false));
        }
    }

    private void filesPage() {
        Ui.section(card, Ui.t("ДАННЫЕ ИГРОКА", "PLAYER DATA"));
        File home = new File(SetupActivity.home(this));
        File[] worlds = new File(home, "saves").listFiles(f -> f.isDirectory() && new File(f, "level.dat").isFile());
        long free = new StatFs(home.getPath()).getAvailableBytes();
        card.addView(Ui.text(this, Ui.t("Миров: ", "Worlds: ") + (worlds == null ? 0 : worlds.length)
                + " · " + Ui.t("Свободно: ", "Free: ") + String.format(Locale.getDefault(), "%.1f GB", free / 1073741824.0), 17, Ui.TEXT, true));
        button(Ui.t("Открыть папку игры", "Open game folder"), false, this::openFolder);
        Ui.section(card, Ui.t("РЕЗЕРВНЫЕ КОПИИ", "BACKUPS"));
        card.addView(Ui.text(this, Ui.t(
                "Миры, скриншоты, наборы ресурсов, шейдеры, скин и настройки. Перед копированием полностью выйдите из игры. Аккаунт Microsoft и файлы игры в архив не входят.",
                "Worlds, screenshots, resource packs, shader packs, skin and settings. Fully quit the game before copying. Microsoft credentials and game resources are excluded."), 14, Ui.MUTED, false));
        button(Ui.t("Сохранить резервную копию", "Create backup"), false, () -> {
            if (!canCopy()) return;
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT).setType("application/zip").addCategory(Intent.CATEGORY_OPENABLE);
            intent.putExtra(Intent.EXTRA_TITLE, "horizon-backup-" + new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ROOT).format(new Date()) + ".zip");
            picker(intent, BACKUP);
        });
        button(Ui.t("Восстановить из ZIP", "Restore from ZIP"), false, () -> {
            if (!canCopy()) return;
            new AlertDialog.Builder(this).setTitle(Ui.t("Восстановление копии", "Restore backup"))
                    .setMessage(Ui.t("Выберите резервную копию Horizon. Совпадающие миры и наборы будут добавлены под новым именем. Существующие настройки сохранятся.",
                            "Choose a Horizon backup. Worlds and packs with matching names are added under new names. Existing settings are preserved."))
                    .setPositiveButton(Ui.t("Выбрать ZIP", "Choose ZIP"), (d, w) -> picker(new Intent(Intent.ACTION_OPEN_DOCUMENT)
                            .setType("*/*").addCategory(Intent.CATEGORY_OPENABLE), RESTORE))
                    .setNegativeButton(Ui.t("Отмена", "Cancel"), null).show();
        });
    }

    private void buildPage() {
        Ui.section(card, Ui.t("ПРИЛОЖЕНИЕ", "APPLICATION"));
        card.addView(Ui.text(this, "Horizon Android " + version(), 21, Ui.TEXT, true));
        card.addView(Ui.text(this, android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL + " · Android " + android.os.Build.VERSION.RELEASE, 14, Ui.MUTED, false));
        card.addView(Ui.text(this, Ui.t("Движок Horizon: 63eccfc · Minecraft 26.3\nARM64 · ресурсы внутри APK · поддержка страниц памяти 16 КБ",
                "Horizon engine: 63eccfc · Minecraft 26.3\nARM64 · bundled resources · 16 KiB memory pages"), 14, Ui.MUTED, false));
        Ui.section(card, Ui.t("ВХОД MICROSOFT", "MICROSOFT SIGN-IN"));
        boolean configured = !Msa.clientId(this).isEmpty();
        card.addView(Ui.text(this, configured ? Ui.t("Приложение Microsoft настроено. Войти можно в профиле.", "Microsoft app is configured. Sign in from the profile.")
                : Ui.t("Ожидается регистрация Microsoft-приложения автором сборки. ID игрового аккаунта для этого не подходит.",
                        "The build owner still needs to register a Microsoft application. A player account ID cannot be used here."), 14, configured ? Ui.MUTED : Ui.WARN, false));
        button(Ui.t("Релизы и обновления", "Releases and updates"), false, () -> {
            try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/DraimCiDo/Android-Horizon/releases"))); }
            catch (Exception e) { tell(Ui.t("Не найден браузер.", "No browser available.")); }
        });
    }

    private void button(String title, boolean primary, Runnable action) {
        Button button = Ui.button(this, title, primary);
        button.setOnClickListener(v -> action.run());
        card.addView(button, Ui.gap(this, primary ? 20 : 10));
    }

    private void openFolder() {
        Intent intent = new Intent(Intent.ACTION_VIEW, DocumentsContract.buildRootUri("dev.horizonlab.horizon.documents", "horizon"));
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try { startActivity(intent); }
        catch (Exception e) { tell(Ui.t("Откройте системное приложение «Файлы» и выберите Horizon в боковом меню.", "Open the system Files app and select Horizon in its side menu.")); }
    }

    private void picker(Intent intent, int code) {
        try { startActivityForResult(intent, code); }
        catch (Exception e) { tell(Ui.t("Системный выбор файлов недоступен.", "System file picker is unavailable.")); }
    }

    private boolean canCopy() {
        if (!HorizonActivity.active) return true;
        tell(Ui.t("Полностью выйдите из игры перед работой с резервными копиями.", "Fully quit the game before working with backups."));
        return false;
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (result != RESULT_OK) return;
        if (request == SETUP) { draw(); startActivity(new Intent(this, HorizonActivity.class)); return; }
        if ((request == BACKUP || request == RESTORE) && data != null && data.getData() != null && canCopy()) {
            if (request == BACKUP && "dev.horizonlab.horizon.documents".equals(data.getData().getAuthority())) {
                tell(Ui.t("Сохраните копию вне папки Horizon: например, в Загрузки или облако.", "Save the backup outside Horizon: for example, in Downloads or cloud storage."));
                return;
            }
            copy(request, data.getData());
        }
    }

    private void copy(int request, Uri uri) {
        busy = true;
        setControls(card, false);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        tell(Ui.t("Подготовка…", "Preparing…"));
        new Thread(() -> {
            String outcome;
            try {
                File home = new File(SetupActivity.home(this));
                UserDataArchive.Result result;
                if (request == BACKUP) {
                    try (OutputStream out = getContentResolver().openOutputStream(uri, "wt")) {
                        if (out == null) throw new IOException("Cannot open output");
                        result = UserDataArchive.backup(home, out, this::progress);
                    }
                    outcome = Ui.t("Копия сохранена. Файлов: ", "Backup saved. Files: ") + result.files;
                } else {
                    try (InputStream in = getContentResolver().openInputStream(uri)) {
                        if (in == null) throw new IOException("Cannot open input");
                        result = UserDataArchive.restore(home, in, this::progress);
                    }
                    outcome = Ui.t("Восстановлено объектов: ", "Restored items: ") + result.restored
                            + Ui.t(". Существующих настроек сохранено: ", ". Existing settings kept: ") + result.skipped;
                }
            } catch (Exception e) { outcome = Ui.t("Операция не завершена: ", "Operation did not complete: ") + e.getMessage(); }
            final String done = outcome;
            runOnUiThread(() -> {
                busy = false;
                if (isDestroyed()) return;
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                notice = done;
                draw();
            });
        }, "horizon-user-data").start();
    }

    private void progress(int files, long bytes) {
        long now = android.os.SystemClock.elapsedRealtime();
        if (now - lastProgress < 250) return;
        lastProgress = now;
        runOnUiThread(() -> { if (!isDestroyed()) tell(files + Ui.t(" файлов · ", " files · ") + (bytes >> 20) + Ui.t(" МБ", " MB")); });
    }

    private void setControls(View view, boolean enabled) {
        if (view instanceof Button) view.setEnabled(enabled);
        if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) setControls(group.getChildAt(i), enabled);
        }
    }

    private void tell(String text) { notice = text; if (message != null) message.setText(text); }
    private String version() {
        try { return getPackageManager().getPackageInfo(getPackageName(), 0).versionName; }
        catch (Exception e) { return "?"; }
    }
    @Override public void onBackPressed() {
        if (busy) { tell(Ui.t("Дождитесь завершения копирования.", "Wait for copying to finish.")); return; }
        if (!tab.equals("play")) { tab = "play"; notice = ""; draw(); } else super.onBackPressed();
    }
}

package dev.horizonlab.horizon;

import android.app.Activity;
import android.os.Bundle;
import android.widget.*;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import org.json.JSONObject;

/** Touch preferences share touch_layout.json with the native editor; custom positions survive. */
public class ControlsActivity extends Activity {
    private JSONObject layout;
    private SeekBar sensitivity, size, opacity, margin, deadzone;
    private Switch invert, passThrough;
    private Spinner preset, guiScale;
    private TextView status;
    private final int[] scales = {0, 1, 2, 3, 4, 5, 6};

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        try { layout = new JSONObject(new String(Files.readAllBytes(file().toPath()), StandardCharsets.UTF_8)); }
        catch (Exception e) { layout = new JSONObject(); }
        LinearLayout card = Ui.card(this);
        card.addView(Ui.text(this, Ui.t("Управление и интерфейс", "Controls and interface"), 25, Ui.TEXT, true));
        card.addView(Ui.text(this, Ui.t("Сохрани изменения и вернись в игру. Настройки применятся, когда отпустишь экран. Положение отдельных кнопок меняется шестерёнкой в игре.",
                "Save and return to the game. Settings apply when all fingers are lifted. Use the in-game gear to move individual buttons."), 14, Ui.MUTED, false));
        Ui.section(card, Ui.t("КАМЕРА И ДВИЖЕНИЕ", "CAMERA AND MOVEMENT"));
        sensitivity = slider(card, Ui.t("Чувствительность", "Sensitivity"), 25, 300, number("sensitivity", 1.0, 100), "%");
        invert = toggle(card, Ui.t("Инверсия камеры по вертикали", "Invert vertical camera"), layout.optBoolean("invert_y", false));
        passThrough = toggle(card, Ui.t("Двигать камеру пальцем на кнопке", "Move camera with a button finger"), layout.optBoolean("pass_through", true));
        deadzone = slider(card, Ui.t("Мёртвая зона джойстика", "Joystick deadzone"), 0, 40, number("stick_deadzone", 0.12, 100), "%");
        Ui.section(card, Ui.t("КНОПКИ", "BUTTONS"));
        String[] schemes = {Ui.t("Обычная", "Standard"), "Bedrock", Ui.t("Своя расстановка", "Custom layout")};
        preset = spinner(card, schemes);
        String value = layout.optString("preset", layout.has("controls") ? "custom" : "standard");
        preset.setSelection(value.equals("bedrock") ? 1 : value.equals("custom") ? 2 : 0);
        size = slider(card, Ui.t("Размер кнопок", "Button size"), 70, 160, number("button_scale", 1.0, 100), "%");
        opacity = slider(card, Ui.t("Непрозрачность кнопок", "Button opacity"), 20, 100, number("opacity", 0.85, 100), "%");
        margin = slider(card, Ui.t("Отступ от краёв", "Edge margin"), 0, 32, number("safe_margin", 12.0, 1), "");
        Ui.section(card, Ui.t("МЕНЮ И ИНВЕНТАРЬ", "MENUS AND INVENTORY"));
        card.addView(Ui.text(this, Ui.t("Масштаб интерфейса: большее значение увеличивает меню и слоты. Максимум зависит от разрешения экрана.",
                "Interface scale: higher values enlarge menus and slots. The maximum depends on screen resolution."), 14, Ui.MUTED, false));
        guiScale = spinner(card, new String[]{Ui.t("Автоматически", "Automatic"), "1×", "2×", "3×", "4×", "5×", "6×"});
        int current = layout.optInt("gui_scale", readGuiScale());
        for (int i = 0; i < scales.length; i++) if (scales[i] == current) guiScale.setSelection(i);
        card.addView(Ui.text(this, Ui.t("Касание слота — обычный клик. Долгое нажатие — правый клик: взять половину стопки или положить один предмет. Список миров и серверов прокручивается пальцем.",
                "Tap a slot for a normal click. Long press for a right click: take half a stack or place one item. Swipe to scroll world and server lists."), 14, Ui.MUTED, false), Ui.gap(this, 12));
        status = Ui.text(this, "", 14, Ui.WARN, false);
        card.addView(status);
        Button save = Ui.button(this, Ui.t("Сохранить", "Save"), true);
        save.setOnClickListener(v -> save());
        card.addView(save, Ui.gap(this, 14));
        Button back = Ui.button(this, Ui.t("Назад без сохранения", "Back without saving"), false);
        back.setOnClickListener(v -> finish());
        card.addView(back, Ui.gap(this, 8));
    }

    private File file() { return new File(SetupActivity.home(this), "touch_layout.json"); }
    private int number(String key, double fallback, int multiplier) { return (int)Math.round(layout.optDouble(key, fallback) * multiplier); }
    private int readGuiScale() {
        try {
            for (String line : Files.readAllLines(new File(SetupActivity.home(this), "options.txt").toPath(), StandardCharsets.UTF_8)) {
                if (line.startsWith("guiScale:")) return Integer.parseInt(line.substring(9).trim());
            }
        } catch (Exception ignored) {}
        return 0;
    }
    private Spinner spinner(LinearLayout card, String[] labels) {
        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, labels) {
            @Override public android.view.View getView(int position, android.view.View view, android.view.ViewGroup parent) {
                TextView text = (TextView)super.getView(position, view, parent);
                text.setTextColor(Ui.TEXT); text.setTextSize(16); return text;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        card.addView(spinner, Ui.gap(this, 8));
        return spinner;
    }
    private Switch toggle(LinearLayout card, String title, boolean value) {
        Switch control = new Switch(this);
        control.setText(title); control.setTextColor(Ui.TEXT); control.setChecked(value);
        control.setPadding(0, Ui.dp(this, 10), 0, Ui.dp(this, 10));
        card.addView(control, Ui.gap(this, 8));
        return control;
    }
    private SeekBar slider(LinearLayout card, String title, int min, int max, int value, String suffix) {
        TextView label = Ui.text(this, "", 15, Ui.TEXT, false);
        card.addView(label, Ui.gap(this, 10));
        SeekBar bar = new SeekBar(this);
        bar.setMax(max - min); bar.setProgress(Math.max(0, Math.min(max - min, value - min)));
        label.setText(title + ": " + (bar.getProgress() + min) + suffix);
        bar.setTag(min);
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar b, int n, boolean user) { label.setText(title + ": " + (n + min) + suffix); }
            public void onStartTrackingTouch(SeekBar b) {}
            public void onStopTrackingTouch(SeekBar b) {}
        });
        bar.setContentDescription(title);
        card.addView(bar, new LinearLayout.LayoutParams(-1, Ui.dp(this, 44)));
        return bar;
    }
    private int value(SeekBar bar) { return bar.getProgress() + (Integer)bar.getTag(); }
    private void save() {
        try {
            if (!layout.has("controls")) layout.put("controls", new JSONObject());
            layout.put("preset", new String[]{"standard", "bedrock", "custom"}[preset.getSelectedItemPosition()]);
            layout.put("sensitivity", value(sensitivity) / 100.0).put("invert_y", invert.isChecked());
            layout.put("pass_through", passThrough.isChecked()).put("stick_deadzone", value(deadzone) / 100.0);
            layout.put("button_scale", value(size) / 100.0).put("opacity", value(opacity) / 100.0);
            layout.put("safe_margin", value(margin)).put("gui_scale", scales[guiScale.getSelectedItemPosition()]);
            File temp = new File(file().getParentFile(), "touch_layout.json.tmp");
            Files.write(temp.toPath(), layout.toString(2).getBytes(StandardCharsets.UTF_8));
            Files.move(temp.toPath(), file().toPath(), StandardCopyOption.REPLACE_EXISTING);
            finish();
        } catch (Exception e) { status.setText(Ui.t("Не удалось сохранить: ", "Could not save: ") + e.getMessage()); }
    }
}

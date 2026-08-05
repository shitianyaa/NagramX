package org.telegram.ui;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.graphics.ColorUtils;
import androidx.core.widget.NestedScrollView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.NumberPicker;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Compact bottom sheet for video sleep timer choices.
 *
 * The sheet owns only the selection UI. PhotoViewer keeps the playback state so
 * a timer can be transferred to MediaController without duplicating lifecycle
 * logic here.
 */
public class VideoSleepTimerSheet extends BottomSheet {

    public static final int MODE_OFF = 0;
    public static final int MODE_DURATION = 1;
    public static final int MODE_AFTER_CURRENT = 2;

    private static final int[] PRESET_MINUTES = {10, 20, 30, 60, 90};
    private static final int MAX_CUSTOM_MINUTES = 12 * 60;

    private final Callback callback;
    private final int currentMode;
    private final int selectedMinutes;
    private final ArrayList<TextView> presetViews = new ArrayList<>();

    private TextView statusTextView;
    private TextView afterCurrentRow;
    private TextView customRow;
    private LinearLayout customPanel;
    private NumberPicker hoursPicker;
    private NumberPicker minutesPicker;
    private TextView customApplyButton;

    public VideoSleepTimerSheet(Context context, Theme.ResourcesProvider resourcesProvider, int mode, int minutes, Callback callback) {
        super(context, false, resourcesProvider);
        this.callback = callback;
        currentMode = mode;
        selectedMinutes = Math.max(0, Math.min(MAX_CUSTOM_MINUTES, minutes));
        fixNavigationBar();

        NestedScrollView scrollView = new NestedScrollView(context);
        scrollView.setFillViewport(true);
        scrollView.setClipToPadding(false);
        scrollView.setVerticalScrollBarEnabled(false);

        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(0, 0, 0, dp(8));
        scrollView.addView(content, new NestedScrollView.LayoutParams(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        addHandle(context, content);
        addHeader(context, content);

        statusTextView = new TextView(context);
        statusTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        statusTextView.setTextColor(getThemedColor(Theme.key_dialogTextGray2));
        statusTextView.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
        statusTextView.setMaxLines(2);
        statusTextView.setEllipsize(TextUtils.TruncateAt.END);
        content.addView(statusTextView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 20, 0, 20, 10));

        TextView quickTitle = createSectionTitle(context, LocaleController.getString(R.string.VideoSleepTimerQuickChoices));
        content.addView(quickTitle, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 20, 0, 20, 4));

        HorizontalScrollView presetsScrollView = new HorizontalScrollView(context);
        presetsScrollView.setHorizontalScrollBarEnabled(false);
        LinearLayout presetsLayout = new LinearLayout(context);
        presetsLayout.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
        presetsLayout.setPadding(dp(16), 0, dp(16), 0);
        presetsScrollView.addView(presetsLayout, new HorizontalScrollView.LayoutParams(LayoutHelper.WRAP_CONTENT, dp(48)));
        content.addView(presetsScrollView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 48));

        for (int presetMinute : PRESET_MINUTES) {
            TextView presetView = createPresetView(context, presetMinute);
            presetViews.add(presetView);
            presetsLayout.addView(presetView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, 44, Gravity.CENTER_VERTICAL, 4, 0, 4, 0));
        }

        afterCurrentRow = createActionRow(context, LocaleController.getString(R.string.VideoSleepTimerAfterCurrent), () -> select(MODE_AFTER_CURRENT, 0));
        content.addView(afterCurrentRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 52, Gravity.CENTER_HORIZONTAL, 12, 4, 12, 0));

        customRow = createActionRow(context, LocaleController.getString(R.string.VideoSleepTimerCustom), this::showCustomPicker);
        content.addView(customRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 52, Gravity.CENTER_HORIZONTAL, 12, 0, 12, 0));

        customPanel = createCustomPanel(context);
        customPanel.setVisibility(isCustomSelection() ? View.VISIBLE : View.GONE);
        content.addView(customPanel, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 20, 4, 20, 0));

        TextView cancelRow = createActionRow(context, LocaleController.getString(R.string.VideoSleepTimerCancel), () -> select(MODE_OFF, 0));
        cancelRow.setTextColor(getThemedColor(Theme.key_dialogTextBlue2));
        cancelRow.setVisibility(mode == MODE_OFF ? View.GONE : View.VISIBLE);
        content.addView(cancelRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 52, Gravity.CENTER_HORIZONTAL, 12, 4, 12, 0));

        setCustomView(scrollView);
        updateState();
    }

    private void addHandle(Context context, LinearLayout parent) {
        FrameLayout handleContainer = new FrameLayout(context);
        View handle = new View(context);
        handle.setBackground(Theme.createRoundRectDrawable(dp(2), ColorUtils.setAlphaComponent(getThemedColor(Theme.key_dialogTextGray2), 110)));
        handleContainer.addView(handle, LayoutHelper.createFrame(36, 4, Gravity.CENTER, 0, 8, 0, 0));
        parent.addView(handleContainer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 24));
    }

    private void addHeader(Context context, LinearLayout parent) {
        FrameLayout header = new FrameLayout(context);

        TextView title = new TextView(context);
        title.setText(LocaleController.getString(R.string.VideoSleepTimer));
        title.setTextColor(getThemedColor(Theme.key_dialogTextBlack));
        title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
        title.setTypeface(AndroidUtilities.bold());
        title.setGravity(Gravity.CENTER_VERTICAL | (LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT));
        header.addView(title, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 52, Gravity.TOP | Gravity.LEFT, 20, 0, 52, 0));

        ImageView closeButton = new ImageView(context);
        closeButton.setImageResource(R.drawable.msg_close);
        closeButton.setColorFilter(new PorterDuffColorFilter(getThemedColor(Theme.key_dialogTextGray2), PorterDuff.Mode.SRC_IN));
        closeButton.setScaleType(ImageView.ScaleType.CENTER);
        closeButton.setContentDescription(LocaleController.getString(R.string.Close));
        closeButton.setBackground(Theme.createSelectorDrawable(getThemedColor(Theme.key_dialogButtonSelector), 1, dp(18)));
        closeButton.setOnClickListener(v -> dismiss());
        header.addView(closeButton, LayoutHelper.createFrame(48, 48, Gravity.TOP | Gravity.RIGHT, 0, 2, 12, 0));

        parent.addView(header, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 56));
    }

    private TextView createSectionTitle(Context context, CharSequence text) {
        TextView title = new TextView(context);
        title.setText(text);
        title.setTextColor(getThemedColor(Theme.key_dialogTextGray2));
        title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        title.setTypeface(AndroidUtilities.bold());
        title.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
        return title;
    }

    private TextView createPresetView(Context context, int minutes) {
        TextView view = new TextView(context);
        view.setText(LocaleController.formatPluralString("Minutes", minutes));
        view.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        view.setGravity(Gravity.CENTER);
        view.setSingleLine(true);
        view.setPadding(dp(14), 0, dp(14), 0);
        view.setMinWidth(dp(64));
        view.setContentDescription(view.getText());
        view.setOnClickListener(v -> select(MODE_DURATION, minutes));
        return view;
    }

    private TextView createActionRow(Context context, CharSequence text, Runnable action) {
        TextView row = new TextView(context);
        row.setText(text);
        row.setTextColor(getThemedColor(Theme.key_dialogTextBlack));
        row.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        row.setGravity(Gravity.CENTER_VERTICAL | (LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT));
        row.setPadding(dp(16), 0, dp(16), 0);
        row.setBackground(Theme.createSelectorDrawable(getThemedColor(Theme.key_dialogButtonSelector), 1, dp(10)));
        row.setOnClickListener(v -> action.run());
        return row;
    }

    private LinearLayout createCustomPanel(Context context) {
        LinearLayout panel = new LinearLayout(context);
        panel.setOrientation(LinearLayout.VERTICAL);

        View divider = new View(context);
        divider.setBackgroundColor(ColorUtils.setAlphaComponent(getThemedColor(Theme.key_divider), 150));
        panel.addView(divider, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 1, Gravity.CENTER_HORIZONTAL, 0, 0, 0, 8));

        TextView hint = new TextView(context);
        hint.setText(LocaleController.getString(R.string.VideoSleepTimerCustomHint));
        hint.setTextColor(getThemedColor(Theme.key_dialogTextGray2));
        hint.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        hint.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
        panel.addView(hint, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 0, 0, 0, 4));

        LinearLayout pickers = new LinearLayout(context);
        pickers.setOrientation(LinearLayout.HORIZONTAL);
        pickers.setGravity(Gravity.CENTER);
        panel.addView(pickers, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 174));

        hoursPicker = createPicker(context, 0, 12, LocaleController.getString(R.string.VideoSleepTimerHours));
        minutesPicker = createPicker(context, 0, 59, LocaleController.getString(R.string.VideoSleepTimerMinutes));
        pickers.addView(createPickerColumn(context, hoursPicker, LocaleController.getString(R.string.VideoSleepTimerHours)), LayoutHelper.createLinear(0, LayoutHelper.MATCH_PARENT, 1, Gravity.CENTER));
        pickers.addView(createPickerColumn(context, minutesPicker, LocaleController.getString(R.string.VideoSleepTimerMinutes)), LayoutHelper.createLinear(0, LayoutHelper.MATCH_PARENT, 1, Gravity.CENTER));

        customApplyButton = new TextView(context);
        customApplyButton.setText(LocaleController.getString(R.string.VideoSleepTimerApply));
        customApplyButton.setTextColor(getThemedColor(Theme.key_featuredStickers_buttonText));
        customApplyButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        customApplyButton.setTypeface(AndroidUtilities.bold());
        customApplyButton.setGravity(Gravity.CENTER);
        customApplyButton.setBackground(Theme.AdaptiveRipple.filledRectByKey(Theme.key_featuredStickers_addButton, 8));
        customApplyButton.setOnClickListener(v -> {
            int totalMinutes = hoursPicker.getValue() * 60 + minutesPicker.getValue();
            if (totalMinutes > 0) {
                select(MODE_DURATION, totalMinutes);
            }
        });
        panel.addView(customApplyButton, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 48, Gravity.CENTER_HORIZONTAL, 0, 0, 0, 8));

        if (selectedMinutes > 0) {
            int hours = Math.min(12, selectedMinutes / 60);
            int minutes = selectedMinutes > MAX_CUSTOM_MINUTES ? 0 : selectedMinutes % 60;
            hoursPicker.setValue(hours);
            minutesPicker.setValue(minutes);
        }
        hoursPicker.setOnValueChangedListener((picker, oldVal, newVal) -> {
            updateMinutesPickerLimit(newVal);
            updateCustomApplyState();
        });
        minutesPicker.setOnValueChangedListener((picker, oldVal, newVal) -> updateCustomApplyState());
        updateMinutesPickerLimit(hoursPicker.getValue());
        updateCustomApplyState();
        return panel;
    }

    private void updateMinutesPickerLimit(int hours) {
        if (minutesPicker == null) {
            return;
        }
        int maxMinutes = hours >= MAX_CUSTOM_MINUTES / 60 ? MAX_CUSTOM_MINUTES % 60 : 59;
        minutesPicker.setMaxValue(maxMinutes);
    }

    private NumberPicker createPicker(Context context, int min, int max, String label) {
        NumberPicker picker = new NumberPicker(context, 20, resourcesProvider);
        picker.setItemCount(3);
        picker.setMinValue(min);
        picker.setMaxValue(max);
        picker.setWrapSelectorWheel(false);
        picker.setTextColor(getThemedColor(Theme.key_dialogTextBlack));
        picker.setSelectorColor(ColorUtils.setAlphaComponent(getThemedColor(Theme.key_dialogTextBlue2), 80));
        picker.setFormatter(value -> String.format(Locale.US, "%02d", value));
        picker.setContentDescription(label);
        return picker;
    }

    private FrameLayout createPickerColumn(Context context, NumberPicker picker, CharSequence label) {
        FrameLayout column = new FrameLayout(context);
        column.addView(picker, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 150, Gravity.TOP | Gravity.CENTER_HORIZONTAL));

        TextView labelView = new TextView(context);
        labelView.setText(label);
        labelView.setTextColor(getThemedColor(Theme.key_dialogTextGray2));
        labelView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        labelView.setGravity(Gravity.CENTER);
        column.addView(labelView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 24, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL));
        return column;
    }

    private boolean isPresetMinutes(int minutes) {
        for (int preset : PRESET_MINUTES) {
            if (preset == minutes) {
                return true;
            }
        }
        return false;
    }

    private boolean isCustomSelection() {
        return currentMode == MODE_DURATION && selectedMinutes > 0 && !isPresetMinutes(selectedMinutes);
    }

    private void showCustomPicker() {
        customPanel.setVisibility(View.VISIBLE);
        customRow.setTextColor(getThemedColor(Theme.key_dialogTextBlue2));
        updateCustomApplyState();
        customPanel.requestLayout();
    }

    private void select(int mode, int minutes) {
        if (callback != null) {
            callback.onTimerSelected(mode, minutes);
        }
        dismiss();
    }

    private void updateState() {
        if (currentMode == MODE_DURATION && selectedMinutes > 0) {
            statusTextView.setText(LocaleController.formatString(R.string.VideoSleepTimerSetFor, formatDuration(selectedMinutes)));
        } else if (currentMode == MODE_AFTER_CURRENT) {
            statusTextView.setText(LocaleController.getString(R.string.VideoSleepTimerAfterCurrentSet));
        } else {
            statusTextView.setText(LocaleController.getString(R.string.VideoSleepTimerChoose));
        }

        int accent = getThemedColor(Theme.key_dialogTextBlue2);
        for (int i = 0; i < presetViews.size(); i++) {
            boolean selected = currentMode == MODE_DURATION && PRESET_MINUTES[i] == selectedMinutes;
            TextView presetView = presetViews.get(i);
            presetView.setTextColor(selected ? accent : getThemedColor(Theme.key_dialogTextBlack));
            presetView.setBackground(Theme.createSimpleSelectorRoundRectDrawable(dp(22),
                    selected ? ColorUtils.setAlphaComponent(accent, 42) : getThemedColor(Theme.key_dialogButtonSelector),
                    ColorUtils.setAlphaComponent(accent, 48)));
        }
        afterCurrentRow.setTextColor(currentMode == MODE_AFTER_CURRENT ? accent : getThemedColor(Theme.key_dialogTextBlack));
        customRow.setTextColor(currentMode == MODE_DURATION && !isPresetMinutes(selectedMinutes) ? accent : getThemedColor(Theme.key_dialogTextBlack));
    }

    private void updateCustomApplyState() {
        if (customApplyButton == null || hoursPicker == null || minutesPicker == null) {
            return;
        }
        boolean enabled = hoursPicker.getValue() * 60 + minutesPicker.getValue() > 0;
        customApplyButton.setEnabled(enabled);
        customApplyButton.setAlpha(enabled ? 1f : .5f);
    }

    private String formatDuration(int minutes) {
        int hours = minutes / 60;
        int remainingMinutes = minutes % 60;
        if (hours > 0 && remainingMinutes > 0) {
            return LocaleController.formatString(R.string.VideoSleepTimerDurationHoursMinutes, hours, remainingMinutes);
        } else if (hours > 0) {
            return LocaleController.formatString(R.string.VideoSleepTimerDurationHours, hours);
        } else {
            return LocaleController.formatString(R.string.VideoSleepTimerDurationMinutes, remainingMinutes);
        }
    }

    public interface Callback {
        void onTimerSelected(int mode, int minutes);
    }
}

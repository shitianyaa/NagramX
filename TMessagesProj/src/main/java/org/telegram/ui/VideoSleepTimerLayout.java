package org.telegram.ui;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.content.Context;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.ActionBarMenuSubItem;
import org.telegram.ui.ActionBar.ActionBarPopupWindow;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.PopupSwipeBackLayout;

public class VideoSleepTimerLayout {

    public static final int MODE_OFF = 0;
    public static final int MODE_DURATION = 1;
    public static final int MODE_AFTER_CURRENT = 2;

    private static final int[] PRESET_MINUTES = {10, 20, 30, 60, 90};

    public final ActionBarPopupWindow.ActionBarPopupWindowLayout layout;

    private final ActionBarMenuSubItem offItem;
    private final ActionBarMenuSubItem afterCurrentItem;
    private final ActionBarMenuSubItem[] durationItems = new ActionBarMenuSubItem[PRESET_MINUTES.length];

    public VideoSleepTimerLayout(Context context, PopupSwipeBackLayout swipeBackLayout, Callback callback) {
        layout = new ActionBarPopupWindow.ActionBarPopupWindowLayout(context, 0, null);
        layout.setFitItems(true);

        ActionBarMenuSubItem backItem = ActionBarMenuItem.addItem(layout, R.drawable.msg_arrow_back, LocaleController.getString(R.string.Back), false, null);
        backItem.setColors(0xfffafafa, 0xfffafafa);
        backItem.setSelectorColor(0x0fffffff);
        backItem.setOnClickListener(view -> swipeBackLayout.closeForeground());

        FrameLayout gap = new FrameLayout(context);
        gap.setMinimumWidth(dp(196));
        gap.setBackgroundColor(0xff181818);
        layout.addView(gap);
        LinearLayout.LayoutParams gapParams = (LinearLayout.LayoutParams) gap.getLayoutParams();
        if (LocaleController.isRTL) {
            gapParams.gravity = Gravity.RIGHT;
        }
        gapParams.width = LayoutHelper.MATCH_PARENT;
        gapParams.height = dp(8);
        gap.setLayoutParams(gapParams);

        offItem = addOption(layout, LocaleController.getString(R.string.VideoSleepTimerOff), () -> callback.onTimerSelected(MODE_OFF, 0));
        afterCurrentItem = addOption(layout, LocaleController.getString(R.string.VideoSleepTimerAfterCurrent), () -> callback.onTimerSelected(MODE_AFTER_CURRENT, 0));

        for (int i = 0; i < PRESET_MINUTES.length; i++) {
            final int minutes = PRESET_MINUTES[i];
            durationItems[i] = addOption(layout, LocaleController.formatPluralString("Minutes", minutes), () -> callback.onTimerSelected(MODE_DURATION, minutes));
        }
    }

    private ActionBarMenuSubItem addOption(ViewGroup parent, CharSequence text, Runnable onClick) {
        ActionBarMenuSubItem item = ActionBarMenuItem.addItem(parent, 0, text, true, null);
        item.setColors(0xfffafafa, 0xfffafafa);
        item.setSelectorColor(0x0fffffff);
        item.setOnClickListener(view -> onClick.run());
        return item;
    }

    public void update(int mode, int selectedMinutes) {
        offItem.setChecked(mode == MODE_OFF);
        afterCurrentItem.setChecked(mode == MODE_AFTER_CURRENT);
        for (int i = 0; i < durationItems.length; i++) {
            durationItems[i].setChecked(mode == MODE_DURATION && PRESET_MINUTES[i] == selectedMinutes);
        }
    }

    public interface Callback {
        void onTimerSelected(int mode, int minutes);
    }
}

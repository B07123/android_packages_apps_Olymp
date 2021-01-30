/*
 * Copyright (C) 2020 ZenX-OS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.zen.hub.fragments;

import java.util.ArrayList;

import android.os.Bundle;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.UserHandle;
import androidx.preference.*;
import androidx.preference.Preference.OnPreferenceChangeListener;
import android.provider.Settings;
import android.content.ContentResolver;
import android.content.Context;

import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import com.android.internal.util.zenx.ThemesUtils;
import android.content.om.IOverlayManager;
import android.content.SharedPreferences;
import android.os.ServiceManager;
import android.content.om.OverlayInfo;
import android.graphics.Color;
import android.os.SystemProperties;
import android.os.RemoteException;
import android.app.UiModeManager;

import com.android.settings.dashboard.DashboardFragment;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;

import static com.zen.hub.utils.Utils.handleOverlays;
import com.zenx.support.preferences.SystemSettingListPreference;
import com.zenx.support.colorpicker.ColorPickerPreference;
import com.android.internal.util.zenx.ZenxUtils;

import lineageos.hardware.LineageHardwareManager;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settings.display.NightModePreferenceController;
import com.android.settings.display.ThemePreferenceController;
import com.android.settings.development.OverlayCategoryPreferenceController;
import com.android.settings.development.EnableBlursPreferenceController;
import com.zenx.support.preferences.CustomSeekBarPreference;
import com.zenx.support.preferences.SystemSettingSwitchPreference;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;

public class ThemingSettings extends DashboardFragment implements OnPreferenceChangeListener {
    private static final String TAG = "ThemingSettings";
    private static final String UI_STYLE = "ui_style";
    private static final String PREF_RGB_ACCENT_PICKER = "rgb_accent_picker";
    private static final String ACCENT_COLOR_PROP = "persist.sys.theme.accentcolor";
    private static final String PREF_THEME_ACCENT_COLOR = "theme_accent_color";
    private static final String ACCENT_PRESET = "accent_preset";
    private static final String RANDOM_ACCENT_COLOR_SCREENOFF_DURATION = "random_accent_color_screenoff_duration";
    private static final String RANDOM_ACCENT_COLOR_ON_SCREEN_OFF = "random_accent_color_on_screen_off";

    private ColorPickerPreference rgbAccentPicker;
    private ListPreference mAccentPreset;
    private CustomSeekBarPreference mAccentRandomColorDurationScreenOff;
    private SystemSettingSwitchPreference mAccentRandomColorOnScreenOff;

    private IOverlayManager mOverlayManager;
    private UiModeManager mUiModeManager;
    private SharedPreferences mSharedPreferences;
    private ListPreference mUIStyle;

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.zen_hub_theming;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return buildPreferenceControllers(context, getSettingsLifecycle());
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(
            Context context, Lifecycle lifecycle) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        controllers.add(new NightModePreferenceController(context));
        controllers.add(new ThemePreferenceController(context));
        controllers.add(new OverlayCategoryPreferenceController(context,
                "android.theme.customization.font"));
        controllers.add(new OverlayCategoryPreferenceController(context,
                "android.theme.customization.adaptive_icon_shape"));
        controllers.add(new OverlayCategoryPreferenceController(context,
                "android.theme.customization.icon_pack.android"));
        controllers.add(new EnableBlursPreferenceController(context));
        return controllers;
    }


    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mUiModeManager = getContext().getSystemService(UiModeManager.class);
        mOverlayManager = IOverlayManager.Stub.asInterface(
                ServiceManager.getService(Context.OVERLAY_SERVICE));
        mUIStyle = (ListPreference) findPreference(UI_STYLE);
        int UIStyle = Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.UI_STYLE, 0);
        int UIStyleValue = getOverlayPosition(ThemesUtils.UI_THEMES);
        if (UIStyleValue != 0) {
            mUIStyle.setValue(String.valueOf(UIStyle));
        }
        mUIStyle.setSummary(mUIStyle.getEntry());
        mUIStyle.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (preference == mUIStyle) {
                    String value = (String) newValue;
                    Settings.System.putInt(getActivity().getContentResolver(), Settings.System.UI_STYLE, Integer.valueOf(value));
                    int valueIndex = mUIStyle.findIndexOfValue(value);
                    mUIStyle.setSummary(mUIStyle.getEntries()[valueIndex]);
                    String overlayName = getOverlayName(ThemesUtils.UI_THEMES);
                    if (overlayName != null) {
                    handleOverlays(overlayName, false, mOverlayManager);
                    }
                    if (valueIndex > 0) {
                        handleOverlays(ThemesUtils.UI_THEMES[valueIndex],
                                true, mOverlayManager);
                    }
                    return true;
                }
                return false;
            }
       });

        rgbAccentPicker = (ColorPickerPreference) findPreference(PREF_RGB_ACCENT_PICKER);
        String colorVal = SystemProperties.get(ACCENT_COLOR_PROP, "-1");
        int color = "-1".equals(colorVal)
                ? Color.WHITE
                : isRandomAccentColorScreenOffIsActive() ? Color.WHITE : Color.parseColor("#" + colorVal);
        rgbAccentPicker.setNewPreviewColor(color);
        rgbAccentPicker.setOnPreferenceChangeListener(this);

        mAccentPreset = (ListPreference) findPreference(ACCENT_PRESET);
        mAccentPreset.setOnPreferenceChangeListener(this);

        mAccentRandomColorOnScreenOff = (SystemSettingSwitchPreference) findPreference(
                RANDOM_ACCENT_COLOR_ON_SCREEN_OFF);
        mAccentRandomColorOnScreenOff.setOnPreferenceChangeListener(this);

        mAccentRandomColorDurationScreenOff = (CustomSeekBarPreference) findPreference(RANDOM_ACCENT_COLOR_SCREENOFF_DURATION);
        int offd = Settings.System.getIntForUser(getActivity().getContentResolver(),
                Settings.System.RANDOM_ACCENT_COLOR_SCREENOFF_DURATION, 10, UserHandle.USER_CURRENT);
        mAccentRandomColorDurationScreenOff.setValue(offd);
        mAccentRandomColorDurationScreenOff.setOnPreferenceChangeListener(this);

        randomColorPreferenceHandler();
        checkColorPreset(colorVal);

    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
         if (preference == rgbAccentPicker) {
            int color = (Integer) newValue;
            String hexColor = String.format("%08X", (0xFFFFFFFF & color));
            SystemProperties.set(ACCENT_COLOR_PROP, hexColor);
            checkColorPreset(hexColor);
            try {
                mOverlayManager.reloadAndroidAssets(UserHandle.USER_CURRENT);
                mOverlayManager.reloadAssets("com.android.settings", UserHandle.USER_CURRENT);
                mOverlayManager.reloadAssets("com.android.systemui", UserHandle.USER_CURRENT);
            } catch (RemoteException ignored) { }

            } else if (preference == mAccentPreset) {
            String value = (String) newValue;
            int index = mAccentPreset.findIndexOfValue(value);
            mAccentPreset.setSummary(mAccentPreset.getEntries()[index]);
            SystemProperties.set(ACCENT_COLOR_PROP, value);
            try {
                mOverlayManager.reloadAndroidAssets(UserHandle.USER_CURRENT);
                mOverlayManager.reloadAssets("com.android.settings", UserHandle.USER_CURRENT);
                mOverlayManager.reloadAssets("com.android.systemui", UserHandle.USER_CURRENT);
            } catch (RemoteException ignored) { }
        } else if (preference == mAccentRandomColorDurationScreenOff) {
            int delay = (Integer) newValue;
            Settings.System.putIntForUser(getActivity().getContentResolver(),
                    Settings.System.RANDOM_ACCENT_COLOR_SCREENOFF_DURATION, delay, UserHandle.USER_CURRENT);
            randomColorPreferenceHandler();
            return true;
        } else if (preference == mAccentRandomColorOnScreenOff) {
            boolean value = (Boolean) newValue;
            Settings.System.putIntForUser(getActivity().getContentResolver(),
                    Settings.System.RANDOM_ACCENT_COLOR_ON_SCREEN_OFF, value ? 1 : 0, UserHandle.USER_CURRENT);
            Intent intent = new Intent("android.intent.action.RANDOM_COLOR_SERVICE_UPDATE");
            getContext().sendBroadcast(intent);
            randomColorPreferenceHandler();
            return true;
        }
        return false;
    }

    private boolean isRandomAccentColorScreenOffIsActive() {
        return Settings.System.getInt(getContentResolver(),
                Settings.System.RANDOM_ACCENT_COLOR_ON_SCREEN_OFF, 0) == 1;
    }

    private void randomColorPreferenceHandler() {
        if(isRandomAccentColorScreenOffIsActive()) {
            mAccentRandomColorDurationScreenOff.setVisible(true);
            mAccentPreset.setVisible(false);
            rgbAccentPicker.setVisible(false);
        } else {
            mAccentRandomColorDurationScreenOff.setVisible(false);
            mAccentPreset.setVisible(true);
            rgbAccentPicker.setVisible(true);
        }
    }


    private void checkColorPreset(String colorValue) {
        List<String> colorPresets = Arrays.asList(
                getResources().getStringArray(R.array.accent_presets_values));
        if (colorPresets.contains(colorValue)) {
            mAccentPreset.setValue(colorValue);
            int index = mAccentPreset.findIndexOfValue(colorValue);
            mAccentPreset.setSummary(mAccentPreset.getEntries()[index]);
        }
        else {
            mAccentPreset.setSummary(
                    getResources().getString(R.string.custom_string));
        }
    }


    private String getOverlayName(String[] overlays) {
            String overlayName = null;
            for (int i = 0; i < overlays.length; i++) {
                String overlay = overlays[i];
                if (ZenxUtils.isThemeEnabled(overlay)) {
                    overlayName = overlay;
                }
            }
            return overlayName;
        }

    private int getOverlayPosition(String[] overlays) {
            int position = -1;
            for (int i = 0; i < overlays.length; i++) {
                String overlay = overlays[i];
                if (ZenxUtils.isThemeEnabled(overlay)) {
                    position = i;
                }
            }
        return position;
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.ZENX_SETTINGS;
    }
}

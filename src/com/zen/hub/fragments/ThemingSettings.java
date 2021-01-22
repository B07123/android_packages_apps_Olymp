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
import com.zen.hub.fragments.subs.service.RandomColorUtils;

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
    private static final String ACCENT_RANDOM_COLOR_DURATION = "accent_random_color_duration";
    private static final String ACCENT_RANDOM_COLOR_DURATION_SCREENOFF = "accent_random_color_duration_screenoff";
    private static final String ACCENT_RANDOM_COLOR = "accent_random_color";
    private static final String CHANGE_ACCENT_COLOR_ON_SCREEN_OFF = "change_accent_color_on_screen_off";
    private static final String ACCENT_RANDOM_UNIT = "accent_random_unit";
    private static final String RANDOM_COLOR_FOOTER = "random_color_footer";

    private ColorPickerPreference rgbAccentPicker;
    private ListPreference mAccentPreset;
    private SystemSettingSwitchPreference mAccentRandomColor;
    private CustomSeekBarPreference mAccentRandomColorDuration;
    private CustomSeekBarPreference mAccentRandomColorDurationScreenOff;
    private SystemSettingListPreference mAccentRandomColorUnit;
    private SystemSettingSwitchPreference mAccentRandomColorOnScreenOff;
    private Preference mFooterPref;

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
                : isRandomAccentColorIsActive() ? Color.WHITE : Color.parseColor("#" + colorVal);
        rgbAccentPicker.setNewPreviewColor(color);
        rgbAccentPicker.setOnPreferenceChangeListener(this);

        mAccentPreset = (ListPreference) findPreference(ACCENT_PRESET);
        mAccentPreset.setOnPreferenceChangeListener(this);

        mAccentRandomColor = (SystemSettingSwitchPreference) findPreference(ACCENT_RANDOM_COLOR);
        mAccentRandomColor.setChecked(Settings.System.getInt(getContentResolver(),
            Settings.System.ACCENT_RANDOM_COLOR, 0) == 1);
        mAccentRandomColor.setOnPreferenceChangeListener(this);

        mAccentRandomColorOnScreenOff = (SystemSettingSwitchPreference) findPreference(CHANGE_ACCENT_COLOR_ON_SCREEN_OFF);
        mAccentRandomColorOnScreenOff.setChecked(Settings.System.getInt(getContentResolver(),
            Settings.System.CHANGE_ACCENT_COLOR_ON_SCREEN_OFF, 1) == 1);
        mAccentRandomColorOnScreenOff.setOnPreferenceChangeListener(this);

        mAccentRandomColorDuration = (CustomSeekBarPreference) findPreference(ACCENT_RANDOM_COLOR_DURATION);
        int time = Settings.System.getInt(getContentResolver(),
                Settings.System.ACCENT_RANDOM_COLOR_DURATION, 1);
        mAccentRandomColorDuration.setValue(time);
        mAccentRandomColorDuration.setOnPreferenceChangeListener(this);

        mAccentRandomColorDurationScreenOff = (CustomSeekBarPreference) findPreference(ACCENT_RANDOM_COLOR_DURATION_SCREENOFF);
        int timeScreenOff = Settings.System.getInt(getContentResolver(),
                Settings.System.ACCENT_RANDOM_COLOR_DURATION_SCREENOFF, 30);
        mAccentRandomColorDurationScreenOff.setValue(timeScreenOff);
        mAccentRandomColorDurationScreenOff.setOnPreferenceChangeListener(this);

        mAccentRandomColorUnit = (SystemSettingListPreference) findPreference(ACCENT_RANDOM_UNIT);
        mAccentRandomColorUnit.setOnPreferenceChangeListener(this);

        mFooterPref = findPreference(RANDOM_COLOR_FOOTER);
        mFooterPref.setTitle(R.string.random_color_attention_summary);

        mFooterPref.setVisible(false);

        if(getRandomAccentColorUnit() == 2) {
            mFooterPref.setVisible(true);
        }
        handleSeekbarValues(getRandomAccentColorUnit());
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
        } else if (preference == mAccentRandomColorDuration) {
            int time = (Integer) newValue;
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.ACCENT_RANDOM_COLOR_DURATION, time);
            randomColorPreferenceHandler();
            RandomColorUtils.restartService(getContext());
            return true;
        } else if (preference == mAccentRandomColorDurationScreenOff) {
            int time = (Integer) newValue;
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.ACCENT_RANDOM_COLOR_DURATION_SCREENOFF, time);
            randomColorPreferenceHandler();
            RandomColorUtils.restartService(getContext());
            return true;
        } else if (preference == mAccentRandomColor) {
            int val = ((Boolean) newValue) ? 1 : 0;
            Settings.System.putInt(getContentResolver(), Settings.System.ACCENT_RANDOM_COLOR, val);
            randomColorPreferenceHandler();
            RandomColorUtils.enableService(getContext());
            return true;
        } else if (preference == mAccentRandomColorOnScreenOff) {
            int val = ((Boolean) newValue) ? 1 : 0;
            Settings.System.putInt(getContentResolver(), Settings.System.CHANGE_ACCENT_COLOR_ON_SCREEN_OFF, val);
            randomColorPreferenceHandler();
            RandomColorUtils.restartService(getContext());
            return true;
        } else if (preference == mAccentRandomColorUnit) {
            int val = Integer.parseInt((String) newValue);
            int index = mAccentRandomColorUnit.findIndexOfValue((String) newValue);
            Settings.System.putInt(getActivity().getContentResolver(),
                Settings.System.ACCENT_RANDOM_UNIT, val);
            mAccentRandomColorUnit.setSummary(mAccentRandomColorUnit.getEntries()[index]);
            handleSeekbarValues(index);
            randomColorPreferenceHandler();
            RandomColorUtils.restartService(getContext());
            return true;
        }
        return false;
    }

    private boolean isRandomAccentColorIsActive() {
        return Settings.System.getInt(getContentResolver(),
                Settings.System.ACCENT_RANDOM_COLOR, 0) == 1;
    }

    private boolean isRandomAccentColorScreenOffIsActive() {
        return Settings.System.getInt(getContentResolver(),
                Settings.System.CHANGE_ACCENT_COLOR_ON_SCREEN_OFF, 0) == 1;
    }

    private int getRandomAccentColorUnit() {
        return Settings.System.getInt(getContentResolver(),
                Settings.System.ACCENT_RANDOM_UNIT, 0);
    }

    private void handleSeekbarValues(int index) {
        switch (index) {
            case 0:
                mAccentRandomColorDuration.setMin(1);
                mAccentRandomColorDuration.setMax(48);
                mAccentRandomColorDuration.setDefaultValue(2);
                break;
            case 1:
                mAccentRandomColorDuration.setMin(1);
                mAccentRandomColorDuration.setMax(60);
                mAccentRandomColorDuration.setDefaultValue(30);
                break;
        }
    }

    private void randomColorPreferenceHandler() {
        if(!isRandomAccentColorIsActive()) {
            mAccentRandomColorDuration.setVisible(false);
            mAccentRandomColorUnit.setVisible(false);
            mAccentRandomColorOnScreenOff.setVisible(false);
            mAccentRandomColorDurationScreenOff.setVisible(false);
            mFooterPref.setVisible(false);
            mAccentPreset.setVisible(true);
            rgbAccentPicker.setVisible(true);
        } else {
            if(isRandomAccentColorScreenOffIsActive()) {
                mAccentRandomColorDurationScreenOff.setVisible(true);
                mAccentRandomColorDuration.setVisible(false);
                mAccentRandomColorUnit.setVisible(false);
                mFooterPref.setVisible(false);
            } else {
                mAccentRandomColorDurationScreenOff.setVisible(false);
                mAccentRandomColorDuration.setVisible(true);
                mAccentRandomColorUnit.setVisible(true);
                mFooterPref.setVisible(true);
            }
             mAccentRandomColorOnScreenOff.setVisible(true);
             mAccentPreset.setVisible(false);
             rgbAccentPicker.setVisible(false);
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

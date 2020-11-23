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

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserHandle;
import androidx.preference.*;
import androidx.preference.Preference.OnPreferenceChangeListener;
import android.provider.Settings;
import android.content.ContentResolver;

import com.android.settings.SettingsPreferenceFragment;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;

public class UserInterfaceSettings extends SettingsPreferenceFragment implements OnPreferenceChangeListener {

    private static final String UI_STYLE = "ui_style";

    private IOverlayManager mOverlayManager;
    private SharedPreferences mSharedPreferences;
    private ListPreference mUIStyle;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.zen_hub_userinterface);

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

    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {

        return false;
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

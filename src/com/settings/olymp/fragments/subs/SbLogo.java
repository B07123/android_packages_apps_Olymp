/*
 *  Copyright (C) 2020 Zeus-OS
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.settings.olymp.fragments.subs;

import com.android.internal.logging.nano.MetricsProto;

import android.app.Activity;
import android.content.Context;
import android.content.ContentResolver;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.database.ContentObserver;
import android.os.SystemProperties;
import android.os.UserHandle;
import androidx.preference.*;
import android.provider.Settings;
import android.view.View;
import android.widget.Toast;
import androidx.preference.PreferenceScreen;

import com.android.settings.SettingsPreferenceFragment;

import com.zeus.support.preferences.SystemSettingListPreference;
import com.zeus.support.preferences.CustomSeekBarPreference;
import com.zeus.support.colorpicker.ColorPickerPreference;

public class SbLogo extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String STATUS_BAR_LOGO_COLOR_MODE = "status_bar_logo_color_mode";
    private static final String STATUSBAR_LOGO_RANDOM_COLOR_INTERVAL = "statusbbar_logo_random_color_interval";
    private static final String STATUS_BAR_LOGO_COLOR = "status_bar_logo_color";

    private SystemSettingListPreference mLogoMode;
    private CustomSeekBarPreference mLogoColorInterval;
    private ColorPickerPreference mLogoColor;

    private static final int DEFAULT_COLOR = 0xffffffff;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.sb_logo);

        mLogoMode = (SystemSettingListPreference) findPreference(STATUS_BAR_LOGO_COLOR_MODE);
        int mode = Settings.System.getIntForUser(getActivity().getContentResolver(),
                Settings.System.STATUS_BAR_LOGO_COLOR_MODE, 0, UserHandle.USER_CURRENT);
        mLogoMode.setValue(String.valueOf(mode));
        mLogoMode.setSummary(mLogoMode.getEntry());
        mLogoMode.setOnPreferenceChangeListener(this);

        mLogoColorInterval = (CustomSeekBarPreference) findPreference(STATUSBAR_LOGO_RANDOM_COLOR_INTERVAL);
        int time = Settings.System.getInt(getContentResolver(),
                Settings.System.STATUSBAR_LOGO_RANDOM_COLOR_INTERVAL, 3);
        mLogoColorInterval.setValue(time);
        mLogoColorInterval.setOnPreferenceChangeListener(this);

        mLogoColor = (ColorPickerPreference) findPreference(STATUS_BAR_LOGO_COLOR);
        mLogoColor.setOnPreferenceChangeListener(this);
        int intColor = Settings.System.getInt(getContentResolver(),
                Settings.System.STATUS_BAR_LOGO_COLOR, DEFAULT_COLOR);
        String hexColor = String.format("#%08x", (DEFAULT_COLOR & intColor));
        mLogoColor.setSummary(hexColor);
        mLogoColor.setNewPreviewColor(intColor);
        handlePreferenceVisibilty(mode);
    }

    private void handlePreferenceVisibilty(int mode) {
        switch (mode) {
            case 0:
                mLogoColorInterval.setVisible(false);
            case 1:
                mLogoColorInterval.setVisible(false);
                break;
            case 2:
                mLogoColorInterval.setVisible(true);
                break;
        }
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        ContentResolver resolver = getActivity().getContentResolver();
        if (preference == mLogoMode) {
            int val = Integer.parseInt((String) newValue);
            int index = mLogoMode.findIndexOfValue((String) newValue);
            Settings.System.putInt(getActivity().getContentResolver(),
                Settings.System.STATUS_BAR_LOGO_COLOR_MODE, val);
            mLogoMode.setSummary(mLogoMode.getEntries()[index]);
            handlePreferenceVisibilty(val);
            return true;
        } else if (preference == mLogoColorInterval) {
            int time = (Integer) newValue;
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.STATUSBAR_LOGO_RANDOM_COLOR_INTERVAL, time);
            return true;
        } else if (preference == mLogoColor) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.STATUS_BAR_LOGO_COLOR, intHex);
            return true;
        }
        return false;
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.ZEUS_SETTINGS;
    }
}

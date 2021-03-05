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

import com.android.settings.SettingsPreferenceFragment;
import com.zeus.support.preferences.CustomSeekBarPreference;
import com.zeus.support.preferences.SystemSettingSwitchPreference;
import com.zeus.support.preferences.SystemSettingListPreference;

public class QsBatteryModes extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String HIDE_PERCENTAGE_NEXT_TO_ESTIMATE = "hide_percentage_next_to_estimate";
    private static final String QS_SHOW_BATTERY_PERCENT = "qs_show_battery_percent";

    private  SystemSettingListPreference mQsBatteryPercentageMode;
    private SystemSettingSwitchPreference mQsHidePercentageNextToEstimate;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.qs_battery_modes);

        mQsBatteryPercentageMode = (SystemSettingListPreference) findPreference(QS_SHOW_BATTERY_PERCENT);
        mQsBatteryPercentageMode.setOnPreferenceChangeListener(this);

        mQsHidePercentageNextToEstimate = (SystemSettingSwitchPreference) findPreference(HIDE_PERCENTAGE_NEXT_TO_ESTIMATE);
        mQsHidePercentageNextToEstimate.setOnPreferenceChangeListener(this);

        handleVisibilty();
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mQsBatteryPercentageMode) {
                int val = Integer.parseInt((String) newValue);
                int index = mQsBatteryPercentageMode.findIndexOfValue((String) newValue);
                Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.QS_SHOW_BATTERY_PERCENT, val);
                mQsBatteryPercentageMode.setSummary(mQsBatteryPercentageMode.getEntries()[index]);
                handleVisibilty();
                return true;
       } else if (preference == mQsHidePercentageNextToEstimate) {
            Boolean value = (Boolean) newValue;
            mQsHidePercentageNextToEstimate.setChecked(value);
            handleVisibilty();
            return true;
        }
        return false;
    }

    private void handleVisibilty() {
        switch (getBatteryPercentageMode()) {
            case 0:
                mQsHidePercentageNextToEstimate.setVisible(false);
                break;
            case 1:
                mQsHidePercentageNextToEstimate.setVisible(false);
                break;
            case 2:
                mQsHidePercentageNextToEstimate.setVisible(true);
                break;
        }
    }


    private int getBatteryPercentageMode() {
        return Settings.System.getInt(getContext().getContentResolver(),
            Settings.System.QS_SHOW_BATTERY_PERCENT, 2);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.ZEUS_SETTINGS;
    }

}

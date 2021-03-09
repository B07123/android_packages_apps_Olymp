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

import android.os.Bundle;
import com.android.settings.R;
import android.content.ContentResolver;
import androidx.preference.*;
import androidx.preference.Preference.OnPreferenceChangeListener;
import com.zeus.support.preferences.SystemSettingEditTextPreference;
import android.os.UserHandle;
import android.provider.Settings;

import com.zeus.support.preferences.SystemSettingSwitchPreference;
import com.zeus.support.preferences.SystemSettingListPreference;

import com.android.settings.SettingsPreferenceFragment;

public class QsFooterItems extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String X_FOOTER_TEXT_STRING = "x_footer_text_string";
    private static final String QS_FOOTER_INFO = "qs_footer_info";
    private static final String QS_FOOTER_INFO_RIGHT = "qs_footer_info_right";

    private SystemSettingEditTextPreference mFooterString;
    private SystemSettingListPreference mFooterInfo;
    private SystemSettingListPreference mFooterInfoRight;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.qs_footer_items);

        mFooterString = (SystemSettingEditTextPreference) findPreference(X_FOOTER_TEXT_STRING);
        mFooterString.setOnPreferenceChangeListener(this);
        String footerString = Settings.System.getString(getContentResolver(),
                X_FOOTER_TEXT_STRING);
        if (footerString != null && footerString != "")
            mFooterString.setText(footerString);
        else {
            mFooterString.setText("Zeus-OS");
            Settings.System.putString(getActivity().getContentResolver(),
                    Settings.System.X_FOOTER_TEXT_STRING, "Zeus-OS");
        }

        mFooterInfo = (SystemSettingListPreference) findPreference(QS_FOOTER_INFO);
        Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.QS_FOOTER_INFO, 3);
        mFooterInfo.setOnPreferenceChangeListener(this);

        mFooterInfoRight = (SystemSettingListPreference) findPreference(QS_FOOTER_INFO_RIGHT);
        Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.QS_FOOTER_INFO_RIGHT, 2);
        mFooterInfoRight.setOnPreferenceChangeListener(this);

    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        ContentResolver resolver = getActivity().getContentResolver();
        if (preference == mFooterString) {
                String value = (String) newValue;
                if (value != "" && value != null)
                    Settings.System.putString(getActivity().getContentResolver(),
                            Settings.System.X_FOOTER_TEXT_STRING, value);
                else {
                    mFooterString.setText("Zeus-OS");
                    Settings.System.putString(getActivity().getContentResolver(),
                            Settings.System.X_FOOTER_TEXT_STRING, "Zeus-OS");
                }
                return true;
        } else if (preference == mFooterInfo) {
            int val = Integer.parseInt((String) newValue);
            int index = mFooterInfo.findIndexOfValue((String) newValue);
            Settings.System.putInt(getActivity().getContentResolver(),
                Settings.System.QS_FOOTER_INFO, val);
            mFooterInfo.setSummary(mFooterInfo.getEntries()[index]);
            return true;
        } else if (preference == mFooterInfoRight) {
            int val = Integer.parseInt((String) newValue);
            int index = mFooterInfoRight.findIndexOfValue((String) newValue);
            Settings.System.putInt(getActivity().getContentResolver(),
                Settings.System.QS_FOOTER_INFO_RIGHT, val);
            mFooterInfoRight.setSummary(mFooterInfoRight.getEntries()[index]);
            return true;
        }
        return false;
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.ZEUS_SETTINGS;
    }

}

/*
 *  Copyright (C) 2020 ZenX-OS
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
package com.zen.hub.fragments.subs;

import com.android.internal.logging.nano.MetricsProto;

import android.os.Bundle;
import com.android.settings.R;
import android.content.ContentResolver;
import androidx.preference.*;
import androidx.preference.Preference.OnPreferenceChangeListener;
import com.zenx.support.preferences.SystemSettingEditTextPreference;
import android.os.UserHandle;
import android.provider.Settings;

import com.android.settings.SettingsPreferenceFragment;

public class QsFooterItems extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String X_FOOTER_TEXT_STRING = "x_footer_text_string";
    private SystemSettingEditTextPreference mFooterString;

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
            mFooterString.setText("ZenX -Feel the Power");
            Settings.System.putString(getActivity().getContentResolver(),
                    Settings.System.X_FOOTER_TEXT_STRING, "ZenX -Feel the Power");
        }
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        ContentResolver resolver = getActivity().getContentResolver();
        if (preference == mFooterString) {
                String value = (String) newValue;
                if (value != "" && value != null)
                    Settings.System.putString(getActivity().getContentResolver(),
                            Settings.System.X_FOOTER_TEXT_STRING, value);
                else {
                    mFooterString.setText("ZenX -Feel the Power");
                    Settings.System.putString(getActivity().getContentResolver(),
                            Settings.System.X_FOOTER_TEXT_STRING, "ZenX -Feel the Power");
                }
                return true;
            }
        return false;
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.ZENX_SETTINGS;
    }

}

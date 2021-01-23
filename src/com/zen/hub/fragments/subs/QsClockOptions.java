/*
 * Copyright (C) 2019 AospExtended
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zen.hub.fragments.subs;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.InputFilter;
import android.text.Spannable;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.SwitchPreference;

import com.android.internal.logging.nano.MetricsProto;
import com.android.internal.util.zenx.ZenxUtils;
import com.zenx.support.preferences.CustomSeekBarPreference;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import com.zenx.support.preferences.SystemSettingListPreference;

public class QsClockOptions extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener {

    public static final String TAG = "QsClockOptions";
    private static final String QS_HEADER_CLOCK_SIZE  = "qs_header_clock_size";

    private CustomSeekBarPreference mQsClockSize;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ContentResolver resolver = getActivity().getContentResolver();

        addPreferencesFromResource(R.xml.qs_clock_options);
        PreferenceScreen prefSet = getPreferenceScreen();

        mQsClockSize = (CustomSeekBarPreference) findPreference(QS_HEADER_CLOCK_SIZE);
        int qsClockSize = Settings.System.getInt(resolver,
                Settings.System.QS_HEADER_CLOCK_SIZE, 14);
                mQsClockSize.setValue(qsClockSize / 1);
        mQsClockSize.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
 		ContentResolver resolver = getActivity().getContentResolver();
       if (preference == mQsClockSize) {
                int width = ((Integer)newValue).intValue();
                Settings.System.putInt(resolver,
                        Settings.System.QS_HEADER_CLOCK_SIZE, width);
                return true;
        }
        return false;
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.ZENX_SETTINGS;
    }
}
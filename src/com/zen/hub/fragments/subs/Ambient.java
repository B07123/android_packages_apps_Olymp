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
import android.content.Context;
import android.content.ContentResolver;
import android.content.res.Resources;
import android.os.UserHandle;
import android.provider.Settings;
import android.os.Bundle;
import android.widget.Toast;
import com.android.settings.R;
import android.text.format.DateFormat;
import android.widget.TimePicker;
import android.app.TimePickerDialog;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragment;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreference;
import androidx.preference.PreferenceScreen;
import androidx.preference.Preference.OnPreferenceChangeListener;

import com.android.settings.SettingsPreferenceFragment;
import com.zenx.support.colorpicker.ColorPickerPreference;
import com.zenx.support.preferences.CustomSeekBarPreference;
import com.zenx.support.preferences.SystemSettingSeekBarPreference;
import com.zenx.support.preferences.SecureSettingListPreference;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.SettingsPreferenceFragment;

public class Ambient extends SettingsPreferenceFragment implements
    Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener {

    private ColorPickerPreference mEdgeLightColorPreference;
    private SystemSettingSeekBarPreference mEdgeLightDurationPreference;
    private SystemSettingSeekBarPreference mEdgeLightRepeatCountPreference;
    private ListPreference mColorMode;
    private SystemSettingSeekBarPreference mEdgeLightTimeoutPreference;
    private static final String MODE_KEY = "doze_always_on_auto_mode";
    private static final String SINCE_PREF_KEY = "doze_always_on_auto_since";
    private static final String TILL_PREF_KEY = "doze_always_on_auto_till";

    private static final String NOTIFICATION_PULSE_COLOR = "ambient_notification_light_color";
    private static final String AMBIENT_LIGHT_DURATION = "ambient_light_duration";
    private static final String AMBIENT_LIGHT_REPEAT_COUNT = "ambient_light_repeat_count";
    private static final String PULSE_COLOR_MODE_PREF = "ambient_notification_light_color_mode";
    private static final String AOD_NOTIFICATION_PULSE_TIMEOUT = "ambient_notification_light_timeout";
    
    private SecureSettingListPreference mModePref;
    private Preference mSincePref;
    private Preference mTillPref;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        ContentResolver resolver = getActivity().getContentResolver();
        addPreferencesFromResource(R.xml.ambient);

        PreferenceScreen prefScreen = getPreferenceScreen();

        mEdgeLightRepeatCountPreference = (SystemSettingSeekBarPreference) findPreference(AMBIENT_LIGHT_REPEAT_COUNT);
        mEdgeLightRepeatCountPreference.setOnPreferenceChangeListener(this);
        int rCount = Settings.System.getInt(getContentResolver(),
                Settings.System.AMBIENT_LIGHT_REPEAT_COUNT, 6);
        mEdgeLightRepeatCountPreference.setValue(rCount);

        mEdgeLightDurationPreference = (SystemSettingSeekBarPreference) findPreference(AMBIENT_LIGHT_DURATION);
        mEdgeLightDurationPreference.setOnPreferenceChangeListener(this);
        int duration = Settings.System.getInt(getContentResolver(),
                Settings.System.AMBIENT_LIGHT_DURATION, 2);
        mEdgeLightDurationPreference.setValue(duration);

        mEdgeLightTimeoutPreference = (SystemSettingSeekBarPreference) findPreference(AOD_NOTIFICATION_PULSE_TIMEOUT);
        mEdgeLightTimeoutPreference.setOnPreferenceChangeListener(this);
        int timeout = Settings.System.getInt(getContentResolver(),
                Settings.System.AOD_NOTIFICATION_PULSE_TIMEOUT, 6);
        mEdgeLightTimeoutPreference.setValue(timeout);

        mEdgeLightColorPreference = (ColorPickerPreference) findPreference(NOTIFICATION_PULSE_COLOR);
        int edgeLightColor = Settings.System.getInt(getContentResolver(),
                Settings.System.NOTIFICATION_PULSE_COLOR, 0xFF3980FF);
        mEdgeLightColorPreference.setNewPreviewColor(edgeLightColor);
        mEdgeLightColorPreference.setAlphaSliderEnabled(false);
        String edgeLightColorHex = String.format("#%08x", (0xFF3980FF & edgeLightColor));
        if (edgeLightColorHex.equals("#ff3980ff")) {
            mEdgeLightColorPreference.setSummary(R.string.color_default);
        } else {
            mEdgeLightColorPreference.setSummary(edgeLightColorHex);
        }
        mEdgeLightColorPreference.setOnPreferenceChangeListener(this);

        mColorMode = (ListPreference) findPreference(PULSE_COLOR_MODE_PREF);
        int value;
        boolean colorModeAutomatic = Settings.System.getInt(getContentResolver(),
                Settings.System.NOTIFICATION_PULSE_COLOR_AUTOMATIC, 1) != 0;
        boolean colorModeAccent = Settings.System.getInt(getContentResolver(),
                Settings.System.NOTIFICATION_PULSE_ACCENT, 0) != 0;
        mEdgeLightColorPreference.setVisible(false);
        if (colorModeAutomatic) {
            value = 0;
        } else if (colorModeAccent) {
            value = 1;
        } else {
            value = 2;
            mEdgeLightColorPreference.setVisible(true);
        }

        mColorMode.setValue(Integer.toString(value));
        mColorMode.setSummary(mColorMode.getEntry());
        mColorMode.setOnPreferenceChangeListener(this);

        mSincePref = findPreference(SINCE_PREF_KEY);
        mSincePref.setOnPreferenceClickListener(this);
        mTillPref = findPreference(TILL_PREF_KEY);
        mTillPref.setOnPreferenceClickListener(this);

        int mode = Settings.Secure.getIntForUser(getContentResolver(),
                MODE_KEY, 0, UserHandle.USER_CURRENT);
        mModePref = (SecureSettingListPreference) findPreference(MODE_KEY);
        mModePref.setValue(String.valueOf(mode));
        mModePref.setSummary(mModePref.getEntry());
        mModePref.setOnPreferenceChangeListener(this);

        updateTimeEnablement(mode == 2);
        updateTimeSummary(mode);
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
         ContentResolver resolver = getActivity().getContentResolver();
        if (preference == mEdgeLightColorPreference) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            if (hex.equals("#ff3980ff")) {
                preference.setSummary(R.string.color_default);
            } else {
                preference.setSummary(hex);
            }
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.NOTIFICATION_PULSE_COLOR, intHex);
            return true;
        } else if (preference == mEdgeLightRepeatCountPreference) {
                int value = (Integer) newValue;
                Settings.System.putInt(getContentResolver(),
                        Settings.System.AMBIENT_LIGHT_REPEAT_COUNT, value);
                return true;
        } else if (preference == mEdgeLightDurationPreference) {
            int value = (Integer) newValue;
                Settings.System.putInt(getContentResolver(),
                    Settings.System.AMBIENT_LIGHT_DURATION, value);
            return true;
        } else if (preference == mEdgeLightTimeoutPreference) {
            int value = (Integer) newValue;
                Settings.System.putInt(getContentResolver(),
                    Settings.System.AOD_NOTIFICATION_PULSE_TIMEOUT, value);
            return true;
        } else if (preference == mColorMode) {
             int value = Integer.valueOf((String) newValue);
            int index = mColorMode.findIndexOfValue((String) newValue);
            mColorMode.setSummary(mColorMode.getEntries()[index]);
            mEdgeLightColorPreference.setVisible(false);
            if (value == 0) {
                Settings.System.putInt(getContentResolver(),
                        Settings.System.NOTIFICATION_PULSE_COLOR_AUTOMATIC, 1);
                Settings.System.putInt(getContentResolver(),
                        Settings.System.NOTIFICATION_PULSE_ACCENT, 0);
            } else if (value == 1) {
                Settings.System.putInt(getContentResolver(),
                        Settings.System.NOTIFICATION_PULSE_COLOR_AUTOMATIC, 0);
                Settings.System.putInt(getContentResolver(),
                        Settings.System.NOTIFICATION_PULSE_ACCENT, 1);
            } else {
                mEdgeLightColorPreference.setVisible(true);
                Settings.System.putInt(getContentResolver(),
                        Settings.System.NOTIFICATION_PULSE_COLOR_AUTOMATIC, 0);
                Settings.System.putInt(getContentResolver(),
                        Settings.System.NOTIFICATION_PULSE_ACCENT, 0);
            }
            return true;
        } else if (preference == mModePref) {
            int value = Integer.valueOf((String) newValue);
            int index = mModePref.findIndexOfValue((String) newValue);
            mModePref.setSummary(mModePref.getEntries()[index]);
            Settings.Secure.putIntForUser(getActivity().getContentResolver(),
                    MODE_KEY, value, UserHandle.USER_CURRENT);
            updateTimeEnablement(value == 2);
            updateTimeSummary(value);
        }
         return false;
    }

    public boolean onPreferenceClick(Preference preference) {
        String[] times = getCustomTimeSetting();
        boolean isSince = preference == mSincePref;
        int hour, minute; hour = minute = 0;
        TimePickerDialog.OnTimeSetListener listener = new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                updateTimeSetting(isSince, hourOfDay, minute);
            }
        };
        if (isSince) {
            String[] sinceValues = times[0].split(":", 0);
            hour = Integer.parseInt(sinceValues[0]);
            minute = Integer.parseInt(sinceValues[1]);
        } else {
            String[] tillValues = times[1].split(":", 0);
            hour = Integer.parseInt(tillValues[0]);
            minute = Integer.parseInt(tillValues[1]);
        }
        TimePickerDialog dialog = new TimePickerDialog(getContext(), listener,
                hour, minute, DateFormat.is24HourFormat(getContext()));
        dialog.show();
        return true;
    }

    private String[] getCustomTimeSetting() {
        String value = Settings.Secure.getStringForUser(getActivity().getContentResolver(),
                Settings.Secure.DOZE_ALWAYS_ON_AUTO_TIME, UserHandle.USER_CURRENT);
        if (value == null || value.equals("")) value = "20:00,07:00";
        return value.split(",", 0);
    }

    private void updateTimeEnablement(boolean enabled) {
        mSincePref.setEnabled(enabled);
        mTillPref.setEnabled(enabled);
    }

    private void updateTimeSummary(int mode) {
        updateTimeSummary(getCustomTimeSetting(), mode);
    }

    private void updateTimeSummary(String[] times, int mode) {
        if (mode == 0) {
            mSincePref.setSummary("-");
            mTillPref.setSummary("-");
            return;
        }
        if (mode == 1) {
            mSincePref.setSummary(R.string.always_on_display_schedule_sunset);
            mTillPref.setSummary(R.string.always_on_display_schedule_sunrise);
            return;
        }
        if (DateFormat.is24HourFormat(getContext())) {
            mSincePref.setSummary(times[0]);
            mTillPref.setSummary(times[1]);
            return;
        }
        String[] sinceValues = times[0].split(":", 0);
        String[] tillValues = times[1].split(":", 0);
        int sinceHour = Integer.parseInt(sinceValues[0]);
        int tillHour = Integer.parseInt(tillValues[0]);
        String sinceSummary = "";
        String tillSummary = "";
        if (sinceHour > 12) {
            sinceHour -= 12;
            sinceSummary += String.valueOf(sinceHour) + ":" + sinceValues[1] + " PM";
        } else {
            sinceSummary = times[0].substring(1) + " AM";
        }
        if (tillHour > 12) {
            tillHour -= 12;
            tillSummary += String.valueOf(tillHour) + ":" + tillValues[1] + " PM";
        } else {
            tillSummary = times[0].substring(1) + " AM";
        }
        mSincePref.setSummary(sinceSummary);
        mTillPref.setSummary(tillSummary);
    }

    private void updateTimeSetting(boolean since, int hour, int minute) {
        String[] times = getCustomTimeSetting();
        String nHour = "";
        String nMinute = "";
        if (hour < 10) nHour += "0";
        if (minute < 10) nMinute += "0";
        nHour += String.valueOf(hour);
        nMinute += String.valueOf(minute);
        times[since ? 0 : 1] = nHour + ":" + nMinute;
        Settings.Secure.putStringForUser(getActivity().getContentResolver(),
                Settings.Secure.DOZE_ALWAYS_ON_AUTO_TIME,
                times[0] + "," + times[1], UserHandle.USER_CURRENT);
        updateTimeSummary(times, 2);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.ZENX_SETTINGS;
    }

}

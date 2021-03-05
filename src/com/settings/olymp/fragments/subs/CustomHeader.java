/*
 * Copyright (C) 2019-2020 crDroid Android Project
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

package com.settings.olymp.fragments.subs;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.SwitchPreference;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.zeus.support.colorpicker.ColorPickerPreference;
import com.zeus.support.preferences.SystemSettingSwitchPreference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CustomHeader extends SettingsPreferenceFragment implements
        OnPreferenceChangeListener {

    private static final String CUSTOM_HEADER_BROWSE = "custom_header_browse";
    private static final String DAYLIGHT_HEADER_PACK = "daylight_header_pack";
    private static final String CUSTOM_HEADER_PROVIDER = "custom_header_provider";
    private static final String STATUS_BAR_CUSTOM_HEADER = "status_bar_custom_header";
    private static final String CUSTOM_HEADER_ENABLED = "status_bar_custom_header";
    private static final String FILE_HEADER_SELECT = "file_header_select";
    private static final String QS_HEADER_STYLE = "qs_header_style";
    private static final String QS_HEADER_STYLE_COLOR = "qs_header_style_color";
    private static final String QS_HEADER_STYLE_GRADIENT = "qs_header_style_gradient";

    private static final int REQUEST_PICK_IMAGE = 0;

    private Preference mHeaderBrowse;
    private ListPreference mDaylightHeaderPack;
    private ListPreference mHeaderProvider;
    private String mDaylightHeaderProvider;
    private SystemSettingSwitchPreference mHeaderEnabled;
    private Preference mFileHeader;
    private String mFileHeaderProvider;
    private ListPreference mQsHeaderStyle;
    private ColorPickerPreference mQsHeaderStyleColor;
    private SystemSettingSwitchPreference mQsHeaderStyleGradient;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.custom_headers);

        PreferenceScreen prefScreen = getPreferenceScreen();
        ContentResolver resolver = getActivity().getContentResolver();
        mHeaderBrowse = findPreference(CUSTOM_HEADER_BROWSE);

        mHeaderEnabled = (SystemSettingSwitchPreference) findPreference(CUSTOM_HEADER_ENABLED);
        mHeaderEnabled.setOnPreferenceChangeListener(this);

        mQsHeaderStyleGradient = (SystemSettingSwitchPreference) findPreference(QS_HEADER_STYLE_GRADIENT);
        mQsHeaderStyleGradient.setChecked(Settings.System.getInt(getContentResolver(),
            Settings.System.QS_HEADER_STYLE_GRADIENT, 0) == 1);
        mQsHeaderStyleGradient.setOnPreferenceChangeListener(this);

        mDaylightHeaderPack = (ListPreference) findPreference(DAYLIGHT_HEADER_PACK);

        List<String> entries = new ArrayList<String>();
        List<String> values = new ArrayList<String>();
        getAvailableHeaderPacks(entries, values);
        mDaylightHeaderPack.setEntries(entries.toArray(new String[entries.size()]));
        mDaylightHeaderPack.setEntryValues(values.toArray(new String[values.size()]));

        boolean headerEnabled = Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_CUSTOM_HEADER, 0) != 0;
        updateHeaderProviderSummary(headerEnabled);
        mDaylightHeaderPack.setOnPreferenceChangeListener(this);

        mDaylightHeaderProvider = getResources().getString(R.string.daylight_header_provider);
        mFileHeaderProvider = getResources().getString(R.string.file_header_provider);
        String providerName = Settings.System.getString(resolver,
                Settings.System.STATUS_BAR_CUSTOM_HEADER_PROVIDER);
        if (providerName == null) {
            providerName = mDaylightHeaderProvider;
        }
        mHeaderBrowse.setEnabled(isBrowseHeaderAvailable() && !providerName.equals(mFileHeaderProvider));

        mHeaderProvider = (ListPreference) findPreference(CUSTOM_HEADER_PROVIDER);
        int valueIndex = mHeaderProvider.findIndexOfValue(providerName);
        mHeaderProvider.setValueIndex(valueIndex >= 0 ? valueIndex : 0);
        mHeaderProvider.setSummary(mHeaderProvider.getEntry());
        mHeaderProvider.setOnPreferenceChangeListener(this);
        mDaylightHeaderPack.setEnabled(providerName.equals(mDaylightHeaderProvider));

        mQsHeaderStyle = (ListPreference) findPreference(CUSTOM_HEADER_PROVIDER);
        int value = mQsHeaderStyle.findIndexOfValue(providerName);
        mQsHeaderStyle.setValueIndex(value >= 0 ? value : 2);
        mQsHeaderStyle.setSummary(mQsHeaderStyle.getEntry());
        mQsHeaderStyle.setOnPreferenceChangeListener(this);

        mFileHeader = findPreference(FILE_HEADER_SELECT);
        mFileHeader.setEnabled(providerName.equals(mFileHeaderProvider));

        getQsHeaderStylePref();
    }

    private void updateHeaderProviderSummary(boolean headerEnabled) {
        mDaylightHeaderPack.setSummary(getResources().getString(R.string.header_provider_disabled));
        if (headerEnabled) {
            String settingHeaderPackage = Settings.System.getString(getActivity().getContentResolver(),
                    Settings.System.STATUS_BAR_DAYLIGHT_HEADER_PACK);
            int valueIndex = mDaylightHeaderPack.findIndexOfValue(settingHeaderPackage);
            if (valueIndex == -1) {
                // no longer found
                Settings.System.putInt(getActivity().getContentResolver(),
                        Settings.System.STATUS_BAR_CUSTOM_HEADER, 0);
            } else {
                mDaylightHeaderPack.setValueIndex(valueIndex >= 0 ? valueIndex : 0);
                mDaylightHeaderPack.setSummary(mDaylightHeaderPack.getEntry());
            }
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        ContentResolver resolver = getActivity().getContentResolver();
        if (preference == mDaylightHeaderPack) {
            String value = (String) newValue;
            Settings.System.putString(resolver,
                    Settings.System.STATUS_BAR_DAYLIGHT_HEADER_PACK, value);
            int valueIndex = mDaylightHeaderPack.findIndexOfValue(value);
            mDaylightHeaderPack.setSummary(mDaylightHeaderPack.getEntries()[valueIndex]);
            return true;
        } else if (preference == mHeaderProvider) {
            String value = (String) newValue;
            Settings.System.putString(resolver,
                    Settings.System.STATUS_BAR_CUSTOM_HEADER_PROVIDER, value);
            int valueIndex = mHeaderProvider.findIndexOfValue(value);
            mHeaderProvider.setSummary(mHeaderProvider.getEntries()[valueIndex]);
            mDaylightHeaderPack.setEnabled(value.equals(mDaylightHeaderProvider));
            mHeaderBrowse.setEnabled(!value.equals(mFileHeaderProvider));
            mHeaderBrowse.setTitle(valueIndex == 0 ? R.string.custom_header_browse_title : R.string.custom_header_pick_title);
            mHeaderBrowse.setSummary(valueIndex == 0 ? R.string.custom_header_browse_summary_new : R.string.custom_header_pick_summary);
            mFileHeader.setEnabled(value.equals(mFileHeaderProvider));
            return true;
        } else if (preference == mHeaderEnabled) {
            Boolean headerEnabled = (Boolean) newValue;
            if(headerEnabled){
                Settings.System.putIntForUser(resolver,
                    Settings.System.QS_HEADER_STYLE_GRADIENT, 0, UserHandle.USER_CURRENT);
                mQsHeaderStyleGradient.setChecked(false);
                Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.QS_HEADER_STYLE, 2);
                mQsHeaderStyle.setSummary(mQsHeaderStyle.getEntries()[2]);
            }
            updateHeaderProviderSummary(headerEnabled);
            return true;
        } else if (preference == mQsHeaderStyle) {
            String value = (String) newValue;
            Settings.System.putInt(getActivity().getContentResolver(),
			    Settings.System.QS_HEADER_STYLE, Integer.valueOf(value));
            int newIndex = mQsHeaderStyle.findIndexOfValue(value);
            mQsHeaderStyle.setSummary(mQsHeaderStyle.getEntries()[newIndex]);
            updateQsHeaderStyleColor();
            return true;
        } else if (preference == mQsHeaderStyleColor) {
                String hex = ColorPickerPreference.convertToARGB(
                        Integer.valueOf(String.valueOf(newValue)));
                preference.setSummary(hex);
                int intHex = ColorPickerPreference.convertToColorInt(hex);
                Settings.System.putInt(getActivity().getContentResolver(),
                        Settings.System.QS_HEADER_STYLE_COLOR, intHex);
            return true;
        } else if (preference == mQsHeaderStyleGradient) {
            int val = ((Boolean) newValue) ? 1 : 0;
            Settings.System.putInt(getContentResolver(), Settings.System.QS_HEADER_STYLE_GRADIENT, val);
            return true;
        }
        return false;
    }

    private void getQsHeaderStylePref() {
        mQsHeaderStyle = (ListPreference) findPreference(QS_HEADER_STYLE);
        int qsHeaderStyle = Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.QS_HEADER_STYLE, 2);
        int valueIndex = mQsHeaderStyle.findIndexOfValue(String.valueOf(qsHeaderStyle));
        mQsHeaderStyle.setValueIndex(valueIndex >= 0 ? valueIndex : 0);
        mQsHeaderStyle.setSummary(mQsHeaderStyle.getEntry());
        mQsHeaderStyle.setOnPreferenceChangeListener(this);
        updateQsHeaderStyleColor();
    }

    private void updateQsHeaderStyleColor() {
        int qsHeaderStyle = Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.QS_HEADER_STYLE, 0);

        if(qsHeaderStyle == 3) {
                mQsHeaderStyleColor = (ColorPickerPreference) findPreference(QS_HEADER_STYLE_COLOR);
                int qsHeaderStyleColor = Settings.System.getInt(getContentResolver(),
                        Settings.System.QS_HEADER_STYLE_COLOR, 0x0000000);
                mQsHeaderStyleColor.setNewPreviewColor(qsHeaderStyleColor);
                String qsHeaderStyleColorHex = String.format("#%08x", (0x0000000 & qsHeaderStyleColor));
                mQsHeaderStyleColor.setSummary(qsHeaderStyleColorHex);
                mQsHeaderStyleColor.setOnPreferenceChangeListener(this);
                mQsHeaderStyleColor.setVisible(true);
        } else {
                mQsHeaderStyleColor = (ColorPickerPreference) findPreference(QS_HEADER_STYLE_COLOR);
                if(mQsHeaderStyleColor != null) {
                        mQsHeaderStyleColor.setVisible(false);
                }
        }
    }

   @Override
    public void onResume() {
        super.onResume();
        updateQsHeaderStyleColor();
    }

    @Override
    public void onPause() {
        super.onPause();
        updateQsHeaderStyleColor();
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference == mFileHeader) {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            startActivityForResult(intent, REQUEST_PICK_IMAGE);
            return true;
        }
        return super.onPreferenceTreeClick(preference);
    }

    private boolean isBrowseHeaderAvailable() {
        PackageManager pm = getActivity().getPackageManager();
        Intent browse = new Intent();
        browse.setClassName("org.omnirom.omnistyle", "org.omnirom.omnistyle.PickHeaderActivity");
        return pm.resolveActivity(browse, 0) != null;
    }

    private void getAvailableHeaderPacks(List<String> entries, List<String> values) {
        Map<String, String> headerMap = new HashMap<String, String>();
        Intent i = new Intent();
        PackageManager packageManager = getActivity().getPackageManager();
        i.setAction("org.omnirom.DaylightHeaderPack");
        for (ResolveInfo r : packageManager.queryIntentActivities(i, 0)) {
            String packageName = r.activityInfo.packageName;
            String label = r.activityInfo.loadLabel(getActivity().getPackageManager()).toString();
            if (label == null) {
                label = r.activityInfo.packageName;
            }
            headerMap.put(label, packageName);
        }
        i.setAction("org.omnirom.DaylightHeaderPack1");
        for (ResolveInfo r : packageManager.queryIntentActivities(i, 0)) {
            String packageName = r.activityInfo.packageName;
            String label = r.activityInfo.loadLabel(getActivity().getPackageManager()).toString();
            if (r.activityInfo.name.endsWith(".theme")) {
                continue;
            }
            if (label == null) {
                label = packageName;
            }
            headerMap.put(label, packageName  + "/" + r.activityInfo.name);
        }
        List<String> labelList = new ArrayList<String>();
        labelList.addAll(headerMap.keySet());
        Collections.sort(labelList);
        for (String label : labelList) {
            entries.add(label);
            values.add(headerMap.get(label));
        }
    }

    public static void reset(Context mContext) {
        ContentResolver resolver = mContext.getContentResolver();
        Settings.System.putIntForUser(resolver,
                Settings.System.STATUS_BAR_CUSTOM_HEADER, 0, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.STATUS_BAR_CUSTOM_HEADER_SHADOW, 0, UserHandle.USER_CURRENT);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.ZEUS_SETTINGS;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent result) {
        if (requestCode == REQUEST_PICK_IMAGE) {
            if (resultCode != Activity.RESULT_OK) {
                return;
            }
            final Uri imageUri = result.getData();
            Settings.System.putString(getContentResolver(), Settings.System.STATUS_BAR_FILE_HEADER_IMAGE, imageUri.toString());
        }
    }
}

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

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.SwitchPreference;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

import com.settings.olymp.fragments.subs.CustomHeader;
import com.zeus.support.preferences.CustomSeekBarPreference;

import java.util.List;
import java.util.ArrayList;

import lineageos.providers.LineageSettings;

public class QsColumnRows extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    public static final String TAG = "QuickSettingsColumnRows";

    private static final String KEY_COL_PORTRAIT = "qs_columns_portrait";
    private static final String KEY_ROW_PORTRAIT = "qs_rows_portrait";
    private static final String KEY_COL_LANDSCAPE = "qs_columns_landscape";
    private static final String KEY_ROW_LANDSCAPE = "qs_rows_landscape";
    private static final String PREF_COLUMNS_QUICKBAR = "qs_quickbar_columns";

    CustomSeekBarPreference mColPortrait;
    CustomSeekBarPreference mRowPortrait;
    CustomSeekBarPreference mColLandscape;
    CustomSeekBarPreference mRowLandscape;
    CustomSeekBarPreference mQsColumnsQuickbar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.qs_column_rows);

        mColPortrait = (CustomSeekBarPreference) findPreference(KEY_COL_PORTRAIT);
        mRowPortrait = (CustomSeekBarPreference) findPreference(KEY_ROW_PORTRAIT);
        mColLandscape = (CustomSeekBarPreference) findPreference(KEY_COL_LANDSCAPE);
        mRowLandscape = (CustomSeekBarPreference) findPreference(KEY_ROW_LANDSCAPE);

        Resources res = null;
        Context ctx = getContext();

        try {
            res = ctx.getPackageManager().getResourcesForApplication("com.android.systemui");
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }

        int col_portrait = res.getInteger(res.getIdentifier(
                "com.android.systemui:integer/config_qs_columns_portrait", null, null));
        int row_portrait = res.getInteger(res.getIdentifier(
                "com.android.systemui:integer/config_qs_rows_portrait", null, null));
        int col_landscape = res.getInteger(res.getIdentifier(
                "com.android.systemui:integer/config_qs_columns_landscape", null, null));
        int row_landscape = res.getInteger(res.getIdentifier(
                "com.android.systemui:integer/config_qs_rows_landscape", null, null));

        mColPortrait.setDefaultValue(col_portrait);
        mRowPortrait.setDefaultValue(row_portrait);
        mColLandscape.setDefaultValue(col_landscape);
        mRowLandscape.setDefaultValue(row_landscape);

        int mColPortraitVal = Settings.System.getIntForUser(ctx.getContentResolver(),
                Settings.System.QS_COLUMNS_PORTRAIT, col_portrait, UserHandle.USER_CURRENT);
        int mRowPortraitVal = Settings.System.getIntForUser(ctx.getContentResolver(),
                Settings.System.QS_ROWS_PORTRAIT, row_portrait, UserHandle.USER_CURRENT);
        int mColLandscapeVal = Settings.System.getIntForUser(ctx.getContentResolver(),
                Settings.System.QS_COLUMNS_LANDSCAPE, col_landscape, UserHandle.USER_CURRENT);
        int mRowLandscapeVal = Settings.System.getIntForUser(ctx.getContentResolver(),
                Settings.System.QS_ROWS_LANDSCAPE, row_landscape, UserHandle.USER_CURRENT);

        mQsColumnsQuickbar = (CustomSeekBarPreference) findPreference(PREF_COLUMNS_QUICKBAR);
        int columnsQuickbar = Settings.System.getInt(ctx.getContentResolver(),
                Settings.System.QS_QUICKBAR_COLUMNS, 6);
        mQsColumnsQuickbar.setValue(columnsQuickbar);
        mQsColumnsQuickbar.setOnPreferenceChangeListener(this);

        mColPortrait.setValue(mColPortraitVal);
        mRowPortrait.setValue(mRowPortraitVal);
        mColLandscape.setValue(mColLandscapeVal);
        mRowLandscape.setValue(mRowLandscapeVal);      

    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mQsColumnsQuickbar) {
            int value = (Integer) newValue;
            Settings.System.putIntForUser(getContentResolver(),
                    Settings.System.QS_QUICKBAR_COLUMNS, value, UserHandle.USER_CURRENT);
            return true;
        }
        return false;
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.ZEUS_SETTINGS;
    }

}

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

import com.android.internal.util.zenx.ThemesUtils;
import android.content.om.IOverlayManager;
import android.content.SharedPreferences;
import android.os.ServiceManager;
import android.content.om.OverlayInfo;
import android.graphics.Color;
import android.os.SystemProperties;
import android.os.RemoteException;
import android.content.Context;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.settings.SettingsPreferenceFragment;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.internal.util.zenx.ZenxUtils;
import com.zen.hub.utils.Utils;

import static com.zen.hub.utils.Utils.handleOverlays;
import com.zenx.support.preferences.SystemSettingListPreference;

public class Navigation extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener{

    private static final String NAVBAR_STYLE = "navbar_style";
    private static final String CATEGORY_BOTTONS = "buttons_cat_key";

    private IOverlayManager mOverlayManager;
    private SystemSettingListPreference mNavbarStyle;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.zen_hub_navigation);

        final PreferenceScreen prefScreen = getPreferenceScreen();

        final PreferenceCategory batteryCategory = prefScreen.findPreference(CATEGORY_BOTTONS);

        final boolean hasHomeKey = Utils.hasHomeKey(getActivity());
        final boolean hasBackKey = Utils.hasBackKey(getActivity());
        final boolean hasMenuKey = Utils.hasMenuKey(getActivity());
        final boolean hasAssistKey = Utils.hasAssistKey(getActivity());
        final boolean hasAppSwitchKey = Utils.hasAppSwitchKey(getActivity());
        final boolean hasCameraKey = Utils.hasCameraKey(getActivity());

        if (!hasHomeKey && !hasBackKey && !hasMenuKey && !hasAssistKey && !hasAppSwitchKey) {
            prefScreen.removePreference(batteryCategory);
        }

        mOverlayManager = IOverlayManager.Stub.asInterface(
            ServiceManager.getService(Context.OVERLAY_SERVICE));

        mNavbarStyle = (SystemSettingListPreference) findPreference(NAVBAR_STYLE);
        int navbarStyle = Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.NAVBAR_STYLE, 0);
        int navbarStyleValue = getOverlayPosition(ThemesUtils.NAVBAR_STYLES);
        if (navbarStyleValue != 0) {
            mNavbarStyle.setValue(String.valueOf(navbarStyle));
        }
        mNavbarStyle.setSummary(mNavbarStyle.getEntry());
        mNavbarStyle.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (preference == mNavbarStyle) {
                    String value = (String) newValue;
                    Settings.System.putInt(getActivity().getContentResolver(), Settings.System.NAVBAR_STYLE, Integer.valueOf(value));
                    int valueIndex = mNavbarStyle.findIndexOfValue(value);
                    mNavbarStyle.setSummary(mNavbarStyle.getEntries()[valueIndex]);
                    String overlayName = getOverlayName(ThemesUtils.NAVBAR_STYLES);
                    if (overlayName != null) {
                    handleOverlays(overlayName, false, mOverlayManager);
                    }
                    if (valueIndex > 0) {
                        handleOverlays(ThemesUtils.NAVBAR_STYLES[valueIndex],
                                true, mOverlayManager);
                    }
                    return true;
                }
                return false;
            }
       });
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        ContentResolver resolver = getActivity().getContentResolver();

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

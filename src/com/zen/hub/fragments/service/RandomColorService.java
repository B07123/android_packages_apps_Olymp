/*
 * Copyright (C) 2021 ZenX-OS
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

package com.zen.hub.fragments.subs.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.ContentResolver;
import android.content.Intent;
import android.util.Log;
import android.provider.Settings;
import androidx.preference.PreferenceManager;
import java.util.Random;
import android.os.UserHandle;
import android.os.Handler;
import android.graphics.Color;
import android.os.SystemProperties;
import android.content.om.IOverlayManager;
import android.os.ServiceManager;
import android.os.RemoteException;
import android.os.IBinder;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;

import java.util.Timer;
import java.util.TimerTask;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class RandomColorService extends Service {
    private static final String TAG = "ZenXRandomColorService";

    private String ACCENT_COLOR_PROP = "persist.sys.theme.accentcolor";

    private static final boolean DEBUG = false;
    private Handler mHandler = new Handler();

    private Timer mTimer = null;
    private Timer mScreemOffTimer = null;
    private int mCounter = 0;

    ScheduledExecutorService executorScreenOn;
    ScheduledExecutorService executorScreenOff;

    private BroadcastReceiver mScreenStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                onDisplayOn(context);
            } else if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                onDisplayOff(context);
            }
        }
    };

    @Override
    public void onCreate() {
        if (DEBUG) Log.d(TAG, "Creating service");

        IntentFilter screenStateFilter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        screenStateFilter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(mScreenStateReceiver, screenStateFilter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (DEBUG) Log.d(TAG, "Starting service");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (DEBUG) Log.d(TAG, "Destroying service");
        super.onDestroy();
        if(executorScreenOn != null) {
            executorScreenOn.shutdown();
        }
        if(executorScreenOff != null) {
            executorScreenOff.shutdown();
        }
        this.unregisterReceiver(mScreenStateReceiver);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void onDisplayOn(Context context) {
        if (DEBUG) Log.d(TAG, "Display on");
        mCounter = 0;
        if(executorScreenOff != null) {
            executorScreenOff.shutdown();
        }

        int interval = getRandomAccentColorInterval();

        TimeUnit time = null;
        int unit = 0;

        switch (getRandomAccentColorUnit()) {
            case 0:
                time = TimeUnit.HOURS;
                break;
            case 1:
                time = TimeUnit.MINUTES;
                break;
        }

        executorScreenOn = Executors.newScheduledThreadPool(1);

        boolean isScreenOffActive = isColorChangeOnScreenOffActive();

        if(isRandomColorActive() && !isScreenOffActive) {
            Runnable task = () -> {
                updateAccentColor();
            };
            executorScreenOn.scheduleWithFixedDelay(task, interval, interval, time);
        } else {
            if(executorScreenOn != null) {
                executorScreenOn.shutdown();
            }
        }
    }

    private void onDisplayOff(Context context) {
        if (DEBUG) Log.d(TAG, "Display off");

        if(executorScreenOn != null && !isColorChangeOnScreenOffActive()) {
            executorScreenOn.shutdown();
        }


            int interval = getRandomAccentColorIntervalScreenOff();


            executorScreenOff = Executors.newScheduledThreadPool(1);

            boolean isScreenOnActive = isRandomColorActive();

            if(isColorChangeOnScreenOffActive() && isScreenOnActive) {
                    Runnable task = () -> {
                        if(mCounter == 0) {
                            updateAccentColor();
                            mCounter =  mCounter + 1;
                        }
                    };
                    executorScreenOff.scheduleWithFixedDelay(task, interval, interval, TimeUnit.SECONDS);
            } else {
                if(executorScreenOff != null) {
                    executorScreenOff.shutdown();
                }
            }
    }

    public void updateAccentColor() {
        IOverlayManager mOverlayManager = IOverlayManager.Stub.asInterface(
                ServiceManager.getService(Context.OVERLAY_SERVICE));

                int color = getRandomColor();
                String hexColor = String.format("%08X", (0xFFFFFFFF & color));
                SystemProperties.set(ACCENT_COLOR_PROP, hexColor);
                            try {
                                mOverlayManager.reloadAndroidAssets(UserHandle.USER_CURRENT);
                                mOverlayManager.reloadAssets("com.android.settings", UserHandle.USER_CURRENT);
                                mOverlayManager.reloadAssets("com.android.systemui", UserHandle.USER_CURRENT);
            } catch (RemoteException ignored) { }
    }

    private boolean isRandomColorActive() {
        return Settings.System.getInt(getContentResolver(),
                Settings.System.ACCENT_RANDOM_COLOR, 0) == 1;
    }

    private boolean isColorChangeOnScreenOffActive() {
        return Settings.System.getInt(getContentResolver(),
                Settings.System.CHANGE_ACCENT_COLOR_ON_SCREEN_OFF, 1) == 1;
    }

    private int getRandomAccentColorUnit() {
        return Settings.System.getInt(getContentResolver(),
                Settings.System.ACCENT_RANDOM_UNIT, 0);
    }

    private int getRandomAccentColorInterval() {
        return Settings.System.getInt(getContentResolver(),
                Settings.System.ACCENT_RANDOM_COLOR_DURATION, 4);
    }

    private int getRandomAccentColorIntervalScreenOff() {
        return Settings.System.getInt(getContentResolver(),
                Settings.System.ACCENT_RANDOM_COLOR_DURATION_SCREENOFF, 30);
    }


    public int getRandomColor(){
        Random rnd = new Random();
            return Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256));
    }
}

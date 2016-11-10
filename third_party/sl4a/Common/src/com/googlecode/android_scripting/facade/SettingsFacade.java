/*
 * Copyright (C) 2016 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.googlecode.android_scripting.facade;

import android.app.AlarmManager;
import android.app.Service;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.Settings.SettingNotFoundException;
import android.view.WindowManager;

import com.android.internal.widget.LockPatternUtils;
import com.googlecode.android_scripting.BaseApplication;
import com.googlecode.android_scripting.FutureActivityTaskExecutor;
import com.googlecode.android_scripting.Log;
import com.googlecode.android_scripting.future.FutureActivityTask;
import com.googlecode.android_scripting.jsonrpc.RpcReceiver;
import com.googlecode.android_scripting.rpc.Rpc;
import com.googlecode.android_scripting.rpc.RpcOptional;
import com.googlecode.android_scripting.rpc.RpcParameter;

/**
 * Exposes phone settings functionality.
 *
 */
public class SettingsFacade extends RpcReceiver {

    private final Service mService;
    private final AndroidFacade mAndroidFacade;
    private final AudioManager mAudio;
    private final PowerManager mPower;
    private final AlarmManager mAlarm;
    private final LockPatternUtils mLockPatternUtils;

    /**
     * Creates a new SettingsFacade.
     *
     * @param service is the {@link Context} the APIs will run under
     */
    public SettingsFacade(FacadeManager manager) {
        super(manager);
        mService = manager.getService();
        mAndroidFacade = manager.getReceiver(AndroidFacade.class);
        mAudio = (AudioManager) mService.getSystemService(Context.AUDIO_SERVICE);
        mPower = (PowerManager) mService.getSystemService(Context.POWER_SERVICE);
        mAlarm = (AlarmManager) mService.getSystemService(Context.ALARM_SERVICE);
        mLockPatternUtils = new LockPatternUtils(mService);
    }

    @Rpc(description = "Sets the screen timeout to this number of seconds.",
            returns = "The original screen timeout.")
    public Integer setScreenTimeout(@RpcParameter(name = "value") Integer value) {
        Integer oldValue = getScreenTimeout();
        android.provider.Settings.System.putInt(mService.getContentResolver(),
                android.provider.Settings.System.SCREEN_OFF_TIMEOUT, value * 1000);
        return oldValue;
    }

    @Rpc(description = "Returns the current screen timeout in seconds.",
            returns = "the current screen timeout in seconds.")
    public Integer getScreenTimeout() {
        try {
            return android.provider.Settings.System.getInt(mService.getContentResolver(),
                    android.provider.Settings.System.SCREEN_OFF_TIMEOUT) / 1000;
        } catch (SettingNotFoundException e) {
            return 0;
        }
    }

    @Rpc(description = "Checks the ringer silent mode setting.",
            returns = "True if ringer silent mode is enabled.")
    public Boolean checkRingerSilentMode() {
        return mAudio.getRingerMode() == AudioManager.RINGER_MODE_SILENT;
    }

    @Rpc(description = "Toggles ringer silent mode on and off.",
            returns = "True if ringer silent mode is enabled.")
    public Boolean toggleRingerSilentMode(
            @RpcParameter(name = "enabled") @RpcOptional Boolean enabled) {
        if (enabled == null) {
            enabled = !checkRingerSilentMode();
        }
        mAudio.setRingerMode(enabled ? AudioManager.RINGER_MODE_SILENT
                : AudioManager.RINGER_MODE_NORMAL);
        return enabled;
    }

    @Rpc(description = "Set the ringer to a specified mode")
    public void setRingerMode(@RpcParameter(name = "mode") Integer mode) throws Exception {
        if (AudioManager.isValidRingerMode(mode)) {
            mAudio.setRingerMode(mode);
        } else {
            throw new Exception("Ringer mode " + mode + " does not exist.");
        }
    }

    @Rpc(description = "Returns the current ringtone mode.",
            returns = "An integer representing the current ringer mode")
    public Integer getRingerMode() {
        return mAudio.getRingerMode();
    }

    @Rpc(description = "Returns the maximum ringer volume.")
    public int getMaxRingerVolume() {
        return mAudio.getStreamMaxVolume(AudioManager.STREAM_RING);
    }

    @Rpc(description = "Returns the current ringer volume.")
    public int getRingerVolume() {
        return mAudio.getStreamVolume(AudioManager.STREAM_RING);
    }

    @Rpc(description = "Sets the ringer volume.")
    public void setRingerVolume(@RpcParameter(name = "volume") Integer volume) {
        mAudio.setStreamVolume(AudioManager.STREAM_RING, volume, 0);
    }

    @Rpc(description = "Returns the maximum media volume.")
    public int getMaxMediaVolume() {
        return mAudio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
    }

    @Rpc(description = "Returns the current media volume.")
    public int getMediaVolume() {
        return mAudio.getStreamVolume(AudioManager.STREAM_MUSIC);
    }

    @Rpc(description = "Sets the media volume.")
    public void setMediaVolume(@RpcParameter(name = "volume") Integer volume) {
        mAudio.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0);
    }

    @Rpc(description = "Returns the screen backlight brightness.",
            returns = "the current screen brightness between 0 and 255")
    public Integer getScreenBrightness() {
        try {
            return android.provider.Settings.System.getInt(mService.getContentResolver(),
                    android.provider.Settings.System.SCREEN_BRIGHTNESS);
        } catch (SettingNotFoundException e) {
            return 0;
        }
    }

    @Rpc(description = "return the system time since boot in nanoseconds")
    public long getSystemElapsedRealtimeNanos() {
        return SystemClock.elapsedRealtimeNanos();
    }

    @Rpc(description = "Sets the the screen backlight brightness.",
            returns = "the original screen brightness.")
    public Integer setScreenBrightness(
            @RpcParameter(name = "value", description = "brightness value between 0 and 255") Integer value) {
        if (value < 0) {
            value = 0;
        } else if (value > 255) {
            value = 255;
        }
        final int brightness = value;
        Integer oldValue = getScreenBrightness();
        android.provider.Settings.System.putInt(mService.getContentResolver(),
                android.provider.Settings.System.SCREEN_BRIGHTNESS, brightness);

        FutureActivityTask<Object> task = new FutureActivityTask<Object>() {
            @Override
            public void onCreate() {
                super.onCreate();
                WindowManager.LayoutParams lp = getActivity().getWindow().getAttributes();
                lp.screenBrightness = brightness * 1.0f / 255;
                getActivity().getWindow().setAttributes(lp);
                setResult(null);
                finish();
            }
        };

        FutureActivityTaskExecutor taskExecutor =
                ((BaseApplication) mService.getApplication()).getTaskExecutor();
        taskExecutor.execute(task);

        return oldValue;
    }

    @Rpc(description = "Returns true if the device is in an interactive state.")
    public Boolean isDeviceInteractive() throws Exception {
        return mPower.isInteractive();
    }

    @Rpc(description = "Issues a request to put the device to sleep after a delay.")
    public void goToSleep(Integer delay) {
        mPower.goToSleep(SystemClock.uptimeMillis() + delay);
    }

    @Rpc(description = "Issues a request to put the device to sleep right away.")
    public void goToSleepNow() {
        mPower.goToSleep(SystemClock.uptimeMillis());
    }

    @Rpc(description = "Issues a request to wake the device up right away.")
    public void wakeUpNow() {
        mPower.wakeUp(SystemClock.uptimeMillis());
    }

    @Rpc(description = "Get Up time of device.",
            returns = "Long value of device up time in milliseconds.")
    public long getDeviceUpTime() throws Exception {
        return SystemClock.elapsedRealtime();
    }

    @Rpc(description = "Set a string password to the device.")
    public void setDevicePassword(@RpcParameter(name = "password") String password) {
        // mLockPatternUtils.setLockPatternEnabled(true, UserHandle.myUserId());
        mLockPatternUtils.setLockScreenDisabled(false, UserHandle.myUserId());
        mLockPatternUtils.setCredentialRequiredToDecrypt(true);
        mLockPatternUtils.saveLockPassword(password, null,
                DevicePolicyManager.PASSWORD_QUALITY_NUMERIC, UserHandle.myUserId());
    }

    @Rpc(description = "Disable screen lock password on the device.")
    public void disableDevicePassword() {
        mLockPatternUtils.clearEncryptionPassword();
        // mLockPatternUtils.setLockPatternEnabled(false, UserHandle.myUserId());
        mLockPatternUtils.setLockScreenDisabled(true, UserHandle.myUserId());
        mLockPatternUtils.setCredentialRequiredToDecrypt(false);
        mLockPatternUtils.clearEncryptionPassword();
        mLockPatternUtils.clearLock(UserHandle.myUserId());
        mLockPatternUtils.setLockScreenDisabled(true, UserHandle.myUserId());
    }

    @Rpc(description = "Set the system time in epoch.")
    public void setTime(Long currentTime) {
        mAlarm.setTime(currentTime);
    }

    @Rpc(description = "Set the system time zone.")
    public void setTimeZone(@RpcParameter(name = "timeZone") String timeZone) {
        mAlarm.setTimeZone(timeZone);
    }

    @Rpc(description = "Show Home Screen")
    public void showHomeScreen() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            mAndroidFacade.startActivityIntent(intent, false);
        } catch (RuntimeException e) {
            Log.d("showHomeScreen RuntimeException" + e);
        } catch (Exception e){
            Log.d("showHomeScreen exception" + e);
        }
    }

    @Override
    public void shutdown() {
        // Nothing to do yet.
    }
}

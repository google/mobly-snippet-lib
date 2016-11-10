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

package com.googlecode.android_scripting.facade.telephony;

import android.app.Activity;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.telephony.CarrierConfigManager;

import com.googlecode.android_scripting.facade.AndroidFacade;
import com.googlecode.android_scripting.facade.FacadeManager;
import com.googlecode.android_scripting.jsonrpc.RpcReceiver;
import com.googlecode.android_scripting.rpc.Rpc;
import com.googlecode.android_scripting.rpc.RpcDefault;
import com.googlecode.android_scripting.rpc.RpcParameter;
import com.googlecode.android_scripting.Log;
import com.googlecode.android_scripting.MainThread;
import com.googlecode.android_scripting.rpc.RpcOptional;

public class CarrierConfigFacade extends RpcReceiver {
    private final Service mService;
    private final AndroidFacade mAndroidFacade;
    private final CarrierConfigManager mCarrierConfigManager;

    public CarrierConfigFacade(FacadeManager manager) {
        super(manager);
        mService = manager.getService();
        mAndroidFacade = manager.getReceiver(AndroidFacade.class);
        mCarrierConfigManager =
            (CarrierConfigManager)mService.getSystemService(Context.CARRIER_CONFIG_SERVICE);
    }

    @Rpc(description = "Tethering Entitlement Check")
    public boolean carrierConfigIsTetheringModeAllowed(
        @RpcParameter(name="mode") String mode,
        @RpcParameter(name="timeout") Integer timeout) {
        String[] mProvisionApp = mService.getResources().getStringArray(
                com.android.internal.R.array.config_mobile_hotspot_provision_app);
        /* following check defined in
        frameworks/base/packages/SettingsLib/src/com/android/settingslib/TetherUtil.java
        isProvisioningNeeded
        */
        if ((mProvisionApp == null) || (mProvisionApp.length != 2)){
            Log.d("carrierConfigIsTetheringModeAllowed: no check is present.");
            return true;
        }
        Log.d("carrierConfigIsTetheringModeAllowed mProvisionApp 0 " + mProvisionApp[0]);
        Log.d("carrierConfigIsTetheringModeAllowed mProvisionApp 1 " + mProvisionApp[1]);

        /* defined in frameworks/base/packages/SettingsLib/src/com/android/settingslib/TetherUtil.java
        public static final int INVALID             = -1;
        public static final int WIFI_TETHERING      = 0;
        public static final int USB_TETHERING       = 1;
        public static final int BLUETOOTH_TETHERING = 2;
        */
        // TODO: b/26273844 need to use android.settingslib.TetherUtil to
        // replace those private defines.
        final int INVALID             = -1;
        final int WIFI_TETHERING      = 0;
        final int USB_TETHERING       = 1;
        final int BLUETOOTH_TETHERING = 2;

        /* defined in packages/apps/Settings/src/com/android/settings/TetherSettings.java
        private static final int PROVISION_REQUEST = 0;
        */
        final int PROVISION_REQUEST = 0;

        int mTetherChoice = INVALID;
        if (mode.equals("wifi")){
            mTetherChoice = WIFI_TETHERING;
        } else if (mode.equals("usb")) {
            mTetherChoice = USB_TETHERING;
        } else if (mode.equals("bluetooth")) {
            mTetherChoice = BLUETOOTH_TETHERING;
        }
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setClassName(mProvisionApp[0], mProvisionApp[1]);
        intent.putExtra("TETHER_TYPE", mTetherChoice);
        int result;
        try{
            result = mAndroidFacade.startActivityForResultCodeWithTimeout(
                intent, PROVISION_REQUEST, timeout);
        } catch (Exception e) {
            Log.d("phoneTetherCheck exception" + e.toString());
            return false;
        }

        if (result == Activity.RESULT_OK) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void shutdown() {

    }
}

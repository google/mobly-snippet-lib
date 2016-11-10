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

package com.googlecode.android_scripting.facade.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

public class BluetoothBroadcastHelper {

  private static BroadcastReceiver mListener;
  private final Context mContext;
  private final BroadcastReceiver mReceiver;
  private final String[] mActions = {BluetoothDevice.ACTION_FOUND,
                                     BluetoothDevice.ACTION_UUID,
                                     BluetoothAdapter.ACTION_DISCOVERY_STARTED,
                                     BluetoothAdapter.ACTION_DISCOVERY_FINISHED};

  public BluetoothBroadcastHelper(Context context, BroadcastReceiver listener) {
    mContext = context;
    mListener = listener;
    mReceiver = new BluetoothReceiver();
  }

  public void startReceiver() {
    IntentFilter mIntentFilter = new IntentFilter();
    for(String action : mActions) {
      mIntentFilter.addAction(action);
    }
    mContext.registerReceiver(mReceiver, mIntentFilter);
  }

  public static class BluetoothReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
      mListener.onReceive(context, intent);
    }
  }

  public void stopReceiver() {
    mContext.unregisterReceiver(mReceiver);
  }
}

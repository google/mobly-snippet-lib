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

package com.googlecode.android_scripting.facade.media;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaScanner;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;

import com.googlecode.android_scripting.Log;
import com.googlecode.android_scripting.facade.EventFacade;
import com.googlecode.android_scripting.facade.FacadeManager;
import com.googlecode.android_scripting.jsonrpc.RpcReceiver;
import com.googlecode.android_scripting.rpc.Rpc;
import com.googlecode.android_scripting.rpc.RpcParameter;

/**
 * Expose functionalities of MediaScanner related APIs.
 */
public class MediaScannerFacade extends RpcReceiver {

    private final Service mService;
    private final MediaScanner mScanService;
    private final EventFacade mEventFacade;
    private final MediaScannerReceiver mReceiver;

    public MediaScannerFacade(FacadeManager manager) {
        super(manager);
        mService = manager.getService();
        mScanService = new MediaScanner(mService, "external");
        mEventFacade = manager.getReceiver(EventFacade.class);
        mReceiver = new MediaScannerReceiver();
    }

    public class MediaScannerReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(action.equals(Intent.ACTION_MEDIA_SCANNER_FINISHED)) {
                Log.d("Scan finished, posting event.");
                mEventFacade.postEvent("MediaScanFinished", new Bundle());
                mService.unregisterReceiver(mReceiver);
            }
        }
    }

    @Rpc(description = "Scan external storage for media files.")
    public void mediaScanForFiles() {
        mService.sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED,
                               Uri.parse("file://" + Environment.getExternalStorageDirectory())));
        mService.registerReceiver(mReceiver,
                                  new IntentFilter(Intent.ACTION_MEDIA_SCANNER_FINISHED));
    }

    @Rpc(description = "Scan for a media file.")
    public void mediaScanForOneFile(@RpcParameter(name = "path") String path) {
        mService.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse(path)));
    }

    @Override
    public void shutdown() {
    }
}

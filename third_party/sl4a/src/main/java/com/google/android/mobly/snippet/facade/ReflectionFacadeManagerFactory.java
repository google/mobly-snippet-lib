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

package com.google.android.mobly.snippet.facade;

import android.app.Service;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import com.google.android.mobly.snippet.rpc.RpcReceiver;
import com.google.android.mobly.snippet.rpc.RpcReceiverManager;
import com.google.android.mobly.snippet.rpc.RpcReceiverManagerFactory;
import com.google.android.mobly.snippet.util.Log;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ReflectionFacadeManagerFactory implements RpcReceiverManagerFactory {
    private static final String METADATA_TAG_NAME = "mobly-snippets";

    private final Context mContext;
    private final Set<Class<? extends RpcReceiver>> mClasses;
    private final Map<Integer, RpcReceiverManager> mFacadeManagers;

    public ReflectionFacadeManagerFactory(Context context) {
        mContext = context;
        mClasses = loadRpcReceivers();
        mFacadeManagers = new HashMap<>();
    }

    @Override
    public FacadeManager create(Integer UID) {
        int sdkLevel = Build.VERSION.SDK_INT;
        FacadeManager facadeManager = new FacadeManager(sdkLevel, mContext, mClasses);
        mFacadeManagers.put(UID, facadeManager);
        return facadeManager;
    }

    @Override
    public Map<Integer, RpcReceiverManager> getRpcReceiverManagers() {
        return Collections.unmodifiableMap(mFacadeManagers);
    }

    private Set<Class<? extends RpcReceiver>> loadRpcReceivers() {
        ApplicationInfo appInfo;
        try {
            appInfo = mContext
                    .getPackageManager()
                    .getApplicationInfo(mContext.getPackageName(), PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
            throw new IllegalStateException(
                    "Failed to find ApplicationInfo with package name: "
                            + mContext.getPackageName());
        }
        Bundle metadata = appInfo.metaData;
        String snippets = metadata.getString(METADATA_TAG_NAME);
        if (snippets == null) {
            throw new IllegalStateException(
                    "AndroidManifest.xml does not contain a <metadata> tag with "
                        + "name=\"" + METADATA_TAG_NAME + "\"");
        }
        String[] snippetClassNames = snippets.split("\\s*,\\s*");
        Set<Class<? extends RpcReceiver>> receiverSet = new HashSet<>();
        for (String snippetClassName : snippetClassNames) {
            try {
                Log.i("Trying to load RpcReceiver class: " + snippetClassName);
                Class<?> snippetClass = Class.forName(snippetClassName);
                receiverSet.add((Class<? extends RpcReceiver>) snippetClass);
            } catch (ClassNotFoundException e) {
                Log.e("Failed to find class " + snippetClassName);
                throw new RuntimeException(e);
            }
        }
        if (receiverSet.isEmpty()) {
            throw new IllegalStateException("Found no subclasses of RpcReceiver.");
        }
        return receiverSet;
    }
}

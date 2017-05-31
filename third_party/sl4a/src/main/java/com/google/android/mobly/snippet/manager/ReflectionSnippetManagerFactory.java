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

package com.google.android.mobly.snippet.manager;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import com.google.android.mobly.snippet.Snippet;
import com.google.android.mobly.snippet.event.EventSnippet;
import com.google.android.mobly.snippet.schedulerpc.ScheduleRpcSnippet;
import com.google.android.mobly.snippet.util.Log;
import java.util.HashSet;
import java.util.Set;

public class ReflectionSnippetManagerFactory implements SnippetManagerFactory {
    private static final String METADATA_TAG_NAME = "mobly-snippets";
    private static ReflectionSnippetManagerFactory mInstance = null;
    private static SnippetManager mSnippetManager;

    private final Context mContext;
    private final Set<Class<? extends Snippet>> mClasses;

    protected ReflectionSnippetManagerFactory(Context context) {
        Log.i("Creating ReflectionSnippetManagerFactory instance: ");
        mContext = context;
        mClasses = loadSnippets();
    }

    public static ReflectionSnippetManagerFactory getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new ReflectionSnippetManagerFactory(context);
        }
        return mInstance;
    }

    @Override
    public SnippetManager getSnippetManager() {
        if (mSnippetManager == null) {
            synchronized (SnippetManager.class) {
                mSnippetManager = SnippetManager.getInstance(mClasses);
            }
        }
        return mSnippetManager;
    }

    private Set<Class<? extends Snippet>> loadSnippets() {
        ApplicationInfo appInfo;
        try {
            appInfo =
                    mContext.getPackageManager()
                            .getApplicationInfo(
                                    mContext.getPackageName(), PackageManager.GET_META_DATA);
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
                            + "name=\""
                            + METADATA_TAG_NAME
                            + "\"");
        }
        String[] snippetClassNames = snippets.split("\\s*,\\s*");
        Set<Class<? extends Snippet>> receiverSet = new HashSet<>();
        /** Add the event snippet class which is provided within the Snippet Lib. */
        receiverSet.add(EventSnippet.class);
        /** Add the schedule RPC snippet class which is provided within the Snippet Lib. */
        receiverSet.add(ScheduleRpcSnippet.class);
        for (String snippetClassName : snippetClassNames) {
            try {
                Log.i("Trying to load Snippet class: " + snippetClassName);
                Class<?> snippetClass = Class.forName(snippetClassName);
                receiverSet.add((Class<? extends Snippet>) snippetClass);
            } catch (ClassNotFoundException e) {
                Log.e("Failed to find class " + snippetClassName);
                throw new RuntimeException(e);
            }
        }
        if (receiverSet.isEmpty()) {
            throw new IllegalStateException("Found no subclasses of Snippet.");
        }
        return receiverSet;
    }
}

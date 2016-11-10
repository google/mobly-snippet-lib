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

package com.googlecode.android_scripting.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemProperties;

import android.util.Log;

import com.googlecode.android_scripting.Constants;

public class ScriptingLayerServiceLauncher extends Activity {

    private static final int DEBUGGABLE_BUILD = 1;

    private static final String LOG_TAG = "sl4a";
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    // Forward the intent that launched us to start the service.

    if(SystemProperties.getInt("ro.debuggable", 0) == DEBUGGABLE_BUILD) {
        Intent intent = getIntent();
        intent.setComponent(Constants.SL4A_SERVICE_COMPONENT_NAME);
        startService(intent);
        finish();
    }
    else {
        Log.e(LOG_TAG, "ERROR: Cannot to start SL4A on non-debuggable build!");
    }
  }
}

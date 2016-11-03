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

import android.content.Context;

import com.google.android.mobly.snippet.rpc.RpcMinSdk;
import com.google.android.mobly.snippet.rpc.Snippet;
import com.google.android.mobly.snippet.rpc.SnippetManager;
import com.google.android.mobly.snippet.util.SnippetLibException;
import com.google.android.mobly.snippet.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;

public class FacadeManager extends SnippetManager {

  private final Context mContext;
  private int mSdkLevel;

  public FacadeManager(int sdkLevel, Context context,
                       Collection<Class<? extends Snippet>> classList) {
    super(context, classList);
    mSdkLevel = sdkLevel;
    mContext = context;
  }

  @Override
  public Object invoke(Class<? extends Snippet> clazz, Method method, Object[] args)
      throws Exception {
    try {
      if (method.isAnnotationPresent(RpcMinSdk.class)) {
        int requiredSdkLevel = method.getAnnotation(RpcMinSdk.class).value();
        if (mSdkLevel < requiredSdkLevel) {
          throw new SnippetLibException(
                  String.format("%s requires API level %d, current level is %d",
                          method.getName(), requiredSdkLevel, mSdkLevel));
        }
      }
      return super.invoke(clazz, method, args);
    } catch (InvocationTargetException e) {
      if (e.getCause() instanceof SecurityException) {
        Log.notify(mContext, "RPC invoke failed...", mContext.getPackageName(), e.getCause()
            .getMessage());
      }
      throw e;
    }
  }
}

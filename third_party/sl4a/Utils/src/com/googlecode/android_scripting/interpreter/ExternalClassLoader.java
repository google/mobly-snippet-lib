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

package com.googlecode.android_scripting.interpreter;

import dalvik.system.DexClassLoader;

import java.util.Collection;
import java.util.Iterator;

public class ExternalClassLoader {

  public Object load(Collection<String> dexPaths, Collection<String> nativePaths, String className)
      throws Exception {
    String dexOutputDir = "/sdcard/dexoutput";
    String joinedDexPaths = join(dexPaths, ":");
    String joinedNativeLibPaths = nativePaths != null ? join(nativePaths, ":") : null;
    DexClassLoader loader =
        new DexClassLoader(joinedDexPaths, dexOutputDir, joinedNativeLibPaths, this.getClass()
            .getClassLoader());
    Class<?> classToLoad = Class.forName(className, true, loader);
    return classToLoad.newInstance();
  }

    private static String join(Collection<String> collection, String delimiter) {
        StringBuffer buffer = new StringBuffer();
        Iterator<String> iter = collection.iterator();
        while (iter.hasNext()) {
            buffer.append(iter.next());
            if (iter.hasNext()) {
                buffer.append(delimiter);
            }
        }
        return buffer.toString();
    }
}
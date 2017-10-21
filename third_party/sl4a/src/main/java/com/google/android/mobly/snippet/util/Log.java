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

package com.google.android.mobly.snippet.util;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;

public final class Log {
    public static volatile String apkLogTag = null;

    private static final String MY_CLASS_NAME = Log.class.getName();
    private static final String ANDROID_LOG_CLASS_NAME = android.util.Log.class.getName();

    // Skip the first two entries in stack trace when trying to infer the caller.
    // The first two entries are:
    // - dalvik.system.VMStack.getThreadStackTrace(Native Method)
    // - java.lang.Thread.getStackTrace(Thread.java:580)
    // The {@code getStackTrace()} function returns the stack trace at where the trace is collected
    // (inisde the JNI function {@code getThreadStackTrace()} instead of at where the {@code
    // getStackTrace()} is called (althrought this is the natual expectation).
    private static final int STACK_TRACE_WALK_START_INDEX = 2;

    private Log() {}

    public static synchronized void initLogTag(Context context) {
        if (apkLogTag != null) {
            throw new IllegalStateException("Logger should not be re-initialized");
        }
        String packageName = context.getPackageName();
        PackageManager packageManager = context.getPackageManager();
        ApplicationInfo appInfo;
        try {
            appInfo = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA);
        } catch (NameNotFoundException e) {
            throw new IllegalStateException(
                    "Failed to find ApplicationInfo with package name: " + packageName);
        }
        Bundle bundle = appInfo.metaData;
        apkLogTag = bundle.getString("mobly-log-tag");
        if (apkLogTag == null) {
            apkLogTag = packageName;
            w(
                    "AndroidManifest.xml does not contain metadata field named \"mobly-log-tag\". "
                            + "Using package name for logging instead.");
        }
    }

    private static String getTag() {
        String logTag = apkLogTag;
        if (logTag == null) {
            throw new IllegalStateException("Logging called before initLogTag()");
        }
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();

        boolean isCallerClassNameFound = false;
        String fullClassName = null;
        int lineNumber = 0;
        // Walk up the stack and look for the first class name that is neither us nor
        // android.util.Log: that's the caller.
        // Do not used hard-coded stack depth: that does not work all the time because of proguard
        // inline optimization.
        for (int i = STACK_TRACE_WALK_START_INDEX; i < stackTraceElements.length; i++) {
            StackTraceElement element = stackTraceElements[i];
            fullClassName = element.getClassName();
            if (!fullClassName.equals(MY_CLASS_NAME)
                    && !fullClassName.equals(ANDROID_LOG_CLASS_NAME)) {
                lineNumber = element.getLineNumber();
                isCallerClassNameFound = true;
                break;
            }
        }

        if (!isCallerClassNameFound) {
            // Failed to determine caller's class name, fall back the the minimal one.
            return logTag;
        } else {
            String className = fullClassName.substring(fullClassName.lastIndexOf(".") + 1);
            return logTag + "." + className + ":" + lineNumber;
        }
    }

    public static void v(String message) {
        android.util.Log.v(getTag(), message);
    }

    public static void v(String message, Throwable e) {
        android.util.Log.v(getTag(), message, e);
    }

    public static void e(Throwable e) {
        android.util.Log.e(getTag(), "Error", e);
    }

    public static void e(String message) {
        android.util.Log.e(getTag(), message);
    }

    public static void e(String message, Throwable e) {
        android.util.Log.e(getTag(), message, e);
    }

    public static void w(Throwable e) {
        android.util.Log.w(getTag(), "Warning", e);
    }

    public static void w(String message) {
        android.util.Log.w(getTag(), message);
    }

    public static void w(String message, Throwable e) {
        android.util.Log.w(getTag(), message, e);
    }

    public static void d(String message) {
        android.util.Log.d(getTag(), message);
    }

    public static void d(String message, Throwable e) {
        android.util.Log.d(getTag(), message, e);
    }

    public static void i(String message) {
        android.util.Log.i(getTag(), message);
    }

    public static void i(String message, Throwable e) {
        android.util.Log.i(getTag(), message, e);
    }
}

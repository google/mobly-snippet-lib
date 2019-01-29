/*
 * Copyright (C) 2017 Google Inc.
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

package com.google.android.mobly.snippet.example4;

import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.content.Intent;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.Until;
import com.google.android.mobly.snippet.Snippet;
import com.google.android.mobly.snippet.rpc.Rpc;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;

/**
 * Demonstrates how to drive an app using UIAutomator without access to the app's source code or
 * classpath.
 *
 * <p>Drives the Espresso example app from ex2 without instrumenting it.
 */
public class UiAutomatorSnippet implements Snippet {
    private static final class UiAutomatorSnippetException extends Exception {
        private static final long serialVersionUID = 1;

        public UiAutomatorSnippetException(String message) {
            super(message);
        }
    }

    private static final String MAIN_PACKAGE = "com.google.android.mobly.snippet.example2";
    private static final int LAUNCH_TIMEOUT = 5000;

    private final Context mContext;
    private final UiDevice mDevice;

    public UiAutomatorSnippet() {
        mContext = InstrumentationRegistry.getInstrumentation().getContext();
        mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
    }

    @Rpc(description="Opens the main activity of the app")
    public void startMainActivity() throws UiAutomatorSnippetException {
        // Send the launch intent
        Intent intent = mContext.getPackageManager().getLaunchIntentForPackage(MAIN_PACKAGE);
        if (intent == null) {
            throw new UiAutomatorSnippetException(
                "Unable to create launch intent for " + MAIN_PACKAGE + "; is the app installed?");
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        mContext.startActivity(intent);

        // Wait for the app to appear
        mDevice.wait(Until.hasObject(By.pkg(MAIN_PACKAGE).depth(0)), LAUNCH_TIMEOUT);
    }

    @Rpc(description="Pushes the main app button, and checks the label if this is the first time.")
    public void pushMainButton(boolean checkFirstRun) {
        if (checkFirstRun) {
            assertEquals(
                "Hello World!",
                // Example of finding object by id.
                mDevice.findObject(By.res(MAIN_PACKAGE, "main_text_view")).getText());
        }
        // Example of finding a button by text. Finding by ID is also possible, as above.
        UiObject2 button = mDevice.findObject(By.text("PUSH THE BUTTON!"));
        button.click();
        if (checkFirstRun) {
            assertEquals(
                "Button pressed 1 times",
                mDevice.findObject(By.res(MAIN_PACKAGE, "main_text_view")).getText());
        }
    }

    @Rpc(description="Perform a UIAutomator dump")
    public String uiautomatorDump() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            mDevice.dumpWindowHierarchy(baos);
            byte[] dumpBytes = baos.toByteArray();
            String dumpStr = new String(dumpBytes, Charset.forName("UTF-8"));
            return dumpStr;
        } finally {
            baos.close();
        }
    }

    @Override
    public void shutdown() throws IOException {
        mDevice.executeShellCommand("am force-stop " + MAIN_PACKAGE);
    }
}

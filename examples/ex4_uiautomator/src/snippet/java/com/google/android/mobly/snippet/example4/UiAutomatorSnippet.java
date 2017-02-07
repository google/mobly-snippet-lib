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

package com.google.android.mobly.snippet.example4;

import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.content.Intent;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SdkSuppress;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject2;
import android.support.test.uiautomator.Until;
import com.google.android.mobly.snippet.Snippet;
import com.google.android.mobly.snippet.rpc.Rpc;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;

@SdkSuppress(minSdkVersion = 18)
public class UiAutomatorSnippet implements Snippet {
    private static final String MAIN_PACKAGE = MainActivity.class.getPackage().getName();
    private static final int LAUNCH_TIMEOUT = 5000;

    private UiDevice mDevice;

    public UiAutomatorSnippet() {
        mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
    }

    @Rpc(description="Opens the main activity of the app")
    public void startMainActivity() {
        // Send the launch intent
        Context context = InstrumentationRegistry.getContext();
        Intent intent = context.getPackageManager().getLaunchIntentForPackage(MAIN_PACKAGE);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);

        // Wait for the app to appear
        mDevice.wait(Until.hasObject(By.pkg(MAIN_PACKAGE).depth(0)), LAUNCH_TIMEOUT);
    }

    @Rpc(description="Clicks the button for the first time and checks the label change")
    public void firstClick() {
        assertEquals(
            "Hello World!",
            mDevice.findObject(By.res(MAIN_PACKAGE, "main_text_view")).getText());
        click();
        assertEquals(
            "Button pressed 1 times",
            mDevice.findObject(By.res(MAIN_PACKAGE, "main_text_view")).getText());
    }

    @Rpc(description="Clicks the button")
    public void click() {
        UiObject2 button = mDevice.findObject(By.text("PUSH THE BUTTON!"));
        button.click();
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
    public void shutdown() {}
}

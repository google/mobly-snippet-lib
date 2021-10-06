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
package com.google.android.mobly.snippet;

import android.app.Instrumentation;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import androidx.test.runner.AndroidJUnitRunner;
import com.google.android.mobly.snippet.rpc.AndroidProxy;
import com.google.android.mobly.snippet.util.EmptyTestClass;
import com.google.android.mobly.snippet.util.Log;
import com.google.android.mobly.snippet.util.NotificationIdFactory;
import java.io.IOException;
import java.net.SocketException;
import java.util.Locale;

/**
 * A launcher that starts the snippet server as an instrumentation so that it has access to the
 * target app's context.
 *
 * <p>We have to extend some subclass of {@link androidx.test.runner.AndroidJUnitRunner} because
 * snippets are launched with 'am instrument', and snippet APKs need to access {@link
 * androidx.test.platform.app.InstrumentationRegistry}.
 *
 * <p>The launch and communication protocol between snippet and client is versionated and reported
 * as follows:
 *
 * <ul>
 *   <li>v0 (not reported):
 *       <ul>
 *         <li>Launch as Instrumentation with SnippetRunner.
 *         <li>No protocol-specific messages reported through instrumentation output.
 *         <li>'stop' action prints 'OK (0 tests)'
 *         <li>'start' action prints nothing.
 *       </ul>
 *   <li>v1.0: New instrumentation output added to track bringup process
 *       <ul>
 *         <li>"SNIPPET START, PROTOCOL &lt;major&gt; &lt;minor&gt;" upon snippet start
 *         <li>"SNIPPET SERVING, PORT &lt;port&gt;" once server is ready
 *       </ul>
 * </ul>
 */
public class SnippetRunner extends AndroidJUnitRunner {

    /**
     * Major version of the launch and communication protocol.
     *
     * <p>Incrementing this means that compatibility with clients using the older version is broken.
     * Avoid breaking compatibility unless there is no other choice.
     */
    public static final int PROTOCOL_MAJOR_VERSION = 1;

    /**
     * Minor version of the launch and communication protocol.
     *
     * <p>Increment this when new features are added to the launch and communication protocol that
     * are backwards compatible with the old protocol and don't break existing clients.
     */
    public static final int PROTOCOL_MINOR_VERSION = 0;

    private static final String ARG_ACTION = "action";
    private static final String ARG_PORT = "port";

    /**
     * Values needed to create a notification channel. This applies to versions > O (26).
     */
    private static final String NOTIFICATION_CHANNEL_ID = "msl_channel";
    private static final String NOTIFICATION_CHANNEL_DESC = "Channel reserved for mobly-snippet-lib.";
    private static final CharSequence NOTIFICATION_CHANNEL_NAME = "msl";

    private enum Action {
        START,
        STOP
    };

    private static final int NOTIFICATION_ID = NotificationIdFactory.create();

    private Bundle mArguments;
    private NotificationManager mNotificationManager;
    private Notification mNotification;

    @Override
    public void onCreate(Bundle arguments) {
        mArguments = arguments;

        // First-run static setup
        Log.initLogTag(getContext());

        // First order of business is to report HELLO to instrumentation output.
        sendString(
                "SNIPPET START, PROTOCOL " + PROTOCOL_MAJOR_VERSION + " " + PROTOCOL_MINOR_VERSION);

        // Prevent this runner from triggering any real JUnit tests in the snippet by feeding it a
        // hardcoded empty test class.
        mArguments.putString("class", EmptyTestClass.class.getCanonicalName());
        mNotificationManager =
                (NotificationManager)
                        getTargetContext().getSystemService(Context.NOTIFICATION_SERVICE);
        super.onCreate(mArguments);
    }

    @Override
    public void onStart() {
        String actionStr = mArguments.getString(ARG_ACTION);
        if (actionStr == null) {
            throw new IllegalArgumentException("\"--e action <action>\" was not specified");
        }
        Action action = Action.valueOf(actionStr.toUpperCase(Locale.ROOT));
        switch (action) {
            case START:
                String servicePort = mArguments.getString(ARG_PORT);
                int port = 0 /* auto chosen */;
                if (servicePort != null) {
                    port = Integer.parseInt(servicePort);
                }
                startServer(port);
                break;
            case STOP:
                mNotificationManager.cancel(NOTIFICATION_ID);
                mNotificationManager.cancelAll();
                super.onStart();
        }
    }

    private void startServer(int port) {
        AndroidProxy androidProxy = new AndroidProxy(getContext());
        try {
            androidProxy.startLocal(port);
        } catch (SocketException e) {
            if ("Permission denied".equals(e.getMessage())) {
                throw new RuntimeException(
                        "Failed to start server. No permission to create a socket. Does the *MAIN* "
                                + "app manifest declare the INTERNET permission?",
                        e);
            }
            throw new RuntimeException("Failed to start server", e);
        } catch (IOException e) {
            throw new RuntimeException("Failed to start server", e);
        }
        createNotification();
        int actualPort = androidProxy.getPort();
        sendString("SNIPPET SERVING, PORT " + actualPort);
        Log.i("Snippet server started for process " + Process.myPid() + " on port " + actualPort);
    }

    @SuppressWarnings("deprecation") // Depreciated calls needed for versions < O (26)
    private void createNotification() {
        Notification.Builder builder;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            builder = new Notification.Builder(getTargetContext());
            builder.setSmallIcon(android.R.drawable.btn_star)
                    .setTicker(null)
                    .setWhen(System.currentTimeMillis())
                    .setContentTitle("Snippet Service");
            mNotification = builder.getNotification();
        } else {
            // Create a new channel for notifications. Needed for versions >= O
            NotificationChannel channel = new NotificationChannel(
                    NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription(NOTIFICATION_CHANNEL_DESC);
            mNotificationManager.createNotificationChannel(channel);

            // Build notification
            builder = new Notification.Builder(getTargetContext(), NOTIFICATION_CHANNEL_ID);
            builder.setSmallIcon(android.R.drawable.btn_star)
                    .setTicker(null)
                    .setWhen(System.currentTimeMillis())
                    .setContentTitle("Snippet Service");
            mNotification = builder.build();
        }
        mNotification.flags = Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;
        mNotificationManager.notify(NOTIFICATION_ID, mNotification);
    }

    private void sendString(String string) {
        Log.i("Sending protocol message: " + string);
        Bundle bundle = new Bundle();
        bundle.putString(Instrumentation.REPORT_KEY_STREAMRESULT, string + "\n");
        sendStatus(0, bundle);
    }
}

package com.google.android.mobly.snippet;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.test.runner.AndroidJUnitRunner;

import com.google.android.mobly.snippet.service.SnippetService;
import com.google.android.mobly.snippet.util.Log;

/**
 * A launcher that starts the snippet server as an instrumentation so that it has access to the
 * target app's context.
 *
 * It is written this way to be compatible with 'am instrument'.
 */
public class SnippetRunner extends AndroidJUnitRunner {
    private static final String ARG_PORT = "port";

    private Context mContext;
    private int mServicePort;

    @Override
    public void onCreate(Bundle arguments) {
        mContext = getContext();
        String servicePort = arguments.getString(ARG_PORT);
        if (servicePort == null) {
            throw new IllegalArgumentException("\"--e port <port>\" was not specified");
        }
        mServicePort = Integer.parseInt(servicePort);
        super.onCreate(arguments);
    }

    @Override
    public void onStart() {
        Intent intent = new Intent(mContext, SnippetService.class);
        intent.setAction(Constants.ACTION_LAUNCH_SERVER);
        intent.putExtra(Constants.EXTRA_SERVICE_PORT, mServicePort);
        mContext.startService(intent);
        Log.e("Started up the snippet server on port " + mServicePort);

        // Wait forever. Once our instrumentation returns, everything related to the app is killed.
        while (true) {
            try {
                Thread.sleep(24L * 60 * 60 * 1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}

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

package com.googlecode.android_scripting.facade;

import android.app.Service;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CallLog.Calls;
import android.util.Log;

import com.googlecode.android_scripting.facade.EventFacade;
import com.googlecode.android_scripting.jsonrpc.RpcReceiver;
import com.googlecode.android_scripting.rpc.Rpc;
import com.googlecode.android_scripting.rpc.RpcOptional;
import com.googlecode.android_scripting.rpc.RpcParameter;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Provides access to contacts related functionality.
 */
public class CallLogFacade extends RpcReceiver {
    private static final String TAG = "CallLogFacade";
    private static final Uri CONTACTS_URI = Uri.parse("content://contacts/people");

    // Fields for use in messages from SL4A scripts
    private static final String JSON_TYPE = "type";
    private static final String JSON_NUMBER = "number";
    private static final String JSON_TIME = "time";

    private final ContentResolver mContentResolver;
    private final Service mService;
    private final CallLogStatusReceiver mCallLogStatusReceiver;
    private final EventFacade mEventFacade;

    public CallLogFacade(FacadeManager manager) {
        super(manager);
        mService = manager.getService();
        mContentResolver = mService.getContentResolver();
        mCallLogStatusReceiver = new CallLogStatusReceiver();
        mContentResolver.registerContentObserver(Calls.CONTENT_URI, true, mCallLogStatusReceiver);
        mEventFacade = manager.getReceiver(EventFacade.class);
    }

    private Uri buildUri(Integer id) {
        Uri uri = ContentUris.withAppendedId(Calls.CONTENT_URI, id);
        return uri;
    }

    @Rpc(description = "Erase all contacts in phone book.")
    public void callLogsEraseAll() {
        Log.d(TAG, "callLogsEraseAll");
        mContentResolver.delete(Calls.CONTENT_URI, null, null);
        return;
    }

    @Rpc(description = "Adds a list of calls to call log.", returns = "Number of calls added")
    public Integer callLogsPut(@RpcParameter(name = "logs") JSONObject log) throws JSONException {
        Log.d(TAG, "callLogsPut");
        int startingCount = callLogGetCount();
        ContentValues values = new ContentValues();
        String type = log.getString(JSON_TYPE);
        String number = log.getString(JSON_NUMBER);
        String time = log.getString(JSON_TIME);
        values.put(Calls.TYPE, type);
        values.put(Calls.NUMBER, number);
        values.put(Calls.DATE, time);
        mContentResolver.insert(Calls.CONTENT_URI, values);

        return callLogGetCount() - startingCount;
    }

    @Rpc(description = "Returns a List of all contacts.", returns = "a List of contacts as Maps")
    public List<JSONObject> callLogsGet(@RpcParameter(name = "type") String type)
            throws JSONException {
        Log.d(TAG, "callLogsGet");
        List<JSONObject> result = new ArrayList<JSONObject>();
        String[] query = new String[] {Calls.NUMBER, Calls.DATE, Calls.TYPE};

        Cursor cursor =
                mContentResolver.query(
                        Calls.CONTENT_URI,
                        query,
                        Calls.TYPE + "= " + type,
                        null,
                        Calls.DATE + ", " + Calls.NUMBER);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                JSONObject message = new JSONObject();
                for (int i = 0; i < query.length; i++) {
                    String key = query[i];
                    String value = cursor.getString(cursor.getColumnIndex(key));
                    message.put(key, value);
                }
                result.add(message);
            }
            cursor.close();
        }
        return result;
    }

    @Rpc(description = "Returns the number of contacts.")
    public Integer callLogGetCount() {
        Log.d(TAG, "callLogGetCount");
        Integer result = 0;
        Cursor cursor = mContentResolver.query(Calls.CONTENT_URI, null, null, null, null);
        if (cursor != null) {
            result = cursor.getCount();
            cursor.close();
        }
        return result;
    }

    private class CallLogStatusReceiver extends ContentObserver {
        public CallLogStatusReceiver() {
            super(null);
        }

        public void onChange(boolean updated) {
            Log.d(TAG, "CallLogStatusReceiver:onChange");
            mEventFacade.postEvent("CallLogChanged", null);
        }
    }

    @Override
    public void shutdown() {
        mContentResolver.unregisterContentObserver(mCallLogStatusReceiver);
    }
}

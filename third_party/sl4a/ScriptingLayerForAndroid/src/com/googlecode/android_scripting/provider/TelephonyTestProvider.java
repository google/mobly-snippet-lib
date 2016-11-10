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

package com.googlecode.android_scripting.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.text.TextUtils;

import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;

import com.googlecode.android_scripting.Log;

/**
 * A simple provider to send MMS PDU to platform MMS service
 */
public class TelephonyTestProvider extends ContentProvider {

    public static final String AUTHORITY = "telephonytestauthority";

    private final boolean DBG = false;

    @Override
    public boolean onCreate() {
        if(DBG) Log.d("TelephonTestProvider Successfully created!");
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        // Not supported
        return null;
    }

    @Override
    public String getType(Uri uri) {
        // Not supported
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        // Not supported
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // Not supported
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // Not supported
        return 0;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String fileMode) throws FileNotFoundException {
        Log.d(String.format("Entered ParcelFileDescriptor: Uri(%s), Mode(%s)", uri.toString(),
                fileMode));
        File file = new File(getContext().getCacheDir(), uri.getPath());
        file.setReadable(true);

        Log.d(String.format("Looking for file at %s", getContext().getCacheDir() + uri.getPath()));
        int mode = (TextUtils.equals(fileMode, "r") ? ParcelFileDescriptor.MODE_READ_ONLY :
                ParcelFileDescriptor.MODE_WRITE_ONLY
                | ParcelFileDescriptor.MODE_TRUNCATE
                | ParcelFileDescriptor.MODE_CREATE);

        try {
            ParcelFileDescriptor descriptor = ParcelFileDescriptor.open(file, mode);

            if(DBG) {
                try {
                    BufferedReader br = new BufferedReader(new
                            FileReader(descriptor.getFileDescriptor()));
                    String line = null;
                    while ((line = br.readLine()) != null) {
                        Log.d("MMS:" + line);
                    }
                } catch (IOException e) {
                }
            }

            return descriptor;
        } catch (FileNotFoundException fnf) {
            Log.e(fnf);
            return null;
        }
    }
}

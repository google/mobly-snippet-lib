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

package com.googlecode.android_scripting.facade.media;

import android.content.Intent;
import android.media.session.MediaSession;
import android.os.Bundle;
import android.view.KeyEvent;

import com.googlecode.android_scripting.Log;
import com.googlecode.android_scripting.facade.EventFacade;

public class MediaButtonCallback extends MediaSession.Callback {
    private final EventFacade mEventFacade;
    public MediaButtonCallback(EventFacade eventFacade) {
        this.mEventFacade = eventFacade;
    }
    private void handleKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        Log.d("Received ACTION_DOWN with keycode " + keyCode);
        Bundle msg = new Bundle();
        if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY) {
            msg.putString("ButtonPressed", "Play");
        } else if (keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE) {
            msg.putString("ButtonPressed", "Pause");
        } else if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
            msg.putString("ButtonPressed", "PlayPause");
        } else if (keyCode == KeyEvent.KEYCODE_MEDIA_STOP) {
            msg.putString("ButtonPressed", "Stop");
        } else if (keyCode == KeyEvent.KEYCODE_MEDIA_NEXT) {
            msg.putString("ButtonPressed", "Next");
        } else if (keyCode == KeyEvent.KEYCODE_MEDIA_PREVIOUS) {
            msg.putString("ButtonPressed", "Previous");
        } else if (keyCode == KeyEvent.KEYCODE_MEDIA_FAST_FORWARD) {
            msg.putString("ButtonPressed", "Forward");
        } else if (keyCode == KeyEvent.KEYCODE_MEDIA_REWIND) {
            msg.putString("ButtonPressed", "Rewind");
        }
        Log.d("Sending MediaButton event with ButtonPressed value "
              + msg.getString("ButtonPressed"));
        this.mEventFacade.postEvent("MediaButton", msg);
    }

    @Override
    public boolean onMediaButtonEvent(Intent mediaButtonIntent) {
        String action = mediaButtonIntent.getAction();
        Log.d("Received intent with action " + action);
        if (action.equals(Intent.ACTION_MEDIA_BUTTON)) {
            KeyEvent event = (KeyEvent) mediaButtonIntent
                                        .getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            int keyAction = event.getAction();
            Log.d("Received KeyEvent with action " + keyAction);
            if (keyAction == KeyEvent.ACTION_DOWN) {
                handleKeyEvent(event);
            } else if (keyAction == KeyEvent.ACTION_UP) {
                handleKeyEvent(event);
            }
            return true;
        }
        return super.onMediaButtonEvent(mediaButtonIntent);
    }
}
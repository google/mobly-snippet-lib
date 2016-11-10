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

package com.googlecode.android_scripting.facade.telephony;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import android.telecom.Call;
import android.telecom.Call.Details;
import android.telecom.CallAudioState;
import android.telecom.Conference;
import android.telecom.Connection;
import android.telecom.ConnectionService;
import android.telecom.InCallService;
import android.telecom.Phone;
import android.telecom.TelecomManager;
import android.telecom.VideoProfile;
import android.telecom.VideoProfile.CameraCapabilities;

import com.googlecode.android_scripting.Log;

import com.googlecode.android_scripting.facade.EventFacade;

public class InCallServiceImpl extends InCallService {

    private static InCallServiceImpl sService = null;

    public static InCallServiceImpl getService() {
        return sService;
    }

    public static class CallListener {

        public static final int LISTEN_CALL_ADDED   = 1 << 0;
        public static final int LISTEN_CALL_REMOVED = 1 << 1;
        public static final int LISTEN_ALL = LISTEN_CALL_ADDED | LISTEN_CALL_REMOVED;

        private static int sListenedEvents = 0;

        public static void startListeningForEvent( int event ) {
            sListenedEvents |= event & LISTEN_ALL;
        }

        public static void stopListeningForEvent( int event ) {
            sListenedEvents &= ~(event & LISTEN_ALL);
        }

        public static void onCallAdded(String callId, Call call) {
            Log.d("CallListener:onCallAdded()");
            if ((sListenedEvents & LISTEN_CALL_ADDED)
                    == LISTEN_CALL_ADDED) {
                servicePostEvent(TelephonyConstants.EventTelecomCallAdded,
                        new CallEvent<Call>(callId, call));
            }
        }

        public static void onCallRemoved(String callId, Call call) {
            Log.d("CallListener:onCallRemoved()");
            if ((sListenedEvents & LISTEN_CALL_REMOVED)
                    == LISTEN_CALL_REMOVED) {
                servicePostEvent(TelephonyConstants.EventTelecomCallRemoved,
                        new CallEvent<Call>(callId, call));
            }
        }
    };


    private static Object mLock = new Object();

    // Provides a return value for getCallState when no call is active
    public static final int STATE_INVALID = -1;

    // Provides a return value for getCallQuality when input is invalid
    public static final int QUALITY_INVALID = -1;

    // Provides a return value for getAudioRoute when input is invalid
    public static final int INVALID_AUDIO_ROUTE = -1;

    public static final int VIDEO_STATE_AUDIO_ONLY = VideoProfile.STATE_AUDIO_ONLY;

    public static final int VIDEO_STATE_TX_ENABLED = VideoProfile.STATE_TX_ENABLED;

    public static final int VIDEO_STATE_RX_ENABLED = VideoProfile.STATE_RX_ENABLED;

    public static final int VIDEO_STATE_BIDIRECTIONAL = VideoProfile.STATE_BIDIRECTIONAL;

    public static final int VIDEO_STATE_TX_PAUSED =
            VideoProfile.STATE_TX_ENABLED | VideoProfile.STATE_PAUSED;

    public static final int VIDEO_STATE_RX_PAUSED =
            VideoProfile.STATE_RX_ENABLED | VideoProfile.STATE_PAUSED;

    public static final int VIDEO_STATE_BIDIRECTIONAL_PAUSED =
            VideoProfile.STATE_BIDIRECTIONAL | VideoProfile.STATE_PAUSED;

    // Container class to return the call ID along with the event
    public static class CallEvent<EventType> {

        private final String mCallId;
        private final EventType mEvent;

        CallEvent(String callId, EventType event) {
            mCallId = callId;
            mEvent = event;
        }

        public String getCallId() {
            return mCallId;
        }

        public EventType getEvent() {
            return mEvent;
        }
    }

    // Currently the same as a call event... here for future use
    public static class VideoCallEvent<EventType> extends CallEvent<EventType> {
        VideoCallEvent(String callId, EventType event) {
            super(callId, event);
        }
    }

    private class CallCallback extends Call.Callback {

        // Invalid video state (valid >= 0)
        public static final int STATE_INVALID = InCallServiceImpl.STATE_INVALID;

        public static final int EVENT_INVALID = -1;
        public static final int EVENT_NONE = 0;
        public static final int EVENT_STATE_CHANGED = 1 << 0;
        public static final int EVENT_PARENT_CHANGED = 1 << 1;
        public static final int EVENT_CHILDREN_CHANGED = 1 << 2;
        public static final int EVENT_DETAILS_CHANGED = 1 << 3;
        public static final int EVENT_CANNED_TEXT_RESPONSES_LOADED = 1 << 4;
        public static final int EVENT_POST_DIAL_WAIT = 1 << 5;
        public static final int EVENT_VIDEO_CALL_CHANGED = 1 << 6;
        public static final int EVENT_CALL_DESTROYED = 1 << 7;
        public static final int EVENT_CONFERENCABLE_CALLS_CHANGED = 1 << 8;

        public static final int EVENT_ALL = EVENT_STATE_CHANGED |
                EVENT_PARENT_CHANGED |
                EVENT_CHILDREN_CHANGED |
                EVENT_DETAILS_CHANGED |
                EVENT_CANNED_TEXT_RESPONSES_LOADED |
                EVENT_POST_DIAL_WAIT |
                EVENT_VIDEO_CALL_CHANGED |
                EVENT_DETAILS_CHANGED |
                EVENT_CALL_DESTROYED |
                EVENT_CONFERENCABLE_CALLS_CHANGED;

        private int mEvents;
        private String mCallId;

        public CallCallback(String callId, int events) {
            super();
            mEvents = events & EVENT_ALL;
            mCallId = callId;
        }

        public void startListeningForEvents(int events) {
            mEvents |= events & EVENT_ALL;
        }

        public void stopListeningForEvents(int events) {
            mEvents &= ~(events & EVENT_ALL);
        }

        @Override
        public void onStateChanged(
                Call call, int state) {
            Log.d("CallCallback:onStateChanged()");
            if ((mEvents & EVENT_STATE_CHANGED)
                    == EVENT_STATE_CHANGED) {
                servicePostEvent(TelephonyConstants.EventTelecomCallStateChanged,
                        new CallEvent<String>(mCallId, getCallStateString(state)));
            }
        }

        @Override
        public void onParentChanged(
                Call call, Call parent) {
            Log.d("CallCallback:onParentChanged()");
            if ((mEvents & EVENT_PARENT_CHANGED)
                    == EVENT_PARENT_CHANGED) {
                servicePostEvent(TelephonyConstants.EventTelecomCallParentChanged,
                        new CallEvent<String>(mCallId, getCallId(parent)));
            }
        }

        @Override
        public void onChildrenChanged(
                Call call, List<Call> children) {
            Log.d("CallCallback:onChildrenChanged()");

            if ((mEvents & EVENT_CHILDREN_CHANGED)
                    == EVENT_CHILDREN_CHANGED) {
                List<String> childList = new ArrayList<String>();

                for (Call child : children) {
                    childList.add(getCallId(child));
                }
                servicePostEvent(TelephonyConstants.EventTelecomCallChildrenChanged,
                        new CallEvent<List<String>>(mCallId, childList));
            }
        }

        @Override
        public void onDetailsChanged(
                Call call, Details details) {
            Log.d("CallCallback:onDetailsChanged()");

            if ((mEvents & EVENT_DETAILS_CHANGED)
                    == EVENT_DETAILS_CHANGED) {
                servicePostEvent(TelephonyConstants.EventTelecomCallDetailsChanged,
                        new CallEvent<Details>(mCallId, details));
            }
        }

        @Override
        public void onCannedTextResponsesLoaded(
                Call call, List<String> cannedTextResponses) {
            Log.d("CallCallback:onCannedTextResponsesLoaded()");
            if ((mEvents & EVENT_CANNED_TEXT_RESPONSES_LOADED)
                    == EVENT_CANNED_TEXT_RESPONSES_LOADED) {
                servicePostEvent(TelephonyConstants.EventTelecomCallCannedTextResponsesLoaded,
                        new CallEvent<List<String>>(mCallId, cannedTextResponses));
            }
        }

        @Override
        public void onPostDialWait(
                Call call, String remainingPostDialSequence) {
            Log.d("CallCallback:onPostDialWait()");
            if ((mEvents & EVENT_POST_DIAL_WAIT)
                    == EVENT_POST_DIAL_WAIT) {
                servicePostEvent(TelephonyConstants.EventTelecomCallPostDialWait,
                        new CallEvent<String>(mCallId, remainingPostDialSequence));
            }
        }

        @Override
        public void onVideoCallChanged(
                Call call, InCallService.VideoCall videoCall) {

            /*
             * There is a race condition such that the lifetime of the VideoCall is not aligned with
             * the lifetime of the underlying call object. We are using the onVideoCallChanged
             * method as a way of determining the lifetime of the VideoCall object rather than
             * onCallAdded/onCallRemoved.
             */
            Log.d("CallCallback:onVideoCallChanged()");

            if (call != null) {
                String callId = getCallId(call);
                CallContainer cc = mCallContainerMap.get(callId);
                if (cc == null) {
                    Log.d(String.format("Call container returned null for callId %s", callId));
                }
                else {
                    synchronized (mLock) {
                        if (videoCall == null) {
                            Log.d("Yo dawg, I heard you like null video calls.");
                            // Try and see if the videoCall has been added/changed after firing the
                            // callback
                            // This probably won't work.
                            videoCall = call.getVideoCall();
                        }
                        if (cc.getVideoCall() != videoCall) {
                            if (videoCall == null) {
                                // VideoCall object deleted
                                cc.updateVideoCall(null, null);
                                Log.d("Removing video call from call.");
                            }
                            else if (cc.getVideoCall() != null) {
                                // Somehow we have a mismatched VideoCall ID!
                                Log.d("Mismatched video calls for same call ID.");
                            }
                            else {
                                Log.d("Huzzah, we have a video call!");

                                VideoCallCallback videoCallCallback =
                                        new VideoCallCallback(callId, VideoCallCallback.EVENT_NONE);

                                videoCall.registerCallback(videoCallCallback);

                                cc.updateVideoCall(
                                        videoCall,
                                        videoCallCallback);
                            }
                        }
                        else {
                            Log.d("Change to existing video call.");
                        }

                    }
                }
            }
            else {
                Log.d("passed null call pointer to call callback");
            }

            if ((mEvents & EVENT_VIDEO_CALL_CHANGED)
                    == EVENT_VIDEO_CALL_CHANGED) {
                // TODO: b/26273778 Need to determine what to return;
                // probably not the whole video call
                servicePostEvent(TelephonyConstants.EventTelecomCallVideoCallChanged,
                        new CallEvent<String>(mCallId, videoCall.toString()));
            }
        }

        @Override
        public void onCallDestroyed(Call call) {
            Log.d("CallCallback:onCallDestroyed()");

            if ((mEvents & EVENT_CALL_DESTROYED)
                    == EVENT_CALL_DESTROYED) {
                servicePostEvent(TelephonyConstants.EventTelecomCallDestroyed,
                        new CallEvent<Call>(mCallId, call));
            }
        }

        @Override
        public void onConferenceableCallsChanged(
                Call call, List<Call> conferenceableCalls) {
            Log.d("CallCallback:onConferenceableCallsChanged()");

            if ((mEvents & EVENT_CONFERENCABLE_CALLS_CHANGED)
                    == EVENT_CONFERENCABLE_CALLS_CHANGED) {
                List<String> confCallList = new ArrayList<String>();
                for (Call cc : conferenceableCalls) {
                    confCallList.add(getCallId(cc));
                }
                servicePostEvent(TelephonyConstants.EventTelecomCallConferenceableCallsChanged,
                        new CallEvent<List<String>>(mCallId, confCallList));
            }
        }
    }

    private class VideoCallCallback extends InCallService.VideoCall.Callback {

        public static final int EVENT_INVALID = -1;
        public static final int EVENT_NONE = 0;
        public static final int EVENT_SESSION_MODIFY_REQUEST_RECEIVED = 1 << 0;
        public static final int EVENT_SESSION_MODIFY_RESPONSE_RECEIVED = 1 << 1;
        public static final int EVENT_SESSION_EVENT = 1 << 2;
        public static final int EVENT_PEER_DIMENSIONS_CHANGED = 1 << 3;
        public static final int EVENT_VIDEO_QUALITY_CHANGED = 1 << 4;
        public static final int EVENT_DATA_USAGE_CHANGED = 1 << 5;
        public static final int EVENT_CAMERA_CAPABILITIES_CHANGED = 1 << 6;
        public static final int EVENT_ALL =
                EVENT_SESSION_MODIFY_REQUEST_RECEIVED |
                EVENT_SESSION_MODIFY_RESPONSE_RECEIVED |
                EVENT_SESSION_EVENT |
                EVENT_PEER_DIMENSIONS_CHANGED |
                EVENT_VIDEO_QUALITY_CHANGED |
                EVENT_DATA_USAGE_CHANGED |
                EVENT_CAMERA_CAPABILITIES_CHANGED;

        private String mCallId;
        private int mEvents;

        public VideoCallCallback(String callId, int listeners) {

            mCallId = callId;
            mEvents = listeners & EVENT_ALL;
        }

        public void startListeningForEvents(int events) {
            Log.d(String.format(
                    "VideoCallCallback(%s):startListeningForEvents(%x): events:%x",
                    mCallId, events, mEvents));

            mEvents |= events & EVENT_ALL;

        }

        public void stopListeningForEvents(int events) {
            mEvents &= ~(events & EVENT_ALL);
        }

        @Override
        public void onSessionModifyRequestReceived(VideoProfile videoProfile) {
            Log.d(String.format("VideoCallCallback(%s):onSessionModifyRequestReceived()", mCallId));

            if ((mEvents & EVENT_SESSION_MODIFY_REQUEST_RECEIVED)
                    == EVENT_SESSION_MODIFY_REQUEST_RECEIVED) {
                servicePostEvent(TelephonyConstants.EventTelecomVideoCallSessionModifyRequestReceived,
                        new VideoCallEvent<VideoProfile>(mCallId, videoProfile));
            }

        }

        @Override
        public void onSessionModifyResponseReceived(int status,
                VideoProfile requestedProfile, VideoProfile responseProfile) {
            Log.d("VideoCallCallback:onSessionModifyResponseReceived()");

            if ((mEvents & EVENT_SESSION_MODIFY_RESPONSE_RECEIVED)
                    == EVENT_SESSION_MODIFY_RESPONSE_RECEIVED) {

                HashMap<String, VideoProfile> smrrInfo = new HashMap<String, VideoProfile>();

                smrrInfo.put("RequestedProfile", requestedProfile);
                smrrInfo.put("ResponseProfile", responseProfile);

                servicePostEvent(TelephonyConstants.EventTelecomVideoCallSessionModifyResponseReceived,
                        new VideoCallEvent<HashMap<String, VideoProfile>>(mCallId, smrrInfo));
            }
        }

        @Override
        public void onCallSessionEvent(int event) {
            Log.d("VideoCallCallback:onCallSessionEvent()");

            String eventString = getVideoCallSessionEventString(event);

            if ((mEvents & EVENT_SESSION_EVENT)
                    == EVENT_SESSION_EVENT) {
                servicePostEvent(TelephonyConstants.EventTelecomVideoCallSessionEvent,
                        new VideoCallEvent<String>(mCallId, eventString));
            }
        }

        @Override
        public void onPeerDimensionsChanged(int width, int height) {
            Log.d("VideoCallCallback:onPeerDimensionsChanged()");

            if ((mEvents & EVENT_PEER_DIMENSIONS_CHANGED)
                    == EVENT_PEER_DIMENSIONS_CHANGED) {

                HashMap<String, Integer> temp = new HashMap<String, Integer>();
                temp.put("Width", width);
                temp.put("Height", height);

                servicePostEvent(TelephonyConstants.EventTelecomVideoCallPeerDimensionsChanged,
                        new VideoCallEvent<HashMap<String, Integer>>(mCallId, temp));
            }
        }

        @Override
        public void onVideoQualityChanged(int videoQuality) {
            Log.d("VideoCallCallback:onVideoQualityChanged()");

            if ((mEvents & EVENT_VIDEO_QUALITY_CHANGED)
                    == EVENT_VIDEO_QUALITY_CHANGED) {
                servicePostEvent(TelephonyConstants.EventTelecomVideoCallVideoQualityChanged,
                        new VideoCallEvent<String>(mCallId,
                                getVideoCallQualityString(videoQuality)));
            }
        }

        @Override
        public void onCallDataUsageChanged(long dataUsage) {
            Log.d("VideoCallCallback:onCallDataUsageChanged()");

            if ((mEvents & EVENT_DATA_USAGE_CHANGED)
                    == EVENT_DATA_USAGE_CHANGED) {
                servicePostEvent(TelephonyConstants.EventTelecomVideoCallDataUsageChanged,
                        new VideoCallEvent<Long>(mCallId, dataUsage));
            }
        }

        @Override
        public void onCameraCapabilitiesChanged(
                CameraCapabilities cameraCapabilities) {
            Log.d("VideoCallCallback:onCallDataUsageChanged()");

            if ((mEvents & EVENT_DATA_USAGE_CHANGED)
                    == EVENT_DATA_USAGE_CHANGED) {
                servicePostEvent(TelephonyConstants.EventTelecomVideoCallCameraCapabilities,
                        new VideoCallEvent<CameraCapabilities>(mCallId, cameraCapabilities));
            }

        }
    }

    /*
     * Container Class for Call and CallCallback Objects
     */
    private class CallContainer {

        /*
         * Call Container Members
         */

        private Call mCall;
        private CallCallback mCallCallback;
        private VideoCall mVideoCall;
        private VideoCallCallback mVideoCallCallback;

        /*
         * Call Container Functions
         */

        public CallContainer(Call call,
                CallCallback callback,
                VideoCall videoCall,
                VideoCallCallback videoCallCallback) {
            mCall = call;
            mCallCallback = callback;
            mVideoCall = videoCall;
            mVideoCallCallback = videoCallCallback;
        }

        public Call getCall() {
            return mCall;
        }

        public CallCallback getCallback() {
            return mCallCallback;
        }

        public InCallService.VideoCall getVideoCall() {
            return mVideoCall;
        }

        public VideoCallCallback getVideoCallCallback() {
            return mVideoCallCallback;
        }

        public void updateVideoCall(VideoCall videoCall, VideoCallCallback videoCallCallback) {
            if (videoCall == null && videoCallCallback != null) {
                Log.d("UpdateVideoCall: videoCall and videoCallCallback are null.");
                return;
            }
            mVideoCall = videoCall;
            mVideoCallCallback = videoCallCallback;
        }
    }

    /*
     * TODO: b/26272583 Refactor so that these are instance members of the
     * incallservice. Then we can perform null checks using the design pattern
     * of the "manager" classes.
     */

    private static EventFacade mEventFacade = null;
    private static HashMap<String, CallContainer> mCallContainerMap =
            new HashMap<String, CallContainer>();

    @Override
    public void onCallAdded(Call call) {
        Log.d("onCallAdded: " + call.toString());
        String id = getCallId(call);
        Log.d("Adding " + id);
        CallCallback callCallback = new CallCallback(id, CallCallback.EVENT_NONE);

        call.registerCallback(callCallback);

        VideoCall videoCall = call.getVideoCall();
        VideoCallCallback videoCallCallback = null;

        if (videoCall != null) {
            synchronized (mLock) {
                if (getVideoCallById(id) == null) {
                    videoCallCallback = new VideoCallCallback(id, VideoCallCallback.EVENT_NONE);
                    videoCall.registerCallback(videoCallCallback);
                }
            }
        }
        else {
            // No valid video object
            Log.d("No Video Call provided to InCallService.");
        }

        mCallContainerMap.put(id,
                new CallContainer(call,
                        callCallback,
                        videoCall,
                        videoCallCallback));

        /*
         * Once we have a call active, anchor the inCallService instance as a psuedo-singleton.
         * Because object lifetime is not guaranteed we shouldn't do this in the
         * constructor/destructor.
         */
        if (sService == null) {
            sService = this;
        }
        else if (sService != this) {
            Log.e("Multiple InCall Services Active in SL4A!");
        }

        CallListener.onCallAdded(id, call);
    }

    @Override
    public void onCallRemoved(Call call) {
        Log.d("onCallRemoved: " + call.toString());
        String id = getCallId(call);
        Log.d("Removing " + id);

        mCallContainerMap.remove(id);

        CallListener.onCallRemoved(id, call);

        if (mCallContainerMap.size() == 0) {
            sService = null;
        }
    }

    public static void setEventFacade(EventFacade facade) {
        Log.d(String.format("setEventFacade(): Settings SL4A event facade to %s",
                (facade != null) ? facade.toString() : "null"));
        mEventFacade = facade;
    }

    private static boolean servicePostEvent(String eventName, Object event) {

        if (mEventFacade == null) {
            Log.d("servicePostEvent():SL4A eventFacade Is Null!!");
            return false;
        }

        mEventFacade.postEvent(eventName, event);

        return true;
    }

    public static String getCallId(Call call) {
        if (call != null) {
            return "Call:" + call.hashCode();
        }
        else
            return "";
    }

    public static String getVideoCallId(InCallServiceImpl.VideoCall videoCall) {
        if (videoCall != null)
            return "VideoCall:" + videoCall.hashCode();
        else
            return "";
    }

    public static Call getCallById(String callId) {

        CallContainer cc = mCallContainerMap.get(callId);

        if (cc != null) {
            return cc.getCall();
        }

        return null;
    }

    private static CallCallback getCallCallbackById(String callId) {

        CallContainer cc = mCallContainerMap.get(callId);

        if (cc != null) {
            return cc.getCallback();
        }

        return null;
    }

    private static InCallService.VideoCall getVideoCallById(String callId) {

        CallContainer cc = mCallContainerMap.get(callId);

        if (cc != null) {
            return cc.getVideoCall();

        }

        return null;
    }

    private static VideoCallCallback
            getVideoCallListenerById(String callId) {

        CallContainer cc = mCallContainerMap.get(callId);

        if (cc != null) {
            return cc.getVideoCallCallback();
        }

        return null;
    }

    /*
     * Public Call/Phone Functions
     */

    public static void callDisconnect(String callId) {
        Call c = getCallById(callId);
        if (c == null) {
            Log.d("callDisconnect: callId is null");
            return;
        }

        c.disconnect();
    }

    public static void holdCall(String callId) {
        Call c = getCallById(callId);
        if (c == null) {
            Log.d("holdCall: callId is null");
            return;
        }
        c.hold();
    }

    public static void mergeCallsInConference(String callId) {
        Call c = getCallById(callId);
        if (c == null) {
            Log.d("mergeCallsInConference: callId is null");
            return;
        }
        c.mergeConference();
    }

    public static void splitCallFromConf(String callId) {
        Call c = getCallById(callId);
        if (c == null) {
            Log.d("splitCallFromConf: callId is null");
            return;
        }
        c.splitFromConference();
    }

    public static void unholdCall(String callId) {
        Call c = getCallById(callId);
        if (c == null) {
            Log.d("unholdCall: callId is null");
            return;
        }
        c.unhold();
    }

    public static void joinCallsInConf(String callIdOne, String callIdTwo) {
        Call callOne = getCallById(callIdOne);
        Call callTwo = getCallById(callIdTwo);

        if (callOne == null || callTwo == null) {
            Log.d("joinCallsInConf: callOne or CallTwo is null");
            return;
        }

        callOne.conference(callTwo);
    }

    public static Set<String> getCallIdList() {
        return mCallContainerMap.keySet();
    }

    public static void clearCallList() {
        mCallContainerMap.clear();
    }

    public static String callGetState(String callId) {
        Call c = getCallById(callId);
        if (c == null) {
            return getCallStateString(STATE_INVALID);
        }

        return getCallStateString(c.getState());
    }

    public static Call.Details callGetDetails(String callId) {
        Call c = getCallById(callId);
        if (c == null) {
            Log.d(String.format("Couldn't find an active call with ID:%s", callId));
            return null;
        }

        return c.getDetails();
    }

    public static List<String> callGetCallProperties(String callId) {
        Call.Details details = callGetDetails(callId);

        if (details == null) {
            return null;
        }

        return getCallPropertiesString(details.getCallProperties());
    }

    public static List<String> callGetCallCapabilities(String callId) {
        Call.Details details = callGetDetails(callId);

        if (details == null) {
            return null;
        }

        return getCallCapabilitiesString(details.getCallCapabilities());
    }

    @SuppressWarnings("deprecation")
    public static void overrideProximitySensor(Boolean screenOn) {
        InCallServiceImpl svc = getService();
        if (svc == null) {
            Log.d("overrideProximitySensor: InCallServiceImpl is null.");
            return;
        }

        Phone phone = svc.getPhone();
        if (phone == null) {
            Log.d("overrideProximitySensor: phone is null.");
            return;
        }

        phone.setProximitySensorOff(screenOn);
    }

    public static CallAudioState serviceGetCallAudioState() {
        InCallServiceImpl svc = getService();

        if (svc != null) {
            return svc.getCallAudioState();
        }
        else {
            return null;
        }
    }

    // Wonky name due to conflict with internal function
    public static void serviceSetAudioRoute(String route) {
        InCallServiceImpl svc = getService();

        if (svc == null) {
            Log.d("serviceSetAudioRoute: InCallServiceImpl is null.");
            return;
        }

        int r = getAudioRoute(route);

        Log.d(String.format("Setting Audio Route to %s:%d", route, r));

        if (r == INVALID_AUDIO_ROUTE) {
            Log.d(String.format("Invalid Audio route %s:%d", route, r));
            return;
        }
        svc.setAudioRoute(r);
    }

    public static void callStartListeningForEvent(String callId, String strEvent) {

        CallCallback cl = getCallCallbackById(callId);

        if (cl == null) {
            Log.d("callStartListeningForEvent: CallCallback is null.");
            return;
        }

        int event = getCallCallbackEvent(strEvent);

        if (event == CallCallback.EVENT_INVALID) {
            Log.d("callStartListeningForEvent: event is invalid.");
            return;
        }

        cl.startListeningForEvents(event);
    }

    public static void callStopListeningForEvent(String callId, String strEvent) {
        CallCallback cl = getCallCallbackById(callId);

        if (cl == null) {
            Log.d("callStopListeningForEvent: CallCallback is null.");
            return;
        }

        int event = getCallCallbackEvent(strEvent);

        if (event == CallCallback.EVENT_INVALID) {
            Log.d("callStopListeningForEvent: event is invalid.");
            return;
        }

        cl.stopListeningForEvents(event);
    }

    public static void videoCallStartListeningForEvent(String callId, String strEvent) {
        VideoCallCallback cl = getVideoCallListenerById(callId);

        if (cl == null) {
            Log.d(String.format("Couldn't find a call with call id:%s", callId));
            return;
        }

        int event = getVideoCallCallbackEvent(strEvent);

        if (event == VideoCallCallback.EVENT_INVALID) {
            Log.d(String.format("Failed to find a valid event:[%s]", strEvent));
            return;
        }

        cl.startListeningForEvents(event);
    }

    public static void videoCallStopListeningForEvent(String callId, String strEvent) {
        VideoCallCallback cl = getVideoCallListenerById(callId);

        if (cl == null) {
            Log.d("videoCallStopListeningForEvent: CallCallback is null.");
            return;
        }

        int event = getVideoCallCallbackEvent(strEvent);

        if (event == VideoCallCallback.EVENT_INVALID) {
            Log.d("getVideoCallCallbackEvent: event is invalid.");
            return;
        }

        cl.stopListeningForEvents(event);
    }

    public static String videoCallGetState(String callId) {
        Call c = getCallById(callId);

        int state = CallCallback.STATE_INVALID;

        if (c == null) {
            Log.d("videoCallGetState: call is null.");
        }
        else {
            state = c.getDetails().getVideoState();
        }

        return getVideoCallStateString(state);
    }

    public static void videoCallSendSessionModifyRequest(
            String callId, String videoStateString, String videoQualityString) {
        VideoCall vc = getVideoCallById(callId);

        if (vc == null) {
            Log.d("Invalid video call for call ID");
            return;
        }

        int videoState = getVideoCallState(videoStateString);
        int videoQuality = getVideoCallQuality(videoQualityString);

        Log.d(String.format("Sending Modify request for %s:%d, %s:%d",
                videoStateString, videoState, videoQualityString, videoQuality));

        if (videoState == CallCallback.STATE_INVALID ||
                videoQuality == QUALITY_INVALID || videoQuality == VideoProfile.QUALITY_UNKNOWN) {
            Log.d("Invalid session modify request!");
            return;
        }

        vc.sendSessionModifyRequest(new VideoProfile(videoState, videoQuality));
    }

    public static void videoCallSendSessionModifyResponse(
            String callId, String videoStateString, String videoQualityString) {
        VideoCall vc = getVideoCallById(callId);

        if (vc == null) {
            Log.d("Invalid video call for call ID");
            return;
        }

        int videoState = getVideoCallState(videoStateString);
        int videoQuality = getVideoCallQuality(videoQualityString);

        Log.d(String.format("Sending Modify request for %s:%d, %s:%d",
                videoStateString, videoState, videoQualityString, videoQuality));

        if (videoState == CallCallback.STATE_INVALID ||
                videoQuality == QUALITY_INVALID || videoQuality == VideoProfile.QUALITY_UNKNOWN) {
            Log.d("Invalid session modify request!");
            return;
        }

        vc.sendSessionModifyResponse(new VideoProfile(videoState, videoQuality));
    }

    public static void callAnswer(String callId, String videoState) {
        Call c = getCallById(callId);

        if (c == null) {
            Log.d("callAnswer: call is null.");
        }

        int state = getVideoCallState(videoState);

        if (state == CallCallback.STATE_INVALID) {
            Log.d("callAnswer: video state is invalid.");
            state = VideoProfile.STATE_AUDIO_ONLY;
        }

        c.answer(state);
    }

    public static void callReject(String callId, String message) {
        Call c = getCallById(callId);

        if (c == null) {
            Log.d("callReject: call is null.");
        }

        c.reject((message != null) ? true : false, message);
    }

    public static String getCallParent(String callId) {
        Call c = getCallById(callId);

        if (c == null) {
            Log.d("getCallParent: call is null.");
            return null;
        }
        Call callParent = c.getParent();
        return getCallId(callParent);
    }

    public static List<String> getCallChildren(String callId) {
        Call c = getCallById(callId);

        if (c == null) {
            Log.d("getCallChildren: call is null.");
            return null;
        }
        List<String> childrenList = new ArrayList<String>();
        List<Call> callChildren = c.getChildren();
        for (Call call : callChildren) {
            childrenList.add(getCallId(call));
        }
        return childrenList;
    }

    public static void swapCallsInConference(String callId) {
        Call c = getCallById(callId);
        if (c == null) {
            Log.d("swapCallsInConference: call is null.");
            return;
        }
        c.swapConference();
    }

    public static void callPlayDtmfTone(String callId, char digit) {
        Call c = getCallById(callId);
        if (c == null) {
            Log.d("callPlayDtmfTone: call is null.");
            return;
        }
        c.playDtmfTone(digit);
    }

    public static void callStopDtmfTone(String callId) {
        Call c = getCallById(callId);
        if (c == null) {
            Log.d("callStopDtmfTone: call is null.");
            return;
        }
        c.stopDtmfTone();
    }

    public static List<String> callGetCannedTextResponses(String callId) {
        Call c = getCallById(callId);
        if (c == null) {
            return null;
        }

        return c.getCannedTextResponses();
    }

    /*
     * String Mapping Functions for Facade Parameter Translation
     */

    public static String getVideoCallStateString(int state) {
        switch (state) {
            case VIDEO_STATE_AUDIO_ONLY:
                return TelephonyConstants.VT_STATE_AUDIO_ONLY;
            case VIDEO_STATE_TX_ENABLED:
                return TelephonyConstants.VT_STATE_TX_ENABLED;
            case VIDEO_STATE_RX_ENABLED:
                return TelephonyConstants.VT_STATE_RX_ENABLED;
            case VIDEO_STATE_BIDIRECTIONAL:
                return TelephonyConstants.VT_STATE_BIDIRECTIONAL;
            case VIDEO_STATE_TX_PAUSED:
                return TelephonyConstants.VT_STATE_TX_PAUSED;
            case VIDEO_STATE_RX_PAUSED:
                return TelephonyConstants.VT_STATE_RX_PAUSED;
            case VIDEO_STATE_BIDIRECTIONAL_PAUSED:
                return TelephonyConstants.VT_STATE_BIDIRECTIONAL_PAUSED;
            default:
        }
        Log.d("getVideoCallStateString: state is invalid.");
        return TelephonyConstants.VT_STATE_STATE_INVALID;
    }

    public static int getVideoCallState(String state) {
        switch (state.toUpperCase()) {
            case TelephonyConstants.VT_STATE_AUDIO_ONLY:
                return VIDEO_STATE_AUDIO_ONLY;
            case TelephonyConstants.VT_STATE_TX_ENABLED:
                return VIDEO_STATE_TX_ENABLED;
            case TelephonyConstants.VT_STATE_RX_ENABLED:
                return VIDEO_STATE_RX_ENABLED;
            case TelephonyConstants.VT_STATE_BIDIRECTIONAL:
                return VIDEO_STATE_BIDIRECTIONAL;
            case TelephonyConstants.VT_STATE_TX_PAUSED:
                return VIDEO_STATE_TX_PAUSED;
            case TelephonyConstants.VT_STATE_RX_PAUSED:
                return VIDEO_STATE_RX_PAUSED;
            case TelephonyConstants.VT_STATE_BIDIRECTIONAL_PAUSED:
                return VIDEO_STATE_BIDIRECTIONAL_PAUSED;

            default:
        }
        Log.d("getVideoCallState: state is invalid.");
        return CallCallback.STATE_INVALID;
    }

    private static int getVideoCallQuality(String quality) {

        switch (quality.toUpperCase()) {
            case TelephonyConstants.VT_VIDEO_QUALITY_UNKNOWN:
                return VideoProfile.QUALITY_UNKNOWN;
            case TelephonyConstants.VT_VIDEO_QUALITY_HIGH:
                return VideoProfile.QUALITY_HIGH;
            case TelephonyConstants.VT_VIDEO_QUALITY_MEDIUM:
                return VideoProfile.QUALITY_MEDIUM;
            case TelephonyConstants.VT_VIDEO_QUALITY_LOW:
                return VideoProfile.QUALITY_LOW;
            case TelephonyConstants.VT_VIDEO_QUALITY_DEFAULT:
                return VideoProfile.QUALITY_DEFAULT;
            default:
        }
        Log.d("getVideoCallQuality: quality is invalid.");
        return QUALITY_INVALID;
    }

    public static String getVideoCallQualityString(int quality) {
        switch (quality) {
            case VideoProfile.QUALITY_UNKNOWN:
                return TelephonyConstants.VT_VIDEO_QUALITY_UNKNOWN;
            case VideoProfile.QUALITY_HIGH:
                return TelephonyConstants.VT_VIDEO_QUALITY_HIGH;
            case VideoProfile.QUALITY_MEDIUM:
                return TelephonyConstants.VT_VIDEO_QUALITY_MEDIUM;
            case VideoProfile.QUALITY_LOW:
                return TelephonyConstants.VT_VIDEO_QUALITY_LOW;
            case VideoProfile.QUALITY_DEFAULT:
                return TelephonyConstants.VT_VIDEO_QUALITY_DEFAULT;
            default:
        }
        Log.d("getVideoCallQualityString: quality is invalid.");
        return TelephonyConstants.VT_VIDEO_QUALITY_INVALID;
    }

    private static int getCallCallbackEvent(String event) {

        switch (event.toUpperCase()) {
            case "EVENT_STATE_CHANGED":
                return CallCallback.EVENT_STATE_CHANGED;
            case "EVENT_PARENT_CHANGED":
                return CallCallback.EVENT_PARENT_CHANGED;
            case "EVENT_CHILDREN_CHANGED":
                return CallCallback.EVENT_CHILDREN_CHANGED;
            case "EVENT_DETAILS_CHANGED":
                return CallCallback.EVENT_DETAILS_CHANGED;
            case "EVENT_CANNED_TEXT_RESPONSES_LOADED":
                return CallCallback.EVENT_CANNED_TEXT_RESPONSES_LOADED;
            case "EVENT_POST_DIAL_WAIT":
                return CallCallback.EVENT_POST_DIAL_WAIT;
            case "EVENT_VIDEO_CALL_CHANGED":
                return CallCallback.EVENT_VIDEO_CALL_CHANGED;
            case "EVENT_CALL_DESTROYED":
                return CallCallback.EVENT_CALL_DESTROYED;
            case "EVENT_CONFERENCABLE_CALLS_CHANGED":
                return CallCallback.EVENT_CONFERENCABLE_CALLS_CHANGED;
        }
        Log.d("getCallCallbackEvent: event is invalid.");
        return CallCallback.EVENT_INVALID;
    }

    public static String getCallCallbackEventString(int event) {

        switch (event) {
            case CallCallback.EVENT_STATE_CHANGED:
                return "EVENT_STATE_CHANGED";
            case CallCallback.EVENT_PARENT_CHANGED:
                return "EVENT_PARENT_CHANGED";
            case CallCallback.EVENT_CHILDREN_CHANGED:
                return "EVENT_CHILDREN_CHANGED";
            case CallCallback.EVENT_DETAILS_CHANGED:
                return "EVENT_DETAILS_CHANGED";
            case CallCallback.EVENT_CANNED_TEXT_RESPONSES_LOADED:
                return "EVENT_CANNED_TEXT_RESPONSES_LOADED";
            case CallCallback.EVENT_POST_DIAL_WAIT:
                return "EVENT_POST_DIAL_WAIT";
            case CallCallback.EVENT_VIDEO_CALL_CHANGED:
                return "EVENT_VIDEO_CALL_CHANGED";
            case CallCallback.EVENT_CALL_DESTROYED:
                return "EVENT_CALL_DESTROYED";
            case CallCallback.EVENT_CONFERENCABLE_CALLS_CHANGED:
                return "EVENT_CONFERENCABLE_CALLS_CHANGED";
        }
        Log.d("getCallCallbackEventString: event is invalid.");
        return "EVENT_INVALID";
    }

    private static int getVideoCallCallbackEvent(String event) {

        switch (event) {
            case TelephonyConstants.EVENT_VIDEO_SESSION_MODIFY_REQUEST_RECEIVED:
                return VideoCallCallback.EVENT_SESSION_MODIFY_REQUEST_RECEIVED;
            case TelephonyConstants.EVENT_VIDEO_SESSION_MODIFY_RESPONSE_RECEIVED:
                return VideoCallCallback.EVENT_SESSION_MODIFY_RESPONSE_RECEIVED;
            case TelephonyConstants.EVENT_VIDEO_SESSION_EVENT:
                return VideoCallCallback.EVENT_SESSION_EVENT;
            case TelephonyConstants.EVENT_VIDEO_PEER_DIMENSIONS_CHANGED:
                return VideoCallCallback.EVENT_PEER_DIMENSIONS_CHANGED;
            case TelephonyConstants.EVENT_VIDEO_QUALITY_CHANGED:
                return VideoCallCallback.EVENT_VIDEO_QUALITY_CHANGED;
            case TelephonyConstants.EVENT_VIDEO_DATA_USAGE_CHANGED:
                return VideoCallCallback.EVENT_DATA_USAGE_CHANGED;
            case TelephonyConstants.EVENT_VIDEO_CAMERA_CAPABILITIES_CHANGED:
                return VideoCallCallback.EVENT_CAMERA_CAPABILITIES_CHANGED;
        }
        Log.d("getVideoCallCallbackEvent: event is invalid.");
        return CallCallback.EVENT_INVALID;
    }

    public static String getVideoCallCallbackEventString(int event) {

        switch (event) {
            case VideoCallCallback.EVENT_SESSION_MODIFY_REQUEST_RECEIVED:
                return TelephonyConstants.EVENT_VIDEO_SESSION_MODIFY_REQUEST_RECEIVED;
            case VideoCallCallback.EVENT_SESSION_MODIFY_RESPONSE_RECEIVED:
                return TelephonyConstants.EVENT_VIDEO_SESSION_MODIFY_RESPONSE_RECEIVED;
            case VideoCallCallback.EVENT_SESSION_EVENT:
                return TelephonyConstants.EVENT_VIDEO_SESSION_EVENT;
            case VideoCallCallback.EVENT_PEER_DIMENSIONS_CHANGED:
                return TelephonyConstants.EVENT_VIDEO_PEER_DIMENSIONS_CHANGED;
            case VideoCallCallback.EVENT_VIDEO_QUALITY_CHANGED:
                return TelephonyConstants.EVENT_VIDEO_QUALITY_CHANGED;
            case VideoCallCallback.EVENT_DATA_USAGE_CHANGED:
                return TelephonyConstants.EVENT_VIDEO_DATA_USAGE_CHANGED;
            case VideoCallCallback.EVENT_CAMERA_CAPABILITIES_CHANGED:
                return TelephonyConstants.EVENT_VIDEO_CAMERA_CAPABILITIES_CHANGED;
        }
        Log.d("getVideoCallCallbackEventString: event is invalid.");
        return TelephonyConstants.EVENT_VIDEO_INVALID;
    }

    public static String getCallStateString(int state) {
        switch (state) {
            case Call.STATE_NEW:
                return TelephonyConstants.CALL_STATE_NEW;
            case Call.STATE_DIALING:
                return TelephonyConstants.CALL_STATE_DIALING;
            case Call.STATE_RINGING:
                return TelephonyConstants.CALL_STATE_RINGING;
            case Call.STATE_HOLDING:
                return TelephonyConstants.CALL_STATE_HOLDING;
            case Call.STATE_ACTIVE:
                return TelephonyConstants.CALL_STATE_ACTIVE;
            case Call.STATE_DISCONNECTED:
                return TelephonyConstants.CALL_STATE_DISCONNECTED;
            case Call.STATE_PRE_DIAL_WAIT:
                return TelephonyConstants.CALL_STATE_PRE_DIAL_WAIT;
            case Call.STATE_CONNECTING:
                return TelephonyConstants.CALL_STATE_CONNECTING;
            case Call.STATE_DISCONNECTING:
                return TelephonyConstants.CALL_STATE_DISCONNECTING;
            case STATE_INVALID:
                return TelephonyConstants.CALL_STATE_INVALID;
            default:
                return TelephonyConstants.CALL_STATE_UNKNOWN;
        }
    }

    private static int getAudioRoute(String audioRoute) {
        switch (audioRoute.toUpperCase()) {
            case TelephonyConstants.AUDIO_ROUTE_BLUETOOTH:
                return CallAudioState.ROUTE_BLUETOOTH;
            case TelephonyConstants.AUDIO_ROUTE_EARPIECE:
                return CallAudioState.ROUTE_EARPIECE;
            case TelephonyConstants.AUDIO_ROUTE_SPEAKER:
                return CallAudioState.ROUTE_SPEAKER;
            case TelephonyConstants.AUDIO_ROUTE_WIRED_HEADSET:
                return CallAudioState.ROUTE_WIRED_HEADSET;
            case TelephonyConstants.AUDIO_ROUTE_WIRED_OR_EARPIECE:
                return CallAudioState.ROUTE_WIRED_OR_EARPIECE;
            default:
                return INVALID_AUDIO_ROUTE;
        }
    }

    public static String getAudioRouteString(int audioRoute) {
        return CallAudioState.audioRouteToString(audioRoute);
    }

    public static String getVideoCallSessionEventString(int event) {

        switch (event) {
            case Connection.VideoProvider.SESSION_EVENT_RX_PAUSE:
                return TelephonyConstants.SESSION_EVENT_RX_PAUSE;
            case Connection.VideoProvider.SESSION_EVENT_RX_RESUME:
                return TelephonyConstants.SESSION_EVENT_RX_RESUME;
            case Connection.VideoProvider.SESSION_EVENT_TX_START:
                return TelephonyConstants.SESSION_EVENT_TX_START;
            case Connection.VideoProvider.SESSION_EVENT_TX_STOP:
                return TelephonyConstants.SESSION_EVENT_TX_STOP;
            case Connection.VideoProvider.SESSION_EVENT_CAMERA_FAILURE:
                return TelephonyConstants.SESSION_EVENT_CAMERA_FAILURE;
            case Connection.VideoProvider.SESSION_EVENT_CAMERA_READY:
                return TelephonyConstants.SESSION_EVENT_CAMERA_READY;
            default:
                return TelephonyConstants.SESSION_EVENT_UNKNOWN;
        }
    }

    public static String getCallCapabilityString(int capability) {
        switch (capability) {
            case Call.Details.CAPABILITY_HOLD:
                return TelephonyConstants.CALL_CAPABILITY_HOLD;
            case Call.Details.CAPABILITY_SUPPORT_HOLD:
                return TelephonyConstants.CALL_CAPABILITY_SUPPORT_HOLD;
            case Call.Details.CAPABILITY_MERGE_CONFERENCE:
                return TelephonyConstants.CALL_CAPABILITY_MERGE_CONFERENCE;
            case Call.Details.CAPABILITY_SWAP_CONFERENCE:
                return TelephonyConstants.CALL_CAPABILITY_SWAP_CONFERENCE;
            case Call.Details.CAPABILITY_UNUSED_1:
                return TelephonyConstants.CALL_CAPABILITY_UNUSED_1;
            case Call.Details.CAPABILITY_RESPOND_VIA_TEXT:
                return TelephonyConstants.CALL_CAPABILITY_RESPOND_VIA_TEXT;
            case Call.Details.CAPABILITY_MUTE:
                return TelephonyConstants.CALL_CAPABILITY_MUTE;
            case Call.Details.CAPABILITY_MANAGE_CONFERENCE:
                return TelephonyConstants.CALL_CAPABILITY_MANAGE_CONFERENCE;
            case Call.Details.CAPABILITY_SUPPORTS_VT_LOCAL_RX:
                return TelephonyConstants.CALL_CAPABILITY_SUPPORTS_VT_LOCAL_RX;
            case Call.Details.CAPABILITY_SUPPORTS_VT_LOCAL_TX:
                return TelephonyConstants.CALL_CAPABILITY_SUPPORTS_VT_LOCAL_TX;
            case Call.Details.CAPABILITY_SUPPORTS_VT_LOCAL_BIDIRECTIONAL:
                return TelephonyConstants.CALL_CAPABILITY_SUPPORTS_VT_LOCAL_BIDIRECTIONAL;
            case Call.Details.CAPABILITY_SUPPORTS_VT_REMOTE_RX:
                return TelephonyConstants.CALL_CAPABILITY_SUPPORTS_VT_REMOTE_RX;
            case Call.Details.CAPABILITY_SUPPORTS_VT_REMOTE_TX:
                return TelephonyConstants.CALL_CAPABILITY_SUPPORTS_VT_REMOTE_TX;
            case Call.Details.CAPABILITY_SUPPORTS_VT_REMOTE_BIDIRECTIONAL:
                return TelephonyConstants.CALL_CAPABILITY_SUPPORTS_VT_REMOTE_BIDIRECTIONAL;
            case Call.Details.CAPABILITY_SEPARATE_FROM_CONFERENCE:
                return TelephonyConstants.CALL_CAPABILITY_SEPARATE_FROM_CONFERENCE;
            case Call.Details.CAPABILITY_DISCONNECT_FROM_CONFERENCE:
                return TelephonyConstants.CALL_CAPABILITY_DISCONNECT_FROM_CONFERENCE;
            case Call.Details.CAPABILITY_SPEED_UP_MT_AUDIO:
                return TelephonyConstants.CALL_CAPABILITY_SPEED_UP_MT_AUDIO;
            case Call.Details.CAPABILITY_CAN_UPGRADE_TO_VIDEO:
                return TelephonyConstants.CALL_CAPABILITY_CAN_UPGRADE_TO_VIDEO;
            case Call.Details.CAPABILITY_CAN_PAUSE_VIDEO:
                return TelephonyConstants.CALL_CAPABILITY_CAN_PAUSE_VIDEO;
        }
        return TelephonyConstants.CALL_CAPABILITY_UNKOWN;
    }

    public static List<String> getCallCapabilitiesString(int capabilities) {
        final int[] capabilityConstants = new int[] {
                Call.Details.CAPABILITY_HOLD,
                Call.Details.CAPABILITY_SUPPORT_HOLD,
                Call.Details.CAPABILITY_MERGE_CONFERENCE,
                Call.Details.CAPABILITY_SWAP_CONFERENCE,
                Call.Details.CAPABILITY_UNUSED_1,
                Call.Details.CAPABILITY_RESPOND_VIA_TEXT,
                Call.Details.CAPABILITY_MUTE,
                Call.Details.CAPABILITY_MANAGE_CONFERENCE,
                Call.Details.CAPABILITY_SUPPORTS_VT_LOCAL_RX,
                Call.Details.CAPABILITY_SUPPORTS_VT_LOCAL_TX,
                Call.Details.CAPABILITY_SUPPORTS_VT_LOCAL_BIDIRECTIONAL,
                Call.Details.CAPABILITY_SUPPORTS_VT_REMOTE_RX,
                Call.Details.CAPABILITY_SUPPORTS_VT_REMOTE_TX,
                Call.Details.CAPABILITY_SUPPORTS_VT_REMOTE_BIDIRECTIONAL,
                Call.Details.CAPABILITY_SEPARATE_FROM_CONFERENCE,
                Call.Details.CAPABILITY_DISCONNECT_FROM_CONFERENCE,
                Call.Details.CAPABILITY_SPEED_UP_MT_AUDIO,
                Call.Details.CAPABILITY_CAN_UPGRADE_TO_VIDEO,
                Call.Details.CAPABILITY_CAN_PAUSE_VIDEO
        };

        List<String> capabilityList = new ArrayList<String>();

        for (int capability : capabilityConstants) {
            if ((capabilities & capability) == capability) {
                capabilityList.add(getCallCapabilityString(capability));
            }
        }
        return capabilityList;
    }

    public static String getCallPropertyString(int property) {

        switch (property) {
            case Call.Details.PROPERTY_CONFERENCE:
                return TelephonyConstants.CALL_PROPERTY_CONFERENCE;
            case Call.Details.PROPERTY_GENERIC_CONFERENCE:
                return TelephonyConstants.CALL_PROPERTY_GENERIC_CONFERENCE;
            case Call.Details.PROPERTY_EMERGENCY_CALLBACK_MODE:
                return TelephonyConstants.CALL_PROPERTY_EMERGENCY_CALLBACK_MODE;
            case Call.Details.PROPERTY_WIFI:
                return TelephonyConstants.CALL_PROPERTY_WIFI;
            case Call.Details.PROPERTY_HIGH_DEF_AUDIO:
                return TelephonyConstants.CALL_PROPERTY_HIGH_DEF_AUDIO;
            default:
                return TelephonyConstants.CALL_PROPERTY_UNKNOWN;
        }
    }

    public static List<String> getCallPropertiesString(int properties) {
        final int[] propertyConstants = new int[] {
                Call.Details.PROPERTY_CONFERENCE,
                Call.Details.PROPERTY_GENERIC_CONFERENCE,
                Call.Details.PROPERTY_EMERGENCY_CALLBACK_MODE,
                Call.Details.PROPERTY_WIFI,
                Call.Details.PROPERTY_HIGH_DEF_AUDIO
        };

        List<String> propertyList = new ArrayList<String>();

        for (int property : propertyConstants) {
            if ((properties & property) == property) {
                propertyList.add(getCallPropertyString(property));
            }
        }

        return propertyList;
    }

    public static String getCallPresentationInfoString(int presentation) {
        switch (presentation) {
            case TelecomManager.PRESENTATION_ALLOWED:
                return TelephonyConstants.CALL_PRESENTATION_ALLOWED;
            case TelecomManager.PRESENTATION_RESTRICTED:
                return TelephonyConstants.CALL_PRESENTATION_RESTRICTED;
            case TelecomManager.PRESENTATION_PAYPHONE:
                return TelephonyConstants.CALL_PRESENTATION_PAYPHONE;
            default:
                return TelephonyConstants.CALL_PRESENTATION_UNKNOWN;
        }
    }
}

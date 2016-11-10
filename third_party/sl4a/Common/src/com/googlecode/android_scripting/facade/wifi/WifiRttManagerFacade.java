
package com.googlecode.android_scripting.facade.wifi;

import android.app.Service;
import android.content.Context;
import android.net.wifi.RttManager;
import android.net.wifi.RttManager.ResponderCallback;
import android.net.wifi.RttManager.ResponderConfig;
import android.net.wifi.RttManager.RttCapabilities;
import android.net.wifi.RttManager.RttListener;
import android.net.wifi.RttManager.RttParams;
import android.net.wifi.RttManager.RttResult;
import android.os.Bundle;
import android.os.Parcelable;

import com.googlecode.android_scripting.facade.EventFacade;
import com.googlecode.android_scripting.facade.FacadeManager;
import com.googlecode.android_scripting.jsonrpc.RpcReceiver;
import com.googlecode.android_scripting.rpc.Rpc;
import com.googlecode.android_scripting.rpc.RpcParameter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

/**
 * WifiRttManager functions.
 */
public class WifiRttManagerFacade extends RpcReceiver {
    private final Service mService;
    private final RttManager mRtt;
    private final EventFacade mEventFacade;
    private final Map<Integer, RttListener> mRangingListeners;
    private final Map<Integer, RttResponderCallback> mResponderCallbacks;

    public WifiRttManagerFacade(FacadeManager manager) {
        super(manager);
        mService = manager.getService();
        mRtt = (RttManager) mService.getSystemService(Context.WIFI_RTT_SERVICE);
        mEventFacade = manager.getReceiver(EventFacade.class);
        mRangingListeners = new Hashtable<Integer, RttListener>();
        mResponderCallbacks = new HashMap<>();
    }

    public static class RangingListener implements RttListener {
        private static final String TAG = "WifiRttRanging";
        private static int sCount = 0;
        private final EventFacade mEventFacade;
        public final int mId;

        public RangingListener(EventFacade eventFacade) {
            sCount += 1;
            mId = sCount;
            mEventFacade = eventFacade;
        }

        public static Bundle packRttResult(RttResult result) {
            Bundle rttResult = new Bundle();
            rttResult.putString("BSSID", result.bssid);
            rttResult.putInt("txRate", result.txRate);
            rttResult.putInt("rxRate", result.rxRate);
            rttResult.putInt("distance", result.distance);
            rttResult.putInt("distanceStandardDeviation",
                    result.distanceStandardDeviation);
            rttResult.putInt("distanceSpread", result.distanceSpread);
            rttResult.putInt("burstDuration", result.burstDuration);
            rttResult.putLong("rtt", result.rtt);
            rttResult.putLong("rttStandardDeviation",
                    result.rttStandardDeviation);
            rttResult.putLong("rttSpread", result.rttSpread);
            rttResult.putLong("ts", result.ts);
            rttResult.putInt("rssi", result.rssi);
            rttResult.putInt("rssiSpread", result.rssiSpread);
            rttResult.putInt("retryAfterDuration", result.retryAfterDuration);
            rttResult.putInt("measurementType", result.measurementType);
            rttResult.putInt("status", result.status);
            rttResult.putInt("frameNumberPerBurstPeer",
                    result.frameNumberPerBurstPeer);
            rttResult.putInt("successMeasurementFrameNumber",
                    result.successMeasurementFrameNumber);
            rttResult.putInt("measurementFrameNumber",
                    result.measurementFrameNumber);
            rttResult.putInt("burstNumber", result.burstNumber);
            rttResult.putInt("status", result.status);
            return rttResult;
        }

        @Override
        public void onSuccess(RttResult[] results) {
            if (results == null) {
                mEventFacade
                        .postEvent(RangingListener.TAG + mId + "onSuccess", null);
                return;
            }
            Bundle msg = new Bundle();
            Parcelable[] resultBundles = new Parcelable[results.length];
            for (int i = 0; i < results.length; i++) {
                resultBundles[i] = packRttResult(results[i]);
            }
            msg.putParcelableArray("Results", resultBundles);
            mEventFacade
                    .postEvent(RangingListener.TAG + mId + "onSuccess", msg);
        }

        @Override
        public void onFailure(int reason, String description) {
            Bundle msg = new Bundle();
            msg.putInt("Reason", reason);
            msg.putString("Description", description);
            mEventFacade
                    .postEvent(RangingListener.TAG + mId + "onFailure", msg);
        }

        @Override
        public void onAborted() {
            mEventFacade.postEvent(RangingListener.TAG + mId + "onAborted",
                    new Bundle());
        }
    }

    /**
     * A {@link ResponderCallback} that handles success and failures for enabling RTT responder
     * mode.
     */
    private static class RttResponderCallback extends ResponderCallback {
        private static final String TAG = "WifiRtt";

        // A monotonic increasing counter for responder callback ids.
        private static int sCount = 0;

        private final int mId;
        private final EventFacade mEventFacade;

        RttResponderCallback(EventFacade eventFacade) {
            sCount++;
            mId = sCount;
            mEventFacade = eventFacade;
        }

        @Override
        public void onResponderEnabled(ResponderConfig config) {
            Bundle bundle = new Bundle();
            bundle.putString("macAddress", config.macAddress);
            bundle.putInt("frequency", config.frequency);
            bundle.putInt("centerFreq0", config.centerFreq0);
            bundle.putInt("centerFreq1", config.centerFreq1);
            bundle.putInt("channelWidth", config.channelWidth);
            bundle.putInt("preamble", config.preamble);
            mEventFacade.postEvent(TAG + mId + "onResponderEnabled", bundle);
        }

        @Override
        public void onResponderEnableFailure(int reason) {
            Bundle bundle = new Bundle();
            bundle.putInt("reason", reason);
            mEventFacade.postEvent(TAG + mId + "onResponderEnableFailure", bundle);
        }
    }

    @Rpc(description = "Get wifi Rtt capabilities.")
    public RttCapabilities wifiRttGetCapabilities() {
        return mRtt.getRttCapabilities();
    }

    public static RttParams parseRttParam(JSONObject j) throws JSONException {
        RttParams result = new RttParams();
        if (j.has("deviceType")) {
            result.deviceType = j.getInt("deviceType");
        }
        if (j.has("requestType")) {
            result.requestType = j.getInt("requestType");
        }
        if (j.has("bssid")) {
            result.bssid = j.getString("bssid");
        }
        if (j.has("frequency")) {
            result.frequency = j.getInt("frequency");
        }
        if (j.has("channelWidth")) {
            result.channelWidth = j.getInt("channelWidth");
        }
        if (j.has("centerFreq0")) {
            result.centerFreq0 = j.getInt("centerFreq0");
        }
        if (j.has("centerFreq1")) {
            result.centerFreq1 = j.getInt("centerFreq1");
        }
        if (j.has("numberBurst")) {
            result.numberBurst = j.getInt("numberBurst");
        }
        if (j.has("burstTimeout")) {
            result.burstTimeout = j.getInt("burstTimeout");
        }
        if (j.has("interval")) {
            result.interval = j.getInt("interval");
        }
        if (j.has("numSamplesPerBurst")) {
            result.numSamplesPerBurst = j.getInt("numSamplesPerBurst");
        }
        if (j.has("numRetriesPerMeasurementFrame")) {
            result.numRetriesPerMeasurementFrame = j
                    .getInt("numRetriesPerMeasurementFrame");
        }
        if (j.has("numRetriesPerFTMR")) {
            result.numRetriesPerFTMR = j.getInt("numRetriesPerFTMR");
        }
        if (j.has("LCIRequest")) {
            result.LCIRequest = j.getBoolean("LCIRequest");
        }
        if (j.has("LCRRequest")) {
            result.LCRRequest = j.getBoolean("LCRRequest");
        }
        if (j.has("preamble")) {
            result.preamble = j.getInt("preamble");
        }
        if (j.has("bandwidth")) {
            result.bandwidth = j.getInt("bandwidth");
        }
        return result;
    }

    /**
     * Start WiFi RTT ranging using the given params. Returns the id associated with the
     * {@link RttListener} used for ranging.
     */
    @Rpc(description = "Start ranging.", returns = "Id of the listener associated with the "
            + "started ranging.")
    public Integer wifiRttStartRanging(
            @RpcParameter(name = "params") JSONArray params)
            throws JSONException {
        RttParams[] rParams = new RttParams[params.length()];
        for (int i = 0; i < params.length(); i++) {
            rParams[i] = parseRttParam(params.getJSONObject(i));
        }
        RangingListener listener = new RangingListener(mEventFacade);
        mRangingListeners.put(listener.mId, listener);
        mRtt.startRanging(rParams, listener);
        return listener.mId;
    }

    /**
     * Stop WiFi Rtt ranging for {@link RttListener} identified by the given {@code index}.
     */
    @Rpc(description = "Stop ranging.")
    public void wifiRttStopRanging(@RpcParameter(name = "index") Integer index) {
        mRtt.stopRanging(mRangingListeners.remove(index));
    }

    /**
     * Enable WiFi RTT responder role. Returns the id associated with the {@link ResponderCallback}
     * used for enabling responder.
     */
    @Rpc(description = "Enable responder", returns = "Id of the callback associated with enabling")
    public Integer wifiRttEnableResponder() {
        RttResponderCallback callback = new RttResponderCallback(mEventFacade);
        mResponderCallbacks.put(callback.mId, callback);
        mRtt.enableResponder(callback);
        return callback.mId;
    }

    /**
     * Disable WiFi RTT responder role for the {@link ResponderCallback} identified by the given
     * {@code index}.
     */
    @Rpc(description = "Disable responder")
    public void wifiRttDisableResponder(@RpcParameter(name = "index") Integer index) {
        mRtt.disableResponder(mResponderCallbacks.remove(index));
    }

    @Override
    public void shutdown() {
        ArrayList<Integer> keys = new ArrayList<Integer>(
                mRangingListeners.keySet());
        for (int k : keys) {
            wifiRttStopRanging(k);
        }
        for (int index : mResponderCallbacks.keySet()) {
            wifiRttDisableResponder(index);
        }
    }
}

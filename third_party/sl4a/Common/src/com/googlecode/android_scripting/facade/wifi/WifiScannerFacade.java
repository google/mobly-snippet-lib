/*
 * Copyright (C) 2014 Google Inc.
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

package com.googlecode.android_scripting.facade.wifi;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.googlecode.android_scripting.Log;
import com.googlecode.android_scripting.MainThread;
import com.googlecode.android_scripting.facade.EventFacade;
import com.googlecode.android_scripting.facade.FacadeManager;
import com.googlecode.android_scripting.jsonrpc.RpcReceiver;
import com.googlecode.android_scripting.rpc.Rpc;
import com.googlecode.android_scripting.rpc.RpcOptional;
import com.googlecode.android_scripting.rpc.RpcParameter;

import android.app.Service;
import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiScanner;
import android.net.wifi.WifiScanner.BssidInfo;
import android.net.wifi.WifiScanner.ChannelSpec;
import android.net.wifi.WifiScanner.ScanData;
import android.net.wifi.WifiScanner.ScanSettings;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Settings.Global;
import android.provider.Settings.SettingNotFoundException;

/**
 * WifiScanner functions.
 */
public class WifiScannerFacade extends RpcReceiver {
    private final Service mService;
    private final EventFacade mEventFacade;
    private final WifiScanner mScan;
    // These counters are just for indexing;
    // they do not represent the total number of listeners
    private static int WifiScanListenerCnt;
    private static int WifiChangeListenerCnt;
    private static int WifiBssidListenerCnt;
    private final ConcurrentHashMap<Integer, WifiScanListener> scanListeners;
    private final ConcurrentHashMap<Integer, WifiScanListener> scanBackgroundListeners;
    private final ConcurrentHashMap<Integer, ChangeListener> trackChangeListeners;
    private final ConcurrentHashMap<Integer, WifiBssidListener> trackBssidListeners;
    private static ConcurrentHashMap<Integer, ScanResult[]> wifiScannerResultList;
    private static ConcurrentHashMap<Integer, ScanData[]> wifiScannerDataList;

    public WifiScannerFacade(FacadeManager manager) {
        super(manager);
        mService = manager.getService();
        mScan = (WifiScanner) mService.getSystemService(Context.WIFI_SCANNING_SERVICE);
        mEventFacade = manager.getReceiver(EventFacade.class);
        scanListeners = new ConcurrentHashMap<Integer, WifiScanListener>();
        scanBackgroundListeners = new ConcurrentHashMap<Integer, WifiScanListener>();
        trackChangeListeners = new ConcurrentHashMap<Integer, ChangeListener>();
        trackBssidListeners = new ConcurrentHashMap<Integer, WifiBssidListener>();
        wifiScannerResultList = new ConcurrentHashMap<Integer, ScanResult[]>();
        wifiScannerDataList = new ConcurrentHashMap<Integer, ScanData[]>();
    }

    public static List<ScanResult> getWifiScanResult(Integer listenerIndex) {
        ScanResult[] sr = wifiScannerResultList.get(listenerIndex);
        return Arrays.asList(sr);
    }

    private class WifiActionListener implements WifiScanner.ActionListener {
        private final Bundle mResults;
        public int mIndex;
        protected String mEventType;
        private long startScanElapsedRealTime;

        public WifiActionListener(String type, int idx, Bundle resultBundle, long startScanERT) {
            this.mIndex = idx;
            this.mEventType = type;
            this.mResults = resultBundle;
            this.startScanElapsedRealTime = startScanERT;
        }

        @Override
        public void onSuccess() {
            Log.d("onSuccess " + mEventType + " " + mIndex);
            mResults.putString("Type", "onSuccess");
            mResults.putInt("Index", mIndex);
            mResults.putLong("ScanElapsedRealtime", startScanElapsedRealTime);
            mEventFacade.postEvent(mEventType + mIndex + "onSuccess", mResults.clone());
            mResults.clear();
        }

        @Override
        public void onFailure(int reason, String description) {
            Log.d("onFailure " + mEventType + " " + mIndex);
            mResults.putString("Type", "onFailure");
            mResults.putInt("Index", mIndex);
            mResults.putInt("Reason", reason);
            mResults.putString("Description", description);
            mEventFacade.postEvent(mEventType + mIndex + "onFailure", mResults.clone());
            mResults.clear();
        }

        public void reportResult(ScanResult[] results, String type) {
            Log.d("reportResult " + mEventType + " " + mIndex);
            mResults.putInt("Index", mIndex);
            mResults.putLong("ResultElapsedRealtime", SystemClock.elapsedRealtime());
            mResults.putString("Type", type);
            mResults.putParcelableArray("Results", results);
            mEventFacade.postEvent(mEventType + mIndex + type, mResults.clone());
            mResults.clear();
        }
    }

    /**
     * Constructs a wifiScanListener obj and returns it
     *
     * @return WifiScanListener
     */
    private WifiScanListener genWifiScanListener() {
        WifiScanListener mWifiScannerListener = MainThread.run(mService,
                new Callable<WifiScanListener>() {
                    @Override
                    public WifiScanListener call() throws Exception {
                        return new WifiScanListener();
                    }
                });
        scanListeners.put(mWifiScannerListener.mIndex, mWifiScannerListener);
        return mWifiScannerListener;
    }

    /**
     * Constructs a wifiScanListener obj for background scan and returns it
     *
     * @return WifiScanListener
     */
    private WifiScanListener genBackgroundWifiScanListener() {
        WifiScanListener mWifiScannerListener = MainThread.run(mService,
                new Callable<WifiScanListener>() {
                    @Override
                    public WifiScanListener call() throws Exception {
                        return new WifiScanListener();
                    }
                });
        scanBackgroundListeners.put(mWifiScannerListener.mIndex, mWifiScannerListener);
        return mWifiScannerListener;
    }

    private class WifiScanListener implements WifiScanner.ScanListener {
        private static final String mEventType = "WifiScannerScan";
        protected final Bundle mScanResults;
        protected final Bundle mScanData;
        private final WifiActionListener mWAL;
        public int mIndex;

        public WifiScanListener() {
            mScanResults = new Bundle();
            mScanData = new Bundle();
            WifiScanListenerCnt += 1;
            mIndex = WifiScanListenerCnt;
            mWAL = new WifiActionListener(mEventType, mIndex, mScanResults,
                    SystemClock.elapsedRealtime());
        }

        @Override
        public void onSuccess() {
            mWAL.onSuccess();
        }

        @Override
        public void onFailure(int reason, String description) {
            scanListeners.remove(mIndex);
            mWAL.onFailure(reason, description);
        }

        @Override
        public void onPeriodChanged(int periodInMs) {
            Log.d("onPeriodChanged " + mEventType + " " + mIndex);
            mScanResults.putString("Type", "onPeriodChanged");
            mScanResults.putInt("NewPeriod", periodInMs);
            mEventFacade.postEvent(mEventType + mIndex, mScanResults.clone());
            mScanResults.clear();
        }

        @Override
        public void onFullResult(ScanResult fullScanResult) {
            Log.d("onFullResult WifiScanListener " + mIndex);
            mWAL.reportResult(new ScanResult[] {
                    fullScanResult
            }, "onFullResult");
        }

        public void onResults(ScanData[] results) {
            Log.d("onResult WifiScanListener " + mIndex);
            wifiScannerDataList.put(mIndex, results);
            mScanData.putInt("Index", mIndex);
            mScanData.putLong("ResultElapsedRealtime", SystemClock.elapsedRealtime());
            mScanData.putString("Type", "onResults");
            mScanData.putParcelableArray("Results", results);
            mEventFacade.postEvent(mEventType + mIndex + "onResults", mScanData.clone());
            mScanData.clear();
        }
    }

    /**
     * Constructs a ChangeListener obj and returns it
     *
     * @return ChangeListener
     */
    private ChangeListener genWifiChangeListener() {
        ChangeListener mWifiChangeListener = MainThread.run(mService,
                new Callable<ChangeListener>() {
                    @Override
                    public ChangeListener call() throws Exception {
                        return new ChangeListener();
                    }
                });
        trackChangeListeners.put(mWifiChangeListener.mIndex, mWifiChangeListener);
        return mWifiChangeListener;
    }

    private class ChangeListener implements WifiScanner.WifiChangeListener {
        private static final String mEventType = "WifiScannerChange";
        protected final Bundle mResults;
        private final WifiActionListener mWAL;
        public int mIndex;

        public ChangeListener() {
            mResults = new Bundle();
            WifiChangeListenerCnt += 1;
            mIndex = WifiChangeListenerCnt;
            mWAL = new WifiActionListener(mEventType, mIndex, mResults,
                    SystemClock.elapsedRealtime());
        }

        @Override
        public void onSuccess() {
            mWAL.onSuccess();
        }

        @Override
        public void onFailure(int reason, String description) {
            trackChangeListeners.remove(mIndex);
            mWAL.onFailure(reason, description);
        }

        /**
         * indicates that changes were detected in wifi environment
         *
         * @param results indicate the access points that exhibited change
         */
        @Override
        public void onChanging(ScanResult[] results) { /* changes are found */
            mWAL.reportResult(results, "onChanging");
        }

        /**
         * indicates that no wifi changes are being detected for a while
         *
         * @param results indicate the access points that are bing monitored for change
         */
        @Override
        public void onQuiescence(ScanResult[] results) { /* changes settled down */
            mWAL.reportResult(results, "onQuiescence");
        }
    }

    private WifiBssidListener genWifiBssidListener() {
        WifiBssidListener mWifiBssidListener = MainThread.run(mService,
                new Callable<WifiBssidListener>() {
                    @Override
                    public WifiBssidListener call() throws Exception {
                        return new WifiBssidListener();
                    }
                });
        trackBssidListeners.put(mWifiBssidListener.mIndex, mWifiBssidListener);
        return mWifiBssidListener;
    }

    private class WifiBssidListener implements WifiScanner.BssidListener {
        private static final String mEventType = "WifiScannerBssid";
        protected final Bundle mResults;
        private final WifiActionListener mWAL;
        public int mIndex;

        public WifiBssidListener() {
            mResults = new Bundle();
            WifiBssidListenerCnt += 1;
            mIndex = WifiBssidListenerCnt;
            mWAL = new WifiActionListener(mEventType, mIndex, mResults,
                    SystemClock.elapsedRealtime());
        }

        @Override
        public void onSuccess() {
            mWAL.onSuccess();
        }

        @Override
        public void onFailure(int reason, String description) {
            trackBssidListeners.remove(mIndex);
            mWAL.onFailure(reason, description);
        }

        @Override
        public void onFound(ScanResult[] results) {
            mWAL.reportResult(results, "onFound");
        }

        @Override
        public void onLost(ScanResult[] results) {
            mWAL.reportResult(results, "onLost");
        }
    }

    private ScanSettings parseScanSettings(JSONObject j) throws JSONException {
        if (j == null) {
            return null;
        }
        ScanSettings result = new ScanSettings();
        if (j.has("band")) {
            result.band = j.optInt("band");
        }
        if (j.has("channels")) {
            JSONArray chs = j.getJSONArray("channels");
            ChannelSpec[] channels = new ChannelSpec[chs.length()];
            for (int i = 0; i < channels.length; i++) {
                channels[i] = new ChannelSpec(chs.getInt(i));
            }
            result.channels = channels;
        }
        if (j.has("maxScansToCache")) {
            result.maxScansToCache = j.getInt("maxScansToCache");
        }
        /* periodInMs and reportEvents are required */
        result.periodInMs = j.getInt("periodInMs");
        if (j.has("maxPeriodInMs")) {
            result.maxPeriodInMs = j.getInt("maxPeriodInMs");
        }
        if (j.has("stepCount")) {
            result.stepCount = j.getInt("stepCount");
        }
        result.reportEvents = j.getInt("reportEvents");
        if (j.has("numBssidsPerScan")) {
            result.numBssidsPerScan = j.getInt("numBssidsPerScan");
        }
        return result;
    }

    private BssidInfo[] parseBssidInfo(JSONArray jBssids) throws JSONException {
        BssidInfo[] bssids = new BssidInfo[jBssids.length()];
        for (int i = 0; i < bssids.length; i++) {
            JSONObject bi = (JSONObject) jBssids.get(i);
            BssidInfo bssidInfo = new BssidInfo();
            bssidInfo.bssid = bi.getString("BSSID");
            bssidInfo.high = bi.getInt("high");
            bssidInfo.low = bi.getInt("low");
            if (bi.has("frequencyHint")) {
                bssidInfo.frequencyHint = bi.getInt("frequencyHint");
            }
            bssids[i] = bssidInfo;
        }
        return bssids;
    }

    /**
     * Starts periodic WifiScanner scan
     *
     * @param scanSettings
     * @return the id of the scan listener associated with this scan
     * @throws JSONException
     */
    @Rpc(description = "Starts a WifiScanner Background scan")
    public Integer wifiScannerStartBackgroundScan(
            @RpcParameter(name = "scanSettings") JSONObject scanSettings)
                    throws JSONException {
        ScanSettings ss = parseScanSettings(scanSettings);
        Log.d("startWifiScannerScan with " + ss.channels);
        WifiScanListener listener = genBackgroundWifiScanListener();
        mScan.startBackgroundScan(ss, listener);
        return listener.mIndex;
    }

    /**
     * Get currently available scan results on appropriate listeners
     *
     * @return true if all scan results were reported correctly
     * @throws JSONException
     */
    @Rpc(description = "Get currently available scan results on appropriate listeners")
    public Boolean wifiScannerGetScanResults() throws JSONException {
        mScan.getScanResults();
        return true;
    }

    /**
     * Stops a WifiScanner scan
     *
     * @param listenerIndex the id of the scan listener whose scan to stop
     * @throws Exception
     */
    @Rpc(description = "Stops an ongoing  WifiScanner Background scan")
    public void wifiScannerStopBackgroundScan(
            @RpcParameter(name = "listener") Integer listenerIndex)
                    throws Exception {
        if (!scanBackgroundListeners.containsKey(listenerIndex)) {
            throw new Exception("Background scan session " + listenerIndex + " does not exist");
        }
        WifiScanListener listener = scanBackgroundListeners.get(listenerIndex);
        Log.d("stopWifiScannerScan listener " + listener.mIndex);
        mScan.stopBackgroundScan(listener);
        wifiScannerResultList.remove(listenerIndex);
        scanBackgroundListeners.remove(listenerIndex);
    }

    /**
     * Starts periodic WifiScanner scan
     *
     * @param scanSettings
     * @return the id of the scan listener associated with this scan
     * @throws JSONException
     */
    @Rpc(description = "Starts a WifiScanner single scan")
    public Integer wifiScannerStartScan(
            @RpcParameter(name = "scanSettings") JSONObject scanSettings)
                    throws JSONException {
        ScanSettings ss = parseScanSettings(scanSettings);
        Log.d("startWifiScannerScan with " + ss.channels);
        WifiScanListener listener = genWifiScanListener();
        mScan.startScan(ss, listener);
        return listener.mIndex;
    }

    /**
     * Stops a WifiScanner scan
     *
     * @param listenerIndex the id of the scan listener whose scan to stop
     * @throws Exception
     */
    @Rpc(description = "Stops an ongoing  WifiScanner Single scan")
    public void wifiScannerStopScan(@RpcParameter(name = "listener") Integer listenerIndex)
            throws Exception {
        if (!scanListeners.containsKey(listenerIndex)) {
            throw new Exception("Single scan session " + listenerIndex + " does not exist");
        }
        WifiScanListener listener = scanListeners.get(listenerIndex);
        Log.d("stopWifiScannerScan listener " + listener.mIndex);
        mScan.stopScan(listener);
        wifiScannerResultList.remove(listener.mIndex);
        scanListeners.remove(listenerIndex);
    }

    /** RPC Methods */
    @Rpc(description = "Returns the channels covered by the specified band number.")
    public List<Integer> wifiScannerGetAvailableChannels(
            @RpcParameter(name = "band") Integer band) {
        return mScan.getAvailableChannels(band);
    }

    /**
     * Starts tracking wifi changes
     *
     * @return the id of the change listener associated with this track
     * @throws Exception
     */
    @Rpc(description = "Starts tracking wifi changes")
    public Integer wifiScannerStartTrackingChange() throws Exception {
        ChangeListener listener = genWifiChangeListener();
        mScan.startTrackingWifiChange(listener);
        return listener.mIndex;
    }

    /**
     * Stops tracking wifi changes
     *
     * @param listenerIndex the id of the change listener whose track to stop
     * @throws Exception
     */
    @Rpc(description = "Stops tracking wifi changes")
    public void wifiScannerStopTrackingChange(
            @RpcParameter(name = "listener") Integer listenerIndex) throws Exception {
        if (!trackChangeListeners.containsKey(listenerIndex)) {
            throw new Exception("Wifi change tracking session " + listenerIndex
                    + " does not exist");
        }
        ChangeListener listener = trackChangeListeners.get(listenerIndex);
        mScan.stopTrackingWifiChange(listener);
        trackChangeListeners.remove(listenerIndex);
    }

    /**
     * Starts tracking changes of the specified bssids.
     *
     * @param bssidInfos An array of json strings, each representing a BssidInfo object.
     * @param apLostThreshold
     * @return The index of the listener used to start the tracking.
     * @throws JSONException
     */
    @Rpc(description = "Starts tracking changes of the specified bssids.")
    public Integer wifiScannerStartTrackingBssids(
            @RpcParameter(name = "bssidInfos") JSONArray bssidInfos,
            @RpcParameter(name = "apLostThreshold") Integer apLostThreshold) throws JSONException {
        BssidInfo[] bssids = parseBssidInfo(bssidInfos);
        WifiBssidListener listener = genWifiBssidListener();
        mScan.startTrackingBssids(bssids, apLostThreshold, listener);
        return listener.mIndex;
    }

    /**
     * Stops tracking the list of APs associated with the input listener
     *
     * @param listenerIndex the id of the bssid listener whose track to stop
     * @throws Exception
     */
    @Rpc(description = "Stops tracking changes in the APs on the list")
    public void wifiScannerStopTrackingBssids(
            @RpcParameter(name = "listener") Integer listenerIndex) throws Exception {
        if (!trackBssidListeners.containsKey(listenerIndex)) {
            throw new Exception("Bssid tracking session " + listenerIndex + " does not exist");
        }
        WifiBssidListener listener = trackBssidListeners.get(listenerIndex);
        mScan.stopTrackingBssids(listener);
        trackBssidListeners.remove(listenerIndex);
    }

    @Rpc(description = "Toggle the 'WiFi scan always available' option. If an input is given, the "
            + "option is set to what the input boolean indicates.")
    public void wifiScannerToggleAlwaysAvailable(
            @RpcParameter(name = "alwaysAvailable") @RpcOptional Boolean alwaysAvailable)
                    throws SettingNotFoundException {
        int new_state = 0;
        if (alwaysAvailable == null) {
            int current_state = Global.getInt(mService.getContentResolver(),
                    Global.WIFI_SCAN_ALWAYS_AVAILABLE);
            new_state = current_state ^ 0x1;
        } else {
            new_state = alwaysAvailable ? 1 : 0;
        }
        Global.putInt(mService.getContentResolver(), Global.WIFI_SCAN_ALWAYS_AVAILABLE, new_state);
    }

    @Rpc(description = "Returns true if WiFi scan is always available, false otherwise.")
    public Boolean wifiScannerIsAlwaysAvailable() throws SettingNotFoundException {
        int current_state = Global.getInt(mService.getContentResolver(),
                Global.WIFI_SCAN_ALWAYS_AVAILABLE);
        if (current_state == 1) {
            return true;
        }
        return false;
    }

    @Rpc(description = "Returns a list of mIndexes of existing listeners")
    public Set<Integer> wifiGetCurrentScanIndexes() {
        return scanListeners.keySet();
    }

    /**
     * Starts tracking wifi changes
     *
     * @return the id of the change listener associated with this track
     * @throws Exception
     */
    @Rpc(description = "Starts tracking wifi changes with track settings")
    public Integer wifiScannerStartTrackingChangeWithSetting(
            @RpcParameter(name = "trackSettings") JSONArray bssidSettings,
            @RpcParameter(name = "rssiSS") Integer rssiSS,
            @RpcParameter(name = "lostApSS") Integer lostApSS,
            @RpcParameter(name = "unchangedSS") Integer unchangedSS,
            @RpcParameter(name = "minApsBreachingThreshold") Integer minApsBreachingThreshold,
            @RpcParameter(name = "periodInMs") Integer periodInMs) throws Exception {
        Log.d("starting change track with track settings");
        BssidInfo[] bssids = parseBssidInfo(bssidSettings);
        mScan.configureWifiChange(rssiSS, lostApSS, unchangedSS, minApsBreachingThreshold,
              periodInMs, bssids);
        ChangeListener listener = genWifiChangeListener();
        mScan.startTrackingWifiChange(listener);
        return listener.mIndex;
    }

    /**
     * Shuts down all activities associated with WifiScanner
     */
    @Rpc(description = "Shuts down all WifiScanner activities and remove listeners.")
    public void wifiScannerShutdown() {
        this.shutdown();
    }

    /**
     * Stops all activity
     */
    @Override
    public void shutdown() {
        try {
            if (!scanListeners.isEmpty()) {
                Iterator<ConcurrentHashMap.Entry<Integer, WifiScanListener>> iter = scanListeners
                        .entrySet().iterator();
                while (iter.hasNext()) {
                    ConcurrentHashMap.Entry<Integer, WifiScanListener> entry = iter.next();
                    this.wifiScannerStopScan(entry.getKey());
                }
            }
            if (!scanBackgroundListeners.isEmpty()) {
                Iterator<ConcurrentHashMap.Entry<Integer, WifiScanListener>> iter = scanBackgroundListeners
                        .entrySet().iterator();
                while (iter.hasNext()) {
                    ConcurrentHashMap.Entry<Integer, WifiScanListener> entry = iter.next();
                    this.wifiScannerStopBackgroundScan(entry.getKey());
                }
            }
            if (!trackChangeListeners.isEmpty()) {
                Iterator<ConcurrentHashMap.Entry<Integer, ChangeListener>> iter = trackChangeListeners
                        .entrySet().iterator();
                while (iter.hasNext()) {
                    ConcurrentHashMap.Entry<Integer, ChangeListener> entry = iter.next();
                    this.wifiScannerStopTrackingChange(entry.getKey());
                }
            }
            if (!trackBssidListeners.isEmpty()) {
                Iterator<ConcurrentHashMap.Entry<Integer, WifiBssidListener>> iter = trackBssidListeners
                        .entrySet().iterator();
                while (iter.hasNext()) {
                    ConcurrentHashMap.Entry<Integer, WifiBssidListener> entry = iter.next();
                    this.wifiScannerStopTrackingBssids(entry.getKey());
                }
            }
        } catch (Exception e) {
            Log.e("Shutdown failed: " + e.toString());
        }
    }
}

/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.googlecode.android_scripting.facade.net.nsd;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;

import com.googlecode.android_scripting.facade.EventFacade;
import com.googlecode.android_scripting.facade.FacadeManager;
import com.googlecode.android_scripting.jsonrpc.JsonSerializable;
import com.googlecode.android_scripting.jsonrpc.RpcReceiver;
import com.googlecode.android_scripting.rpc.Rpc;
import com.googlecode.android_scripting.rpc.RpcParameter;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

/**
 * Facade for NsdManager - exposing mDNS utilities.
 */
public class NsdManagerFacade extends RpcReceiver {
    /**
     * NsdServiceInfo JSON constants
     */
    private static final String NSD_SERVICE_INFO_HOST = "serviceInfoHost";
    private static final String NSD_SERVICE_INFO_PORT = "serviceInfoPort";
    private static final String NSD_SERVICE_INFO_SERVICE_NAME = "serviceInfoServiceName";
    private static final String NSD_SERVICE_INFO_SERVICE_TYPE = "serviceInfoServiceType";

    /**
     * RegisterationListener events/data
     */
    private static final String REG_LISTENER_EVENT = "NsdRegistrationListener";

    private static final String REG_LISTENER_EVENT_ON_REG_FAILED = "OnRegistrationFailed";
    private static final String REG_LISTENER_EVENT_ON_SERVICE_REGISTERED = "OnServiceRegistered";
    private static final String REG_LISTENER_EVENT_ON_SERVICE_UNREG = "OnServiceUnregistered";
    private static final String REG_LISTENER_EVENT_ON_UNREG_FAILED = "OnUnregistrationFailed";

    private static final String REG_LISTENER_DATA_ID = "id";
    private static final String REG_LISTENER_CALLBACK = "callback";
    private static final String REG_LISTENER_ERROR_CODE = "error_code";

    /**
     * DiscoveryListener events/data
     */
    private static final String DISCOVERY_LISTENER_EVENT = "NsdDiscoveryListener";

    private static final String DISCOVERY_LISTENER_EVENT_ON_DISCOVERY_STARTED =
            "OnDiscoveryStarted";
    private static final String DISCOVERY_LISTENER_EVENT_ON_DISCOVERY_STOPPED =
            "OnDiscoveryStopped";
    private static final String DISCOVERY_LISTENER_EVENT_ON_SERVICE_FOUND = "OnServiceFound";
    private static final String DISCOVERY_LISTENER_EVENT_ON_SERVICE_LOST = "OnServiceLost";
    private static final String DISCOVERY_LISTENER_EVENT_ON_START_DISCOVERY_FAILED =
            "OnStartDiscoveryFailed";
    private static final String DISCOVERY_LISTENER_EVENT_ON_STOP_DISCOVERY_FAILED =
            "OnStopDiscoveryFailed";

    private static final String DISCOVERY_LISTENER_DATA_ID = "id";
    private static final String DISCOVERY_LISTENER_DATA_CALLBACK = "callback";
    private static final String DISCOVERY_LISTENER_DATA_SERVICE_TYPE = "service_type";
    private static final String DISCOVERY_LISTENER_DATA_ERROR_CODE = "error_code";

    /**
     * ResolveListener events/data
     */
    private static final String RESOLVE_LISTENER_EVENT = "NsdResolveListener";

    private static final String RESOLVE_LISTENER_EVENT_ON_RESOLVE_FAIL = "OnResolveFail";
    private static final String RESOLVE_LISTENER_EVENT_ON_SERVICE_RESOLVED = "OnServiceResolved";

    private static final String RESOLVE_LISTENER_DATA_ID = "id";
    private static final String RESOLVE_LISTENER_DATA_CALLBACK = "callback";
    private static final String RESOLVE_LISTENER_DATA_ERROR_CODE = "error_code";

    // facade class data

    private final EventFacade mEventFacade;

    private final NsdManager mNsdManager;
    private final Map<String, RegistrationListener> mRegistrationListeners = new HashMap<>();
    private final Map<String, DiscoveryListener> mDiscoveryListeners = new HashMap<>();
    private final Map<String, ResolveListener> mResolveListener = new HashMap<>();

    private static NsdServiceInfo getNsdServiceInfo(JSONObject j)
            throws JSONException, UnknownHostException {
        if (j == null) {
            return null;
        }

        NsdServiceInfo nsi = new NsdServiceInfo();

        if (j.has(NSD_SERVICE_INFO_SERVICE_NAME)) {
            nsi.setServiceName(j.getString(NSD_SERVICE_INFO_SERVICE_NAME));
        }
        if (j.has(NSD_SERVICE_INFO_SERVICE_TYPE)) {
            nsi.setServiceType(j.getString(NSD_SERVICE_INFO_SERVICE_TYPE));
        }
        if (j.has(NSD_SERVICE_INFO_PORT)) {
            nsi.setPort(j.getInt(NSD_SERVICE_INFO_PORT));
        }
        if (j.has(NSD_SERVICE_INFO_HOST)) {
            nsi.setHost(InetAddress.getByName(j.getString(NSD_SERVICE_INFO_HOST)));
        }

        return nsi;
    }

    private static void addNsdServiceInfo(JSONObject j, NsdServiceInfo nsi) {
        try {
            j.put(NSD_SERVICE_INFO_SERVICE_NAME, nsi.getServiceName());
            j.put(NSD_SERVICE_INFO_SERVICE_TYPE, nsi.getServiceType());
            j.put(NSD_SERVICE_INFO_HOST, nsi.getHost());
            j.put(NSD_SERVICE_INFO_PORT, nsi.getPort());
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public NsdManagerFacade(FacadeManager manager) {
        super(manager);
        mNsdManager = (NsdManager) manager.getService().getSystemService(Context.NSD_SERVICE);
        mEventFacade = manager.getReceiver(EventFacade.class);
    }

    @Override
    public void shutdown() {
        if (mNsdManager != null) {
            for (RegistrationListener listener : mRegistrationListeners.values()) {
                mNsdManager.unregisterService(listener);
            }
            mRegistrationListeners.clear();

            for (NsdManager.DiscoveryListener listener : mDiscoveryListeners.values()) {
                mNsdManager.stopServiceDiscovery(listener);
            }
            mDiscoveryListeners.clear();

            mResolveListener.clear();
        }
    }

    /**
     * Facade for NsdManager.registerService().
     */
    @Rpc(description = "register a service (starts advertising)")
    public String nsdRegisterService(
            @RpcParameter(name = "service information") JSONObject serviceInfo)
            throws JSONException, UnknownHostException {
        NsdServiceInfo nsi = getNsdServiceInfo(serviceInfo);
        RegistrationListener listener = new RegistrationListener();
        mRegistrationListeners.put(listener.mListenerId, listener);
        mNsdManager.registerService(nsi, NsdManager.PROTOCOL_DNS_SD, listener);
        return listener.mListenerId;
    }

    /**
     * Facade for NsdManager.unregisterService().
     */
    @Rpc(description = "unregister a service (stops advertising)")
    public Boolean nsdUnregisterService(
            @RpcParameter(name = "service registration callback ID") String id) {
        RegistrationListener listener = mRegistrationListeners.get(id);
        if (listener != null) {
            mNsdManager.unregisterService(listener);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Facade for NsdManager.discoverServices().
     */
    @Rpc(description = "start service discovery")
    public String nsdDiscoverServices(@RpcParameter(name = "service type") String serviceType) {
        DiscoveryListener listener = new DiscoveryListener();
        mDiscoveryListeners.put(listener.mListenerId, listener);
        mNsdManager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, listener);
        return listener.mListenerId;
    }

    /**
     * Facade for NsdManager.stopServiceDiscovery().
     */
    @Rpc(description = "stop service discovery")
    public Boolean nsdStopServiceDiscovery(
            @RpcParameter(name = "service discovery callback ID") String id) {
        DiscoveryListener listener = mDiscoveryListeners.get(id);
        if (listener != null) {
            mNsdManager.stopServiceDiscovery(listener);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Facade for NsdManager.resolveService().
     */
    @Rpc(description = "resolved discovered service info")
    public String nsdResolveService(
            @RpcParameter(name = "service information") JSONObject serviceInfo)
            throws JSONException, UnknownHostException {
        ResolveListener listener = new ResolveListener();
        mResolveListener.put(listener.mListenerId, listener);
        mNsdManager.resolveService(getNsdServiceInfo(serviceInfo), listener);
        return listener.mListenerId;
    }

    private class RegistrationListener implements NsdManager.RegistrationListener {
        final String mListenerId;

        RegistrationListener() {
            mListenerId = "com.googlecode.android_scripting.facade.net.nsd.RegistrationListener: "
                    + System.identityHashCode(this);
        }

        @Override
        public void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
            mEventFacade.postEvent(
                    REG_LISTENER_EVENT,
                    new RegistrationListenerEvent(REG_LISTENER_EVENT_ON_REG_FAILED, serviceInfo,
                            errorCode));
        }

        @Override
        public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
            mEventFacade.postEvent(
                    REG_LISTENER_EVENT,
                    new RegistrationListenerEvent(REG_LISTENER_EVENT_ON_UNREG_FAILED, serviceInfo,
                            errorCode));
        }

        @Override
        public void onServiceRegistered(NsdServiceInfo serviceInfo) {
            mEventFacade.postEvent(
                    REG_LISTENER_EVENT,
                    new RegistrationListenerEvent(REG_LISTENER_EVENT_ON_SERVICE_REGISTERED,
                            serviceInfo, null));
        }

        @Override
        public void onServiceUnregistered(NsdServiceInfo serviceInfo) {
            mEventFacade.postEvent(
                    REG_LISTENER_EVENT,
                    new RegistrationListenerEvent(REG_LISTENER_EVENT_ON_SERVICE_UNREG, serviceInfo,
                            null));
        }

        private class RegistrationListenerEvent implements JsonSerializable {
            private final String mCallback;
            private final NsdServiceInfo mServiceInfo;
            private final Integer mErrorCode;

            RegistrationListenerEvent(String callback, NsdServiceInfo serviceInfo,
                    Integer errorCode) {
                mCallback = callback;
                mServiceInfo = serviceInfo;
                mErrorCode = errorCode;
            }

            @Override
            public JSONObject toJSON() throws JSONException {
                JSONObject j = new JSONObject();

                j.put(REG_LISTENER_DATA_ID, mListenerId);
                j.put(REG_LISTENER_CALLBACK, mCallback);
                addNsdServiceInfo(j, mServiceInfo);
                if (mErrorCode != null) {
                    j.put(REG_LISTENER_ERROR_CODE, mErrorCode.intValue());
                }

                return j;
            }
        }
    }

    private class DiscoveryListener implements NsdManager.DiscoveryListener {
        final String mListenerId;

        DiscoveryListener() {
            mListenerId = "com.googlecode.android_scripting.facade.net.nsd.DiscoveryListener: "
                    + System.identityHashCode(this);
        }

        @Override
        public void onStartDiscoveryFailed(String serviceType, int errorCode) {
            mEventFacade.postEvent(DISCOVERY_LISTENER_EVENT,
                    new DiscoveryListenerEvent(DISCOVERY_LISTENER_EVENT_ON_START_DISCOVERY_FAILED,
                            serviceType, null, errorCode));
        }

        @Override
        public void onStopDiscoveryFailed(String serviceType, int errorCode) {
            mEventFacade.postEvent(DISCOVERY_LISTENER_EVENT,
                    new DiscoveryListenerEvent(DISCOVERY_LISTENER_EVENT_ON_STOP_DISCOVERY_FAILED,
                            serviceType, null, errorCode));
        }

        @Override
        public void onDiscoveryStarted(String serviceType) {
            mEventFacade.postEvent(DISCOVERY_LISTENER_EVENT,
                    new DiscoveryListenerEvent(DISCOVERY_LISTENER_EVENT_ON_DISCOVERY_STARTED,
                            serviceType, null, null));
        }

        @Override
        public void onDiscoveryStopped(String serviceType) {
            mEventFacade.postEvent(DISCOVERY_LISTENER_EVENT,
                    new DiscoveryListenerEvent(DISCOVERY_LISTENER_EVENT_ON_DISCOVERY_STOPPED,
                            serviceType, null, null));
        }

        @Override
        public void onServiceFound(NsdServiceInfo serviceInfo) {
            mEventFacade.postEvent(DISCOVERY_LISTENER_EVENT,
                    new DiscoveryListenerEvent(DISCOVERY_LISTENER_EVENT_ON_SERVICE_FOUND, null,
                            serviceInfo, null));
        }

        @Override
        public void onServiceLost(NsdServiceInfo serviceInfo) {
            mEventFacade.postEvent(DISCOVERY_LISTENER_EVENT,
                    new DiscoveryListenerEvent(DISCOVERY_LISTENER_EVENT_ON_SERVICE_LOST, null,
                            serviceInfo, null));
        }

        private class DiscoveryListenerEvent implements JsonSerializable {
            private final String mCallback;
            private final String mServiceType;
            private final NsdServiceInfo mServiceInfo;
            private final Integer mErrorCode;

            DiscoveryListenerEvent(String callback, String serviceType, NsdServiceInfo serviceInfo,
                    Integer errorCode) {
                mCallback = callback;
                mServiceType = serviceType;
                mServiceInfo = serviceInfo;
                mErrorCode = errorCode;
            }

            @Override
            public JSONObject toJSON() throws JSONException {
                JSONObject j = new JSONObject();

                j.put(DISCOVERY_LISTENER_DATA_ID, mListenerId);
                j.put(DISCOVERY_LISTENER_DATA_CALLBACK, mCallback);
                if (mServiceType != null) {
                    j.put(DISCOVERY_LISTENER_DATA_SERVICE_TYPE, mServiceType);
                }
                if (mServiceInfo != null) {
                    addNsdServiceInfo(j, mServiceInfo);
                }
                if (mErrorCode != null) {
                    j.put(DISCOVERY_LISTENER_DATA_ERROR_CODE, mErrorCode.intValue());
                }

                return j;
            }
        }
    }

    private class ResolveListener implements NsdManager.ResolveListener {
        final String mListenerId;

        ResolveListener() {
            mListenerId = "com.googlecode.android_scripting.facade.net.nsd.ResolveListener: "
                    + System.identityHashCode(this);
        }

        @Override
        public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
            mEventFacade.postEvent(RESOLVE_LISTENER_EVENT,
                    new ResolveListenerEvent(RESOLVE_LISTENER_EVENT_ON_RESOLVE_FAIL, serviceInfo,
                            errorCode));
        }

        @Override
        public void onServiceResolved(NsdServiceInfo serviceInfo) {
            mEventFacade.postEvent(RESOLVE_LISTENER_EVENT,
                    new ResolveListenerEvent(RESOLVE_LISTENER_EVENT_ON_SERVICE_RESOLVED,
                            serviceInfo, null));
        }

        private class ResolveListenerEvent implements JsonSerializable {
            private final String mCallback;
            private final NsdServiceInfo mServiceInfo;
            private final Integer mErrorCode;

            ResolveListenerEvent(String callback, NsdServiceInfo serviceInfo,
                    Integer errorCode) {
                mCallback = callback;
                mServiceInfo = serviceInfo;
                mErrorCode = errorCode;
            }

            @Override
            public JSONObject toJSON() throws JSONException {
                JSONObject j = new JSONObject();

                j.put(RESOLVE_LISTENER_DATA_ID, mListenerId);
                j.put(RESOLVE_LISTENER_DATA_CALLBACK, mCallback);
                addNsdServiceInfo(j, mServiceInfo);
                if (mErrorCode != null) {
                    j.put(RESOLVE_LISTENER_DATA_ERROR_CODE, mErrorCode.intValue());
                }

                return j;
            }
        }
    }
}

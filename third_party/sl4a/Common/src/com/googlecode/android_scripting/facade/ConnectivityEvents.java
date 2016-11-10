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

package com.googlecode.android_scripting.facade;

import com.googlecode.android_scripting.jsonrpc.JsonSerializable;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Utility class for ConnectivityManager/Service events. Encapsulates the data in a JSON format
 * to be used in the test script.
 *
 * Note that not all information is encapsulated. Add to the *Event classes as more information
 * is needed.
 */
public class ConnectivityEvents {
    /**
     * Translates a packet keep-alive event to JSON.
     */
    public static class PacketKeepaliveEvent implements JsonSerializable {
        private String mId;
        private String mPacketKeepaliveEvent;

        public PacketKeepaliveEvent(String id, String event) {
            mId = id;
            mPacketKeepaliveEvent = event;
        }

        /**
         * Create a JSON data-structure.
         */
        public JSONObject toJSON() throws JSONException {
            JSONObject packetKeepalive = new JSONObject();

            packetKeepalive.put(
                    ConnectivityConstants.PacketKeepaliveContainer.ID,
                    mId);
            packetKeepalive.put(
                    ConnectivityConstants.PacketKeepaliveContainer.PACKET_KEEPALIVE_EVENT,
                    mPacketKeepaliveEvent);

            return packetKeepalive;
        }
    }

    /**
     * Translates a ConnectivityManager.NetworkCallback to JSON.
     */
    public static class NetworkCallbackEventBase implements JsonSerializable {
        private String mId;
        private String mNetworkCallbackEvent;

        public NetworkCallbackEventBase(String id, String event) {
            mId = id;
            mNetworkCallbackEvent = event;
        }

        /**
         * Create a JSON data-structure.
         */
        public JSONObject toJSON() throws JSONException {
            JSONObject networkCallback = new JSONObject();

            networkCallback.put(ConnectivityConstants.NetworkCallbackContainer.ID, mId);
            networkCallback.put(
                    ConnectivityConstants.NetworkCallbackContainer.NETWORK_CALLBACK_EVENT,
                    mNetworkCallbackEvent);

            return networkCallback;
        }
    }

    /**
     * Specializes NetworkCallbackEventBase to add information for the onLosing() callback.
     */
    public static class NetworkCallbackEventOnLosing extends NetworkCallbackEventBase {
        private int mMaxMsToLive;

        public NetworkCallbackEventOnLosing(String id, String event, int maxMsToLive) {
            super(id, event);
            mMaxMsToLive = maxMsToLive;
        }

        /**
         * Create a JSON data-structure.
         */
        public JSONObject toJSON() throws JSONException {
            JSONObject json = super.toJSON();
            json.put(ConnectivityConstants.NetworkCallbackContainer.MAX_MS_TO_LIVE, mMaxMsToLive);
            return json;
        }
    }

    /**
     * Specializes NetworkCallbackEventBase to add information for the onCapabilitiesChanged()
     * callback.
     */
    public static class NetworkCallbackEventOnCapabilitiesChanged extends NetworkCallbackEventBase {
        private int mRssi;

        public NetworkCallbackEventOnCapabilitiesChanged(String id, String event, int rssi) {
            super(id, event);
            mRssi = rssi;
        }

        /**
         * Create a JSON data-structure.
         */
        public JSONObject toJSON() throws JSONException {
            JSONObject json = super.toJSON();
            json.put(ConnectivityConstants.NetworkCallbackContainer.RSSI, mRssi);
            return json;
        }
    }

    /**
     * Specializes NetworkCallbackEventBase to add information for the onCapabilitiesChanged()
     * callback.
     */
    public static class NetworkCallbackEventOnLinkPropertiesChanged extends
            NetworkCallbackEventBase {
        private String mInterfaceName;

        public NetworkCallbackEventOnLinkPropertiesChanged(String id, String event,
                String interfaceName) {
            super(id, event);
            mInterfaceName = interfaceName;
        }

        /**
         * Create a JSON data-structure.
         */
        public JSONObject toJSON() throws JSONException {
            JSONObject json = super.toJSON();
            json.put(ConnectivityConstants.NetworkCallbackContainer.INTERFACE_NAME, mInterfaceName);
            return json;
        }
    }
}

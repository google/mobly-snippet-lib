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

/**
 * Constants to be used in the facade for ConnectivityManager/Service.
 */
public class ConnectivityConstants {
    /**
     * Network callback master event name
     */
    public static final String EventNetworkCallback = "NetworkCallback";

    /**
     * Connectivity events - sub-names
     */
    public static final String NetworkCallbackPreCheck = "PreCheck";
    public static final String NetworkCallbackAvailable = "Available";
    public static final String NetworkCallbackLosing = "Losing";
    public static final String NetworkCallbackLost = "Lost";
    public static final String NetworkCallbackUnavailable = "Unavailable";
    public static final String NetworkCallbackCapabilitiesChanged = "CapabilitiesChanged";
    public static final String NetworkCallbackSuspended = "Suspended";
    public static final String NetworkCallbackResumed = "Resumed";
    public static final String NetworkCallbackLinkPropertiesChanged = "LinkPropertiesChanged";
    public static final String NetworkCallbackInvalid = "Invalid";

    /**
     * Connectivity changed event
     */
    public static final String EventConnectivityChanged = "ConnectivityChanged";

    /**
     * Packet keep-alive event
     */
    public static final String EventPacketKeepaliveCallback = "PacketKeepaliveCallback";

    /**
     * Packet keep-alive - sub-names
     */
    public static final String PacketKeepaliveCallbackStarted = "Started";
    public static final String PacketKeepaliveCallbackStopped = "Stopped";
    public static final String PacketKeepaliveCallbackError = "Error";
    public static final String PacketKeepaliveCallbackInvalid = "Invalid";

    /**
     * Constants for PacketKeepaliveEvent.
     */
    public static class PacketKeepaliveContainer {
        public static final String ID = "id";
        public static final String PACKET_KEEPALIVE_EVENT = "packetKeepaliveEvent";
    }

    /**
     * Constants for NetworkCallbackEvent.
     */
    public static class NetworkCallbackContainer {
        public static final String ID = "id";
        public static final String NETWORK_CALLBACK_EVENT = "networkCallbackEvent";
        public static final String MAX_MS_TO_LIVE = "maxMsToLive";
        public static final String RSSI = "rssi";
        public static final String INTERFACE_NAME = "interfaceName";
    }
}

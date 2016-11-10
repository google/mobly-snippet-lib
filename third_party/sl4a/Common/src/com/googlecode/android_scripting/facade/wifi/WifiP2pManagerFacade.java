
package com.googlecode.android_scripting.facade.wifi;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pGroupList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pServiceRequest;
import android.net.wifi.p2p.nsd.WifiP2pUpnpServiceInfo;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import com.android.internal.util.Protocol;
import com.googlecode.android_scripting.Log;
import com.googlecode.android_scripting.facade.EventFacade;
import com.googlecode.android_scripting.facade.FacadeManager;
import com.googlecode.android_scripting.jsonrpc.RpcReceiver;
import com.googlecode.android_scripting.rpc.Rpc;
import com.googlecode.android_scripting.rpc.RpcParameter;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * WifiP2pManager functions.
 */
public class WifiP2pManagerFacade extends RpcReceiver {

    class WifiP2pActionListener implements WifiP2pManager.ActionListener {
        private final EventFacade mEventFacade;
        private final String mEventType;
        private final String TAG;

        public WifiP2pActionListener(EventFacade eventFacade, String tag) {
            mEventType = "WifiP2p";
            mEventFacade = eventFacade;
            TAG = tag;
        }

        @Override
        public void onSuccess() {
            mEventFacade.postEvent(mEventType + TAG + "OnSuccess", null);
        }

        @Override
        public void onFailure(int reason) {
            Log.d("WifiActionListener  " + mEventType);
            Bundle msg = new Bundle();
            if (reason == WifiP2pManager.P2P_UNSUPPORTED) {
                msg.putString("reason", "P2P_UNSUPPORTED");
            } else if (reason == WifiP2pManager.ERROR) {
                msg.putString("reason", "ERROR");
            } else if (reason == WifiP2pManager.BUSY) {
                msg.putString("reason", "BUSY");
            } else if (reason == WifiP2pManager.NO_SERVICE_REQUESTS) {
                msg.putString("reason", "NO_SERVICE_REQUESTS");
            } else {
                msg.putInt("reason", reason);
            }
            mEventFacade.postEvent(mEventType + TAG + "OnFailure", msg);
        }
    }

    class WifiP2pConnectionInfoListener implements WifiP2pManager.ConnectionInfoListener {
        private final EventFacade mEventFacade;
        private final String mEventType;

        public WifiP2pConnectionInfoListener(EventFacade eventFacade) {
            mEventType = "WifiP2p";
            mEventFacade = eventFacade;
        }

        @Override
        public void onConnectionInfoAvailable(WifiP2pInfo info) {
            Bundle msg = new Bundle();
            msg.putBoolean("groupFormed", info.groupFormed);
            msg.putBoolean("isGroupOwner", info.isGroupOwner);
            InetAddress addr = info.groupOwnerAddress;
            String hostName = null;
            String hostAddress = null;
            if (addr != null) {
                hostName = addr.getHostName();
                hostAddress = addr.getHostAddress();
            }
            msg.putString("groupOwnerHostName", hostName);
            msg.putString("groupOwnerHostAddress", hostAddress);
            mEventFacade.postEvent(mEventType + "OnConnectionInfoAvailable", msg);
        }
    }

    class WifiP2pDnsSdServiceResponseListener implements
            WifiP2pManager.DnsSdServiceResponseListener {
        private final EventFacade mEventFacade;
        private final String mEventType;

        public WifiP2pDnsSdServiceResponseListener(EventFacade eventFacade) {
            mEventType = "WifiP2p";
            mEventFacade = eventFacade;
        }

        @Override
        public void onDnsSdServiceAvailable(String instanceName, String registrationType,
                WifiP2pDevice srcDevice) {
            Bundle msg = new Bundle();
            msg.putString("InstanceName", instanceName);
            msg.putString("RegistrationType", registrationType);
            msg.putString("SourceDeviceName", srcDevice.deviceName);
            msg.putString("SourceDeviceAddress", srcDevice.deviceAddress);
            mEventFacade.postEvent(mEventType + "OnDnsSdServiceAvailable", msg);
        }
    }

    class WifiP2pDnsSdTxtRecordListener implements WifiP2pManager.DnsSdTxtRecordListener {
        private final EventFacade mEventFacade;
        private final String mEventType;

        public WifiP2pDnsSdTxtRecordListener(EventFacade eventFacade) {
            mEventType = "WifiP2p";
            mEventFacade = eventFacade;
        }

        @Override
        public void onDnsSdTxtRecordAvailable(String fullDomainName,
                Map<String, String> txtRecordMap, WifiP2pDevice srcDevice) {
            Bundle msg = new Bundle();
            msg.putString("FullDomainName", fullDomainName);
            Bundle txtMap = new Bundle();
            for (String key : txtRecordMap.keySet()) {
                txtMap.putString(key, txtRecordMap.get(key));
            }
            msg.putBundle("TxtRecordMap", txtMap);
            msg.putString("SourceDeviceName", srcDevice.deviceName);
            msg.putString("SourceDeviceAddress", srcDevice.deviceAddress);
            mEventFacade.postEvent(mEventType + "OnDnsSdTxtRecordAvailable", msg);
        }

    }

    class WifiP2pGroupInfoListener implements WifiP2pManager.GroupInfoListener {
        private final EventFacade mEventFacade;
        private final String mEventType;

        public WifiP2pGroupInfoListener(EventFacade eventFacade) {
            mEventType = "WifiP2p";
            mEventFacade = eventFacade;
        }

        @Override
        public void onGroupInfoAvailable(WifiP2pGroup group) {
            mEventFacade.postEvent(mEventType + "OnGroupInfoAvailable", parseGroupInfo(group));
        }
    }

    class WifiP2pPeerListListener implements WifiP2pManager.PeerListListener {
        private final EventFacade mEventFacade;

        public WifiP2pPeerListListener(EventFacade eventFacade) {
            mEventFacade = eventFacade;
        }

        @Override
        public void onPeersAvailable(WifiP2pDeviceList newPeers) {
            Collection<WifiP2pDevice> devices = newPeers.getDeviceList();
            Log.d(devices.toString());
            if (devices.size() > 0) {
                mP2pPeers.clear();
                mP2pPeers.addAll(devices);
                Bundle msg = new Bundle();
                msg.putParcelableList("Peers", mP2pPeers);
                mEventFacade.postEvent(mEventType + "OnPeersAvailable", msg);
            }
        }
    }

    class WifiP2pPersistentGroupInfoListener implements WifiP2pManager.PersistentGroupInfoListener {
        private final EventFacade mEventFacade;
        private final String mEventType;

        public WifiP2pPersistentGroupInfoListener(EventFacade eventFacade) {
            mEventType = "WifiP2p";
            mEventFacade = eventFacade;
        }

        @Override
        public void onPersistentGroupInfoAvailable(WifiP2pGroupList groups) {
            ArrayList<Bundle> gs = new ArrayList<Bundle>();
            for (WifiP2pGroup g : groups.getGroupList()) {
                gs.add(parseGroupInfo(g));
            }
            mEventFacade.postEvent(mEventType + "OnPersistentGroupInfoAvailable", gs);
        }

    }

    class WifiP2pStateChangedReceiver extends BroadcastReceiver {
        private final EventFacade mEventFacade;
        private final Bundle mResults;

        WifiP2pStateChangedReceiver(EventFacade eventFacade) {
            mEventFacade = eventFacade;
            mResults = new Bundle();
        }

        @Override
        public void onReceive(Context c, Intent intent) {
            String action = intent.getAction();
            if (action.equals(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)) {
                Log.d("Wifi P2p State Changed.");
                int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, 0);
                if (state == WifiP2pManager.WIFI_P2P_STATE_DISABLED) {
                    Log.d("Disabled");
                    isP2pEnabled = false;
                } else if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                    Log.d("Enabled");
                    isP2pEnabled = true;
                }
            } else if (action.equals(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)) {
                Log.d("Wifi P2p Peers Changed. Requesting peers.");
                WifiP2pDeviceList peers = intent
                        .getParcelableExtra(WifiP2pManager.EXTRA_P2P_DEVICE_LIST);
                Log.d(peers.toString());
                wifiP2pRequestPeers();
            } else if (action.equals(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)) {
                Log.d("Wifi P2p Connection Changed.");
                WifiP2pInfo p2pInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_INFO);
                NetworkInfo networkInfo = intent
                        .getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
                WifiP2pGroup group = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_GROUP);
                if (networkInfo.isConnected()) {
                    Log.d("Wifi P2p Connected.");
                    mResults.putParcelable("P2pInfo", p2pInfo);
                    mResults.putParcelable("Group", group);
                    mEventFacade.postEvent(mEventType + "Connected", mResults);
                    mResults.clear();
                } else {
                    mEventFacade.postEvent(mEventType + "Disconnected", null);
                }
            } else if (action.equals(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)) {
                Log.d("Wifi P2p This Device Changed.");
                WifiP2pDevice device = intent
                        .getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
                mResults.putParcelable("Device", device);
                mEventFacade.postEvent(mEventType + "ThisDeviceChanged", mResults);
                mResults.clear();
            } else if (action.equals(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION)) {
                Log.d("Wifi P2p Discovery Changed.");
                int state = intent.getIntExtra(WifiP2pManager.EXTRA_DISCOVERY_STATE, 0);
                if (state == WifiP2pManager.WIFI_P2P_DISCOVERY_STARTED) {
                    Log.d("discovery started.");
                } else if (state == WifiP2pManager.WIFI_P2P_DISCOVERY_STOPPED) {
                    Log.d("discovery stoped.");
                }
            }
        }
    }

    private final static String mEventType = "WifiP2p";

    private WifiP2pManager.Channel mChannel;
    private final EventFacade mEventFacade;
    private final WifiP2pManager mP2p;
    private final WifiP2pStateChangedReceiver mP2pStateChangedReceiver;
    private final Service mService;
    private final IntentFilter mStateChangeFilter;
    private final Map<Integer, WifiP2pServiceRequest> mServiceRequests;

    private boolean isP2pEnabled;
    private int mServiceRequestCnt = 0;
    private WifiP2pServiceInfo mServiceInfo = null;
    private List<WifiP2pDevice> mP2pPeers = new ArrayList<WifiP2pDevice>();

    public WifiP2pManagerFacade(FacadeManager manager) {
        super(manager);
        mService = manager.getService();
        mP2p = (WifiP2pManager) mService.getSystemService(Context.WIFI_P2P_SERVICE);
        mEventFacade = manager.getReceiver(EventFacade.class);

        mStateChangeFilter = new IntentFilter(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mStateChangeFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mStateChangeFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mStateChangeFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        mStateChangeFilter.setPriority(999);

        mP2pStateChangedReceiver = new WifiP2pStateChangedReceiver(mEventFacade);
        mServiceRequests = new HashMap<Integer, WifiP2pServiceRequest>();
    }

    public Bundle parseGroupInfo(WifiP2pGroup group) {
        Bundle msg = new Bundle();
        msg.putString("Interface", group.getInterface());
        msg.putString("NetworkName", group.getNetworkName());
        msg.putString("Passphrase", group.getPassphrase());
        msg.putInt("NetworkId", group.getNetworkId());
        msg.putString("OwnerName", group.getOwner().deviceName);
        msg.putString("OwnerAddress", group.getOwner().deviceAddress);
        return msg;
    }

    @Override
    public void shutdown() {
        mService.unregisterReceiver(mP2pStateChangedReceiver);
    }

    @Rpc(description = "Accept p2p connection invitation.")
    public void wifiP2pAcceptConnection() throws RemoteException {
        Log.d("Accepting p2p connection.");
        Messenger m = mP2p.getP2pStateMachineMessenger();
        int user_accept = Protocol.BASE_WIFI_P2P_SERVICE + 2;
        Message msg = Message.obtain();
        msg.what = user_accept;
        m.send(msg);
    }

    @Rpc(description = "Reject p2p connection invitation.")
    public void wifiP2pRejectConnection() throws RemoteException {
        Log.d("Rejecting p2p connection.");
        Messenger m = mP2p.getP2pStateMachineMessenger();
        int user_accept = Protocol.BASE_WIFI_P2P_SERVICE + 3;
        Message msg = Message.obtain();
        msg.what = user_accept;
        m.send(msg);
    }

    @Rpc(description = "Register a local service for service discovery. One of the \"CreateXxxServiceInfo functions needs to be called first.\"")
    public void wifiP2pAddLocalService() {
        mP2p.addLocalService(mChannel, mServiceInfo,
                new WifiP2pActionListener(mEventFacade, "AddLocalService"));
    }

    @Rpc(description = "Add a service discovery request.")
    public Integer wifiP2pAddServiceRequest(
            @RpcParameter(name = "protocolType") Integer protocolType) {
        WifiP2pServiceRequest request = WifiP2pServiceRequest.newInstance(protocolType);
        mServiceRequestCnt += 1;
        mServiceRequests.put(mServiceRequestCnt, request);
        mP2p.addServiceRequest(mChannel, request, new WifiP2pActionListener(mEventFacade,
                "AddServiceRequest"));
        return mServiceRequestCnt;
    }

    @Rpc(description = "Cancel any ongoing connect negotiation.")
    public void wifiP2pCancelConnect() {
        mP2p.cancelConnect(mChannel, new WifiP2pActionListener(mEventFacade, "CancelConnect"));
    }

    @Rpc(description = "Clear all registered local services of service discovery.")
    public void wifiP2pClearLocalServices() {
        mP2p.clearLocalServices(mChannel,
                new WifiP2pActionListener(mEventFacade, "ClearLocalServices"));
    }

    @Rpc(description = "Clear all registered service discovery requests.")
    public void wifiP2pClearServiceRequests() {
        mP2p.clearServiceRequests(mChannel,
                new WifiP2pActionListener(mEventFacade, "ClearServiceRequests"));
    }

    @Rpc(description = "Connects to a discovered wifi p2p device.")
    public void wifiP2pConnect(@RpcParameter(name = "deviceId") String deviceId) {
        for (WifiP2pDevice d : mP2pPeers) {
            if (wifiP2pDeviceMatches(d, deviceId)) {
                WifiP2pConfig config = new WifiP2pConfig();
                config.deviceAddress = d.deviceAddress;
                config.wps.setup = WpsInfo.PBC;
                mP2p.connect(mChannel, config,
                        new WifiP2pActionListener(mEventFacade, "Connect"));
            }
        }
    }

    @Rpc(description = "Create a Bonjour service info object to be used for wifiP2pAddLocalService.")
    public void wifiP2pCreateBonjourServiceInfo(
            @RpcParameter(name = "instanceName") String instanceName,
            @RpcParameter(name = "serviceType") String serviceType,
            @RpcParameter(name = "txtMap") JSONObject txtMap) throws JSONException {
        Map<String, String> map = new HashMap<String, String>();
        for (String key : txtMap.keySet()) {
            map.put(key, txtMap.getString(key));
        }
        mServiceInfo = WifiP2pDnsSdServiceInfo.newInstance(instanceName, serviceType, map);
    }

    @Rpc(description = "Create a wifi p2p group.")
    public void wifiP2pCreateGroup() {
        mP2p.createGroup(mChannel, new WifiP2pActionListener(mEventFacade, "CreatGroup"));
    }

    @Rpc(description = "Create a Upnp service info object to be used for wifiP2pAddLocalService.")
    public void wifiP2pCreateUpnpServiceInfo(
            @RpcParameter(name = "uuid") String uuid,
            @RpcParameter(name = "device") String device,
            @RpcParameter(name = "services") List<String> services) {
        mServiceInfo = WifiP2pUpnpServiceInfo.newInstance(uuid, device, services);
    }

    @Rpc(description = "Delete a stored persistent group from the system settings.")
    public void wifiP2pDeletePersistentGroup(@RpcParameter(name = "netId") Integer netId) {
        mP2p.deletePersistentGroup(mChannel, netId,
                new WifiP2pActionListener(mEventFacade, "DeletePersistentGroup"));
    }

    private boolean wifiP2pDeviceMatches(WifiP2pDevice d, String deviceId) {
        return d.deviceName.equals(deviceId) || d.deviceAddress.equals(deviceId);
    }

    @Rpc(description = "Start peers discovery for wifi p2p.")
    public void wifiP2pDiscoverPeers() {
        mP2p.discoverPeers(mChannel, new WifiP2pActionListener(mEventFacade, "DiscoverPeers"));
    }

    @Rpc(description = "Initiate service discovery.")
    public void wifiP2pDiscoverServices() {
        mP2p.discoverServices(mChannel,
                new WifiP2pActionListener(mEventFacade, "DiscoverServices"));
    }

    @Rpc(description = "Initialize wifi p2p. Must be called before any other p2p functions.")
    public void wifiP2pInitialize() {
        mService.registerReceiver(mP2pStateChangedReceiver, mStateChangeFilter);
        mChannel = mP2p.initialize(mService, mService.getMainLooper(), null);
    }

    @Rpc(description = "Returns true if wifi p2p is enabled, false otherwise.")
    public Boolean wifiP2pIsEnabled() {
        return isP2pEnabled;
    }

    @Rpc(description = "Remove the current p2p group.")
    public void wifiP2pRemoveGroup() {
        mP2p.removeGroup(mChannel, new WifiP2pActionListener(mEventFacade, "RemoveGroup"));
    }

    @Rpc(description = "Remove a registered local service added with wifiP2pAddLocalService.")
    public void wifiP2pRemoveLocalService() {
        mP2p.removeLocalService(mChannel, mServiceInfo,
                new WifiP2pActionListener(mEventFacade, "RemoveLocalService"));
    }

    @Rpc(description = "Remove a service discovery request.")
    public void wifiP2pRemoveServiceRequest(@RpcParameter(name = "index") Integer index) {
        mP2p.removeServiceRequest(mChannel, mServiceRequests.remove(index),
                new WifiP2pActionListener(mEventFacade, "RemoveServiceRequest"));
    }

    @Rpc(description = "Request device connection info.")
    public void wifiP2pRequestConnectionInfo() {
        mP2p.requestConnectionInfo(mChannel, new WifiP2pConnectionInfoListener(mEventFacade));
    }

    @Rpc(description = "Create a wifi p2p group.")
    public void wifiP2pRequestGroupInfo() {
        mP2p.requestGroupInfo(mChannel, new WifiP2pGroupInfoListener(mEventFacade));
    }

    @Rpc(description = "Request peers that are discovered for wifi p2p.")
    public void wifiP2pRequestPeers() {
        mP2p.requestPeers(mChannel, new WifiP2pPeerListListener(mEventFacade));
    }

    @Rpc(description = "Request a list of all the persistent p2p groups stored in system.")
    public void wifiP2pRequestPersistentGroupInfo() {
        mP2p.requestPersistentGroupInfo(mChannel,
                new WifiP2pPersistentGroupInfoListener(mEventFacade));
    }

    @Rpc(description = "Set p2p device name.")
    public void wifiP2pSetDeviceName(@RpcParameter(name = "devName") String devName) {
        mP2p.setDeviceName(mChannel, devName,
                new WifiP2pActionListener(mEventFacade, "SetDeviceName"));
    }

    @Rpc(description = "Register a callback to be invoked on receiving Bonjour service discovery response.")
    public void wifiP2pSetDnsSdResponseListeners() {
        mP2p.setDnsSdResponseListeners(mChannel,
                new WifiP2pDnsSdServiceResponseListener(mEventFacade),
                new WifiP2pDnsSdTxtRecordListener(mEventFacade));
    }

    @Rpc(description = "Stop an ongoing peer discovery.")
    public void wifiP2pStopPeerDiscovery() {
        mP2p.stopPeerDiscovery(mChannel,
                new WifiP2pActionListener(mEventFacade, "StopPeerDiscovery"));
    }

}

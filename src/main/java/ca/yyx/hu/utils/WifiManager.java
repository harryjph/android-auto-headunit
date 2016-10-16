package ca.yyx.hu.utils;

import android.app.UiModeManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.AsyncTask;

import ca.yyx.hu.aap.AapTransport;

/**
 * @author algavris
 * @date 28/04/2016.
 */
public class WifiManager {
    private final Context mContext;
    private final AapTransport mTransport;
    private final Listener mListener;
    private WifiStartTask mStartTask;

    private WifiP2pManager m_wifidir_mgr;
    private WifiP2pManager.Channel m_wifidir_chan;
    private BroadcastReceiver m_wifidir_bcr;

    private IntentFilter mIntentFilter;

    interface Listener {
        void onWifiStartListener();
    }

    public WifiManager(Context context, AapTransport transport, Listener listener) {
        mContext = context;
        mTransport = transport;
        mListener = listener;
    }


    public void start() {
        if (mStartTask != null) {
            return;
        }
        mStartTask = new WifiStartTask(mTransport);
    }

    private void onWifiStarted() {
        mStartTask = null;
        mListener.onWifiStartListener();
    }

    private class WifiStartTask extends AsyncTask<Object, Void, Boolean> {
        private AapTransport mTransport;

        public WifiStartTask(AapTransport transport) {//, View statusText) {
            mTransport = transport;
        }

        @Override
        protected Boolean doInBackground(Object... params) {//(Void... params) {// (Params... p) {//Void... v) {//Void... params) {
            AppLog.logd("wifi_long_start start ");
            return mTransport.connectAndStart(null);
        }

        // Start activity that can handle the JPEG image
        @Override
        protected void onPostExecute(Boolean result) {//String result) {
            AppLog.logd("wifi_long_start done: " + result);
            onWifiStarted();
        }
    }

    void deinitP2P() {
        AppLog.logd("m_wifidir_mgr: " + m_wifidir_mgr + "  m_wifidir_chan: " + m_wifidir_chan + "  m_wifidir_bcr: " + m_wifidir_bcr);

        if (m_wifidir_chan == null || m_wifidir_mgr == null) {
            return;
        }
//*
        m_wifidir_mgr.stopPeerDiscovery(m_wifidir_chan, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                AppLog.logd("stopPeerDiscovery Success");
            }

            @Override
            public void onFailure(int reasonCode) {
                AppLog.loge("stopPeerDiscovery Failure reasonCode: " + reasonCode);
            }
        });
//*/
        WifiP2pManager.ActionListener wal = new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                AppLog.logd("stopPeerDiscovery/cancelConnect/removeGroup Success");
            }

            @Override
            public void onFailure(int reasonCode) {
                AppLog.loge("stopPeerDiscovery/cancelConnect/removeGroup Failure reasonCode: " + reasonCode);
            }
        };

        m_wifidir_mgr.stopPeerDiscovery(m_wifidir_chan, wal);

        m_wifidir_mgr.cancelConnect(m_wifidir_chan, wal);

        m_wifidir_mgr.removeGroup(m_wifidir_chan, wal);

        m_wifidir_chan = null;
        m_wifidir_mgr = null;
        m_wifidir_bcr = null;

    }

    void initP2P() {
        m_wifidir_mgr = (WifiP2pManager) mContext.getSystemService(Context.WIFI_P2P_SERVICE);
        m_wifidir_chan = m_wifidir_mgr.initialize(mContext, mContext.getMainLooper(), null);
        m_wifidir_bcr = new WiFiDirectBroadcastReceiver(m_wifidir_mgr);

        AppLog.logd("m_wifidir_mgr: " + m_wifidir_mgr + "  m_wifidir_chan: " + m_wifidir_chan + "  m_wifidir_bcr: " + m_wifidir_bcr);

        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        WifiP2pManager.ActionListener wal = new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                AppLog.logd("createGroup Success");                            // This is only triggered after we have a connection
            }

            @Override
            public void onFailure(int reasonCode) {
                AppLog.loge("createGroup Failure reasonCode: " + reasonCode);  // Already exists ?: reateGroup Failure reasonCode: 2
            }
        };
        m_wifidir_mgr.createGroup(m_wifidir_chan, wal);
    }

    WifiP2pManager.PeerListListener myPeerListListener = new WifiP2pManager.PeerListListener() {
        @Override
        public void onPeersAvailable(WifiP2pDeviceList peers) {
            AppLog.logd("myPeerListListener onPeersAvailable peers: " + peers);
        }
    };

    // A BroadcastReceiver that notifies of important Wi-Fi p2p events.
    public class WiFiDirectBroadcastReceiver extends BroadcastReceiver {

        private WifiP2pManager m_wifidir_mgr;

        public WiFiDirectBroadcastReceiver(WifiP2pManager manager) {
            super();
            this.m_wifidir_mgr = manager;
        }


        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            AppLog.logd("action: " + action);

            if (action.equals(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)) {          // Check to see if Wi-Fi is enabled and notify appropriate activity
                int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
                if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                    AppLog.logd("STATE_CHANGED Wifi P2P is enabled");
                } else {
                    AppLog.logd("STATE_CHANGED Wi-Fi P2P is not enabled");
                }
            } else if (action.equals(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)) {
                AppLog.logd("PEERS_CHANGED");        // Request currently available peers from the wifi p2p manager. This is an asynchronous call and the calling activity is notified with a callback on PeerListListener.onPeersAvailable()
                if (m_wifidir_mgr != null) {
                    m_wifidir_mgr.requestPeers(m_wifidir_chan, myPeerListListener);
                }
            } else if (action.equals(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)) {
                AppLog.logd("CONNECTION_CHANGED");          // Respond to new connection or disconnections
            } else if (action.equals(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)) {
                AppLog.logd("THIS_DEVICE_CHANGED");          // Respond to this device's wifi state changing
            } else if (action.equals(UiModeManager.ACTION_ENTER_CAR_MODE)) {
                AppLog.logd("ACTION_ENTER_CAR_MODE");
            } else if (action.equals(UiModeManager.ACTION_EXIT_CAR_MODE)) {
                AppLog.logd("ACTION_EXIT_CAR_MODE");
            } else if (action.equals(UiModeManager.ACTION_ENTER_DESK_MODE)) {
                AppLog.logd("ACTION_ENTER_DESK_MODE");
            } else if (action.equals(UiModeManager.ACTION_EXIT_DESK_MODE)) {
                AppLog.logd("ACTION_EXIT_DESK_MODE");
            } else {
                AppLog.loge("OTHER !! ??");
            }
        }

    }

}

package ca.yyx.hu.ui;

import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

/**
 * @author algavris
 * @date 05/11/2016.
 */

public class BaseFragment extends Fragment {

    public Context getContext() {
        return getActivity();
    }

    public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter) {
        return getActivity().registerReceiver(receiver, filter);
    }

    public void unregisterReceiver(BroadcastReceiver receiver) {
        getActivity().unregisterReceiver(receiver);
    }
}

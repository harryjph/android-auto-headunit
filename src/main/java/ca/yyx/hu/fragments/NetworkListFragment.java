package ca.yyx.hu.fragments;

import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import ca.yyx.hu.R;
import ca.yyx.hu.aap.AapService;
import ca.yyx.hu.utils.NetworkUtils;
import ca.yyx.hu.utils.Settings;

/**
 * @author algavris
 * @date 05/11/2016.
 */

public class NetworkListFragment extends BaseFragment {
    public static final String TAG = "NetworkListFragment";
    AddressAdapter mAdapter;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        RecyclerView recyclerView = (RecyclerView) inflater.inflate(R.layout.fragment_list, container, false);

        Context context = getContext();

        mAdapter = new AddressAdapter(context, getFragmentManager());
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.setAdapter(mAdapter);
        return recyclerView;
    }

    @Override
    public void onResume() {
        super.onResume();

        try {
            int currentIp = NetworkUtils.getWifiIpAddress(getActivity());
            InetAddress inet = NetworkUtils.intToInetAddress(currentIp);
            mAdapter.setCurrentAddress(inet);
        } catch (IOException ignored) {
            mAdapter.setNoCurrentAddress();
        }

        mAdapter.loadAddresses();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    public void addAddress(InetAddress ip) {
        mAdapter.addNewAddress(ip);
    }

    private static class DeviceViewHolder extends RecyclerView.ViewHolder {
        final Button allowButton;
        final Button startButton;

        DeviceViewHolder(View itemView) {
            super(itemView);
            this.startButton = (Button) itemView.findViewById(android.R.id.button2);
            this.allowButton = (Button) itemView.findViewById(android.R.id.button1);
        }
    }

    private static class AddressAdapter extends RecyclerView.Adapter<DeviceViewHolder> implements View.OnClickListener {
        private final Context mContext;
        private final FragmentManager mFragmentManager;
        private ArrayList<String> mAddressList = new ArrayList<>();
        private InetAddress mCurrentAddress;
        private final Settings mSettings;


        AddressAdapter(Context context, FragmentManager fragmentManager) {
            mContext = context;
            mFragmentManager = fragmentManager;
            mSettings = new Settings(context);

        }

        @Override
        public DeviceViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(mContext).inflate(R.layout.list_item_device, parent, false);
            DeviceViewHolder holder = new DeviceViewHolder(view);

            holder.startButton.setOnClickListener(this);
            holder.allowButton.setOnClickListener(this);
            return holder;
        }

        @Override
        public void onBindViewHolder(DeviceViewHolder holder, int position) {
            String device = mAddressList.get(position);

            String line1;
            String line2;
            if (position == 0) {
                line1 = "Add a new address";
                line2 = "Current ip: " + (TextUtils.isEmpty(device) ? "No ip address" : device);
                holder.allowButton.setVisibility(View.GONE);
            } else {
                line1 = device;
                line2 = "";
                holder.allowButton.setVisibility(View.VISIBLE);
                holder.allowButton.setText(R.string.remove);
            }
            String msg = String.format(Locale.US, "<b>%1$s</b><br/>%2$s", line1, line2);
            holder.startButton.setTag(R.integer.key_position, position);
            holder.startButton.setText(Html.fromHtml(msg));
            holder.startButton.setTag(R.integer.key_data, device);
            holder.allowButton.setText(R.string.remove);
        }

        @Override
        public int getItemCount() {
            return mAddressList.size();
        }

        @Override
        public void onClick(View v) {
            if (v.getId() == android.R.id.button2) {
                int position = (int) v.getTag(R.integer.key_position);
                if (position == 0) {
                    InetAddress ip = null;
                    try {
                        int ipInt = NetworkUtils.getWifiIpAddress(mContext);
                        ip = NetworkUtils.intToInetAddress(ipInt);
                    } catch (IOException ignored) {
                    }

                    AddNetworkAddressDialog dialog = AddNetworkAddressDialog.create(ip);
                    dialog.show(mFragmentManager, "AddNetworkAddressDialog");
                } else {
                    String ipAddress = (String) v.getTag(R.integer.key_data);
                    mContext.startService(AapService.createIntent(ipAddress, mContext));
                }
            } else {
                String ipAddress = (String) v.getTag(R.integer.key_data);
                this.removeAddress(ipAddress);
            }
        }

        private void addCurrentAddress() {
            if (mCurrentAddress != null) {
                mAddressList.add(mCurrentAddress.getHostAddress());
            } else {
                mAddressList.add("");
            }
        }

        void setCurrentAddress(InetAddress currentAddress) {
            mCurrentAddress = currentAddress;
        }

        void setNoCurrentAddress() {
            mCurrentAddress = null;
        }

        void addNewAddress(InetAddress ip) {
            HashSet<String> addrs = (HashSet<String>) mSettings.getNetworkAddresses();
            addrs.add(ip.getHostAddress());
            mSettings.setNetworkAddresses(addrs);
            set(addrs);
        }

        void loadAddresses() {
            Set<String> addrs = mSettings.getNetworkAddresses();
            set(addrs);
        }

        private void set(Collection<String> addrs) {
            mAddressList.clear();
            addCurrentAddress();
            mAddressList.addAll(addrs);
            notifyDataSetChanged();
        }

        private void removeAddress(String ipAddress) {
            HashSet<String> addrs = (HashSet<String>) mSettings.getNetworkAddresses();
            addrs.remove(ipAddress);
            mSettings.setNetworkAddresses(addrs);
            set(addrs);
        }

    }
}

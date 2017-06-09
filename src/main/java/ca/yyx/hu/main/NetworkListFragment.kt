package ca.yyx.hu.main

import android.app.FragmentManager
import android.content.Context
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.Html
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button

import java.io.IOException
import java.net.InetAddress
import java.util.ArrayList
import java.util.HashSet
import java.util.Locale

import ca.yyx.hu.R
import ca.yyx.hu.aap.AapService
import ca.yyx.hu.app.BaseFragment
import ca.yyx.hu.utils.NetworkUtils
import ca.yyx.hu.utils.Settings

/**
 * @author algavris
 * *
 * @date 05/11/2016.
 */

class NetworkListFragment : BaseFragment() {
    private lateinit var mAdapter: AddressAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle?): View {
        val recyclerView = inflater.inflate(R.layout.fragment_list, container, false) as RecyclerView

        val context = activity

        mAdapter = AddressAdapter(context, fragmentManager)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = mAdapter
        return recyclerView
    }

    override fun onResume() {
        super.onResume()

        try {
            val currentIp = NetworkUtils.getWifiIpAddress(activity)
            val inet = NetworkUtils.intToInetAddress(currentIp)
            mAdapter.setCurrentAddress(inet)
        } catch (ignored: IOException) {
            mAdapter.setNoCurrentAddress()
        }

        mAdapter.loadAddresses()
    }

    fun addAddress(ip: InetAddress) {
        mAdapter.addNewAddress(ip)
    }

    private class DeviceViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {
        internal val allowButton: Button = itemView.findViewById(android.R.id.button1) as Button
        internal val startButton: Button = itemView.findViewById(android.R.id.button2) as Button
    }

    private class AddressAdapter
        internal constructor(
                private val mContext: Context,
                private val mFragmentManager: FragmentManager) : RecyclerView.Adapter<DeviceViewHolder>(), View.OnClickListener {

        private val mAddressList = ArrayList<String>()
        private var mCurrentAddress: InetAddress? = null
        private val mSettings: Settings = Settings(mContext)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
            val view = LayoutInflater.from(mContext).inflate(R.layout.list_item_device, parent, false)
            val holder = DeviceViewHolder(view)

            holder.startButton.setOnClickListener(this)
            holder.allowButton.setOnClickListener(this)
            return holder
        }

        override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
            val device = mAddressList[position]

            val line1: String
            val line2: String
            if (position == 0) {
                line1 = "Add a new address"
                line2 = "Current ip: " + if (TextUtils.isEmpty(device)) "No ip address" else device
                holder.allowButton.visibility = View.GONE
            } else {
                line1 = device
                line2 = ""
                holder.allowButton.visibility = View.VISIBLE
                holder.allowButton.setText(R.string.remove)
            }
            val msg = String.format(Locale.US, "<b>%1\$s</b><br/>%2\$s", line1, line2)
            holder.startButton.setTag(R.integer.key_position, position)
            holder.startButton.text = Html.fromHtml(msg)
            holder.startButton.setTag(R.integer.key_data, device)
            holder.allowButton.setText(R.string.remove)
        }

        override fun getItemCount(): Int {
           return mAddressList.size
        }

        override fun onClick(v: View) {
            if (v.id == android.R.id.button2) {
                if (v.getTag(R.integer.key_position) == 0) {
                    var ip: InetAddress? = null
                    try {
                        val ipInt = NetworkUtils.getWifiIpAddress(mContext)
                        ip = NetworkUtils.intToInetAddress(ipInt)
                    } catch (ignored: IOException) {
                    }

                    val dialog = AddNetworkAddressDialog.create(ip)
                    dialog.show(mFragmentManager, "AddNetworkAddressDialog")
                } else {
                    mContext.startService(AapService.createIntent(v.getTag(R.integer.key_data) as String, mContext))
                }
            } else {
                this.removeAddress(v.getTag(R.integer.key_data) as String)
            }
        }

        private fun addCurrentAddress() {
            if (mCurrentAddress != null) {
                mAddressList.add(mCurrentAddress!!.getHostAddress())
            } else {
                mAddressList.add("")
            }
        }

        internal fun setCurrentAddress(currentAddress: InetAddress) {
            mCurrentAddress = currentAddress
        }

        internal fun setNoCurrentAddress() {
            mCurrentAddress = null
        }

        internal fun addNewAddress(ip: InetAddress) {
            val addrs = mSettings.networkAddresses as HashSet<String>
            addrs.add(ip.hostAddress)
            mSettings.networkAddresses = addrs
            set(addrs)
        }

        internal fun loadAddresses() {
            val addrs = mSettings.networkAddresses
            set(addrs)
        }

        private fun set(addrs: Collection<String>) {
            mAddressList.clear()
            addCurrentAddress()
            mAddressList.addAll(addrs)
            notifyDataSetChanged()
        }

        private fun removeAddress(ipAddress: String) {
            val addrs = mSettings.networkAddresses as HashSet<String>
            addrs.remove(ipAddress)
            mSettings.networkAddresses = addrs
            set(addrs)
        }

    }

    companion object {
        val TAG = "NetworkListFragment"
    }
}

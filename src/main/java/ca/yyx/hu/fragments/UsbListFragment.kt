package ca.yyx.hu.fragments

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast

import java.util.Collections
import java.util.Comparator
import java.util.Locale

import ca.yyx.hu.R
import ca.yyx.hu.aap.AapService
import ca.yyx.hu.connection.UsbDeviceCompat
import ca.yyx.hu.connection.UsbModeSwitch
import ca.yyx.hu.connection.UsbReceiver
import ca.yyx.hu.utils.Settings

/**
 * @author algavris
 * *
 * @date 05/11/2016.
 */

class UsbListFragment : BaseFragment(), UsbReceiver.Listener {
    private lateinit var mAdapter: DeviceAdapter
    private lateinit var mSettings: Settings
    private lateinit var mUsbReceiver: UsbReceiver

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle?): View {
        val recyclerView = inflater.inflate(R.layout.fragment_list, container, false) as RecyclerView

        val context = activity

        mSettings = Settings(context)
        mAdapter = DeviceAdapter(context, mSettings)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.setAdapter(mAdapter)

        mUsbReceiver = UsbReceiver(this)

        return recyclerView
    }

    override fun onResume() {
        super.onResume()
        val allowDevices = mSettings.allowedDevices
        mAdapter.setData(createDeviceList(allowDevices), allowDevices)
        registerReceiver(mUsbReceiver, UsbReceiver.createFilter())
    }

    override fun onPause() {
        super.onPause()
        mSettings.commit()
        unregisterReceiver(mUsbReceiver)
    }

    override fun onUsbDetach(device: UsbDevice) {
        val allowDevices = mSettings.allowedDevices
        mAdapter.setData(createDeviceList(allowDevices), allowDevices)
    }

    override fun onUsbAttach(device: UsbDevice) {
        val allowDevices = mSettings.allowedDevices
        mAdapter.setData(createDeviceList(allowDevices), allowDevices)
    }

    override fun onUsbPermission(granted: Boolean, connect: Boolean, device: UsbDevice) {
        val allowDevices = mSettings.allowedDevices
        mAdapter.setData(createDeviceList(allowDevices), allowDevices)
    }

    private class DeviceViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {
        internal val allowButton: Button = itemView.findViewById(android.R.id.button1) as Button
        internal val startButton: Button = itemView.findViewById(android.R.id.button2) as Button
    }

    private class DeviceAdapter internal constructor(private val mContext: Context, private val mSettings: Settings) : RecyclerView.Adapter<DeviceViewHolder>(), View.OnClickListener {
        private var mAllowedDevices: MutableSet<String> = mutableSetOf()
        private var mDeviceList: List<UsbDeviceCompat> = listOf()


        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
            val view = LayoutInflater.from(mContext).inflate(R.layout.list_item_device, parent, false)
            return DeviceViewHolder(view)
        }

        override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
            val device = mDeviceList[position]

            holder.startButton.text = Html.fromHtml(String.format(
                    Locale.US, "<b>%1\$s</b><br/>%2\$s",
                    device.uniqueName, device.deviceName
            ))
            holder.startButton.tag = position
            holder.startButton.setOnClickListener(this)

            if (device.isInAccessoryMode) {
                holder.allowButton.setText(R.string.allowed)
                holder.allowButton.setTextColor(mContext.resources.getColor(R.color.material_green_700))
                holder.allowButton.isEnabled = false
            } else {
                if (mAllowedDevices.contains(device.uniqueName)) {
                    holder.allowButton.setText(R.string.allowed)
                    holder.allowButton.setTextColor(mContext.resources.getColor(R.color.material_green_700))
                } else {
                    holder.allowButton.setText(R.string.ignored)
                    holder.allowButton.setTextColor(mContext.resources.getColor(R.color.material_orange_700))
                }
                holder.allowButton.tag = position
                holder.allowButton.isEnabled = true
                holder.allowButton.setOnClickListener(this)
            }
        }

        override fun getItemCount(): Int {
            return mDeviceList.size
        }

        override fun onClick(v: View) {
            val device = mDeviceList.get(v.tag as Int)
            if (v.id == android.R.id.button1) {
                if (mAllowedDevices.contains(device.uniqueName)) {
                    mAllowedDevices.remove(device.uniqueName)
                } else {
                    mAllowedDevices.add(device.uniqueName)
                }
                mSettings.allowedDevices = mAllowedDevices
                notifyDataSetChanged()
            } else {
                if (device.isInAccessoryMode) {
                    mContext.startService(AapService.createIntent(device.wrappedDevice, mContext))
                } else {
                    val usbMode = UsbModeSwitch(mContext.getSystemService(Context.USB_SERVICE) as UsbManager)
                    if (usbMode.switchMode(device.wrappedDevice)) {
                        Toast.makeText(mContext, "Success", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(mContext, "Failed", Toast.LENGTH_SHORT).show()
                    }
                    notifyDataSetChanged()
                }
            }
        }

        internal fun setData(deviceList: List<UsbDeviceCompat>, allowedDevices: Set<String>) {
            mAllowedDevices = allowedDevices.toMutableSet()
            mDeviceList = deviceList
            notifyDataSetChanged()
        }
    }

    private fun createDeviceList(allowDevices: Set<String>): List<UsbDeviceCompat> {
        val manager = activity.getSystemService(Context.USB_SERVICE) as UsbManager
        val devices = manager.deviceList
        val list = devices.entries.map { (_, device) ->
            UsbDeviceCompat(device)
        }

        Collections.sort(list, Comparator<UsbDeviceCompat> { lhs, rhs ->
            if (lhs.isInAccessoryMode) {
                return@Comparator -1
            }
            if (rhs.isInAccessoryMode) {
                return@Comparator 1
            }
            if (allowDevices.contains(lhs.uniqueName)) {
                return@Comparator -1
            }
            if (allowDevices.contains(rhs.uniqueName)) {
                return@Comparator 1
            }
            lhs.uniqueName.compareTo(rhs.uniqueName)
        })

        return list
    }
}

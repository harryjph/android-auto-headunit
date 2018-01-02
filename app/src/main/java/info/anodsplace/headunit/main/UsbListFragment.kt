package info.anodsplace.headunit.main

import android.hardware.usb.UsbManager
import android.widget.Button

import info.anodsplace.headunit.connection.UsbDeviceCompat
import info.anodsplace.headunit.utils.Settings

/**
 * @author algavris
 * *
 * @date 05/11/2016.
 */

class UsbListFragment : info.anodsplace.headunit.app.BaseFragment(), info.anodsplace.headunit.connection.UsbReceiver.Listener {
    private lateinit var adapter: info.anodsplace.headunit.main.UsbListFragment.DeviceAdapter
    private lateinit var settings: info.anodsplace.headunit.utils.Settings
    private lateinit var usbReceiver: info.anodsplace.headunit.connection.UsbReceiver

    override fun onCreateView(inflater: android.view.LayoutInflater, container: android.view.ViewGroup, savedInstanceState: android.os.Bundle?): android.view.View {
        val recyclerView = inflater.inflate(info.anodsplace.headunit.R.layout.fragment_list, container, false) as android.support.v7.widget.RecyclerView

        val context = activity

        settings = Settings(context)
        adapter = UsbListFragment.DeviceAdapter(context, settings)
        recyclerView.layoutManager = android.support.v7.widget.LinearLayoutManager(context)
        recyclerView.adapter = adapter

        usbReceiver = info.anodsplace.headunit.connection.UsbReceiver(this)

        return recyclerView
    }

    override fun onResume() {
        super.onResume()
        val allowDevices = settings.allowedDevices
        adapter.setData(createDeviceList(allowDevices), allowDevices)
        registerReceiver(usbReceiver, info.anodsplace.headunit.connection.UsbReceiver.Companion.createFilter())
    }

    override fun onPause() {
        super.onPause()
        settings.commit()
        unregisterReceiver(usbReceiver)
    }

    override fun onUsbDetach(device: android.hardware.usb.UsbDevice) {
        val allowDevices = settings.allowedDevices
        adapter.setData(createDeviceList(allowDevices), allowDevices)
    }

    override fun onUsbAttach(device: android.hardware.usb.UsbDevice) {
        val allowDevices = settings.allowedDevices
        adapter.setData(createDeviceList(allowDevices), allowDevices)
    }

    override fun onUsbPermission(granted: Boolean, connect: Boolean, device: android.hardware.usb.UsbDevice) {
        val allowDevices = settings.allowedDevices
        adapter.setData(createDeviceList(allowDevices), allowDevices)
    }

    private class DeviceViewHolder internal constructor(itemView: android.view.View) : android.support.v7.widget.RecyclerView.ViewHolder(itemView) {
        internal val allowButton = itemView.findViewById<Button>(android.R.id.button1)
        internal val startButton = itemView.findViewById<Button>(android.R.id.button2)
    }

    private class DeviceAdapter internal constructor(private val mContext: android.content.Context, private val mSettings: info.anodsplace.headunit.utils.Settings) : android.support.v7.widget.RecyclerView.Adapter<DeviceViewHolder>(), android.view.View.OnClickListener {
        private var allowedDevices: MutableSet<String> = mutableSetOf()
        private var deviceList: List<info.anodsplace.headunit.connection.UsbDeviceCompat> = listOf()


        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): info.anodsplace.headunit.main.UsbListFragment.DeviceViewHolder {
            val view = android.view.LayoutInflater.from(mContext).inflate(info.anodsplace.headunit.R.layout.list_item_device, parent, false)
            return info.anodsplace.headunit.main.UsbListFragment.DeviceViewHolder(view)
        }

        override fun onBindViewHolder(holder: info.anodsplace.headunit.main.UsbListFragment.DeviceViewHolder, position: Int) {
            val device = deviceList[position]

            holder.startButton.text = android.text.Html.fromHtml(String.format(
                    java.util.Locale.US, "<b>%1\$s</b><br/>%2\$s",
                    device.uniqueName, device.deviceName
            ))
            holder.startButton.tag = position
            holder.startButton.setOnClickListener(this)

            if (device.isInAccessoryMode) {
                holder.allowButton.setText(info.anodsplace.headunit.R.string.allowed)
                holder.allowButton.setTextColor(mContext.resources.getColor(info.anodsplace.headunit.R.color.material_green_700))
                holder.allowButton.isEnabled = false
            } else {
                if (allowedDevices.contains(device.uniqueName)) {
                    holder.allowButton.setText(info.anodsplace.headunit.R.string.allowed)
                    holder.allowButton.setTextColor(mContext.resources.getColor(info.anodsplace.headunit.R.color.material_green_700))
                } else {
                    holder.allowButton.setText(info.anodsplace.headunit.R.string.ignored)
                    holder.allowButton.setTextColor(mContext.resources.getColor(info.anodsplace.headunit.R.color.material_orange_700))
                }
                holder.allowButton.tag = position
                holder.allowButton.isEnabled = true
                holder.allowButton.setOnClickListener(this)
            }
        }

        override fun getItemCount(): Int {
            return deviceList.size
        }

        override fun onClick(v: android.view.View) {
            val device = deviceList.get(v.tag as Int)
            if (v.id == android.R.id.button1) {
                if (allowedDevices.contains(device.uniqueName)) {
                    allowedDevices.remove(device.uniqueName)
                } else {
                    allowedDevices.add(device.uniqueName)
                }
                mSettings.allowedDevices = allowedDevices
                notifyDataSetChanged()
            } else {
                if (device.isInAccessoryMode) {
                    mContext.startService(info.anodsplace.headunit.aap.AapService.Companion.createIntent(device.wrappedDevice, mContext))
                } else {
                    val usbMode = info.anodsplace.headunit.connection.UsbAccessoryMode(mContext.getSystemService(android.content.Context.USB_SERVICE) as UsbManager)
                    if (usbMode.connectAndSwitch(device.wrappedDevice)) {
                        android.widget.Toast.makeText(mContext, "Success", android.widget.Toast.LENGTH_SHORT).show()
                    } else {
                        android.widget.Toast.makeText(mContext, "Failed", android.widget.Toast.LENGTH_SHORT).show()
                    }
                    notifyDataSetChanged()
                }
            }
        }

        internal fun setData(deviceList: List<info.anodsplace.headunit.connection.UsbDeviceCompat>, allowedDevices: Set<String>) {
            this.allowedDevices = allowedDevices.toMutableSet()
            this.deviceList = deviceList
            notifyDataSetChanged()
        }
    }

    private fun createDeviceList(allowDevices: Set<String>): List<info.anodsplace.headunit.connection.UsbDeviceCompat> {
        val manager = activity.getSystemService(android.content.Context.USB_SERVICE) as android.hardware.usb.UsbManager
        val devices = manager.deviceList
        val list = devices.entries.map { (_, device) ->
            info.anodsplace.headunit.connection.UsbDeviceCompat(device)
        }

        java.util.Collections.sort(list, java.util.Comparator<UsbDeviceCompat> { lhs, rhs ->
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

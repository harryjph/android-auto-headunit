package info.anodsplace.headunit.main

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import info.anodsplace.headunit.R
import info.anodsplace.headunit.aap.AapService
import info.anodsplace.headunit.connection.UsbAccessoryMode

import info.anodsplace.headunit.connection.UsbDeviceCompat
import info.anodsplace.headunit.connection.UsbReceiver
import info.anodsplace.headunit.utils.Settings

class UsbListFragment : Fragment() {
    private lateinit var adapter: DeviceAdapter
    private lateinit var settings: Settings

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val recyclerView = inflater.inflate(R.layout.fragment_list, container, false) as RecyclerView

        settings = Settings(context!!)
        adapter = DeviceAdapter(context!!, settings)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter

        return recyclerView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mainViewModel = ViewModelProviders.of(activity!!).get(MainViewModel::class.java)
        mainViewModel.usbDevices.observe(this, Observer {
            val allowDevices = settings.allowedDevices
            adapter.setData(it, allowDevices)
        })
    }

    override fun onPause() {
        super.onPause()
        settings.commit()
    }

    private class DeviceViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {
        internal val allowButton = itemView.findViewById<Button>(android.R.id.button1)
        internal val startButton = itemView.findViewById<Button>(android.R.id.button2)
    }

    private class DeviceAdapter internal constructor(private val mContext: Context, private val mSettings: Settings) : RecyclerView.Adapter<DeviceViewHolder>(), android.view.View.OnClickListener {
        private var allowedDevices: MutableSet<String> = mutableSetOf()
        private var deviceList: List<UsbDeviceCompat> = listOf()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
            val view = LayoutInflater.from(mContext).inflate(R.layout.list_item_device, parent, false)
            return DeviceViewHolder(view)
        }

        override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
            val device = deviceList[position]

            holder.startButton.text = Html.fromHtml(String.format(
                    java.util.Locale.US, "<b>%1\$s</b><br/>%2\$s",
                    device.uniqueName, device.deviceName
            ))
            holder.startButton.tag = position
            holder.startButton.setOnClickListener(this)

            if (device.isInAccessoryMode) {
                holder.allowButton.setText(info.anodsplace.headunit.R.string.allowed)
                holder.allowButton.setTextColor(mContext.resources.getColor(R.color.material_green_700))
                holder.allowButton.isEnabled = false
            } else {
                if (allowedDevices.contains(device.uniqueName)) {
                    holder.allowButton.setText(info.anodsplace.headunit.R.string.allowed)
                    holder.allowButton.setTextColor(mContext.resources.getColor(R.color.material_green_700))
                } else {
                    holder.allowButton.setText(info.anodsplace.headunit.R.string.ignored)
                    holder.allowButton.setTextColor(mContext.resources.getColor(R.color.material_orange_700))
                }
                holder.allowButton.tag = position
                holder.allowButton.isEnabled = true
                holder.allowButton.setOnClickListener(this)
            }
        }

        override fun getItemCount(): Int {
            return deviceList.size
        }

        override fun onClick(v: View) {
            val device = deviceList[v.tag as Int]
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
                    mContext.startService(AapService.createIntent(device.wrappedDevice, mContext))
                } else {
                    val usbMode = UsbAccessoryMode(mContext.getSystemService(Context.USB_SERVICE) as UsbManager)
                    if (usbMode.connectAndSwitch(device.wrappedDevice)) {
                        Toast.makeText(mContext, "Success", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(mContext, "Failed", Toast.LENGTH_SHORT).show()
                    }
                    notifyDataSetChanged()
                }
            }
        }

        internal fun setData(deviceList: List<UsbDeviceCompat>, allowedDevices: Set<String>) {
            this.allowedDevices = allowedDevices.toMutableSet()
            this.deviceList = deviceList
            notifyDataSetChanged()
        }
    }

}

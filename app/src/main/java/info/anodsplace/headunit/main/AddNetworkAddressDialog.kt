package info.anodsplace.headunit.main

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import androidx.fragment.app.DialogFragment
import info.anodsplace.headunit.R
import info.anodsplace.headunit.utils.AppLog
import java.net.InetAddress

/**
 * @author algavris
 * *
 * @date 15/11/2016.
 */

class AddNetworkAddressDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: android.os.Bundle?): android.app.Dialog {
        val builder = AlertDialog.Builder(activity)
        val content = LayoutInflater.from(activity)
                .inflate(R.layout.fragment_add_network_address, null, false)

        val first = content.findViewById<EditText>(info.anodsplace.headunit.R.id.first)
        val second = content.findViewById<EditText>(info.anodsplace.headunit.R.id.second)
        val third = content.findViewById<EditText>(info.anodsplace.headunit.R.id.third)
        val fourth = content.findViewById<EditText>(info.anodsplace.headunit.R.id.fourth)

        val ip = arguments!!.getSerializable("ip") as java.net.InetAddress
        val addr = ip.address

        first.setText("$($addr[0] and 0xFF)")
        second.setText("$($addr[1] and 0xFF)")
        third.setText("$($addr[2] and 0xFF)")

        fourth.requestFocus()

        builder.setView(content)
                .setTitle("Enter ip address")
                .setPositiveButton("Add") { _, _ ->
                    val newAddr = ByteArray(4)
                    try {
                        newAddr[0] = strToByte(first.text.toString())
                        newAddr[1] = strToByte(second.text.toString())
                        newAddr[2] = strToByte(third.text.toString())
                        newAddr[3] = strToByte(fourth.text.toString())

                        val f = fragmentManager!!.findFragmentByTag(NetworkListFragment.TAG) as NetworkListFragment
                        f.addAddress(java.net.InetAddress.getByAddress(newAddr))
                    } catch (e: java.net.UnknownHostException) {
                        AppLog.e(e)
                    } catch (e: NumberFormatException) {
                        AppLog.e(e)
                    }
                }
                .setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.cancel() }
        return builder.create()
    }

    companion object {

        fun create(ip: InetAddress?) = AddNetworkAddressDialog().apply {
            arguments = Bundle()
            if (ip != null) {
                arguments!!.putSerializable("ip", ip)
            }
        }

        fun strToByte(str: String): Byte {
            val i = Integer.valueOf(str)
            return i.toByte()
        }
    }
}

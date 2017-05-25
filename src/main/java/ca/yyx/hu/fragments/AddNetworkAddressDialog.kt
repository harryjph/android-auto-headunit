package ca.yyx.hu.fragments

import android.app.AlertDialog
import android.app.Dialog
import android.app.DialogFragment
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText

import java.net.InetAddress
import java.net.UnknownHostException

import ca.yyx.hu.R
import ca.yyx.hu.utils.AppLog
import kotlin.experimental.and

/**
 * @author algavris
 * *
 * @date 15/11/2016.
 */

class AddNetworkAddressDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(activity)
        val content = LayoutInflater.from(activity).inflate(R.layout.fragment_add_network_address, null, false)


        val first = content.findViewById(R.id.first) as EditText
        val second = content.findViewById(R.id.second) as EditText
        val third = content.findViewById(R.id.third) as EditText
        val fourth = content.findViewById(R.id.fourth) as EditText

        val ip = arguments.getSerializable("ip") as InetAddress
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

                        val f = fragmentManager.findFragmentByTag(NetworkListFragment.TAG) as NetworkListFragment
                        f.addAddress(InetAddress.getByAddress(newAddr))
                    } catch (e: UnknownHostException) {
                        AppLog.e(e)
                    } catch (e: NumberFormatException) {
                        AppLog.e(e)
                    }
                }
                .setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.cancel() }
        return builder.create()
    }

    companion object {

        fun create(ip: InetAddress?): AddNetworkAddressDialog {

            val dialog = AddNetworkAddressDialog()

            val args = Bundle()
            if (ip != null) {
                args.putSerializable("ip", ip)
            }
            dialog.arguments = args

            return dialog

        }

        internal fun strToByte(str: String): Byte {
            val i = Integer.valueOf(str)
            return i.toByte()
        }
    }
}

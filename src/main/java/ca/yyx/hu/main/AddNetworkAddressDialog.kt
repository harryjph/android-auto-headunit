package ca.yyx.hu.main

/**
 * @author algavris
 * *
 * @date 15/11/2016.
 */

class AddNetworkAddressDialog : android.app.DialogFragment() {

    override fun onCreateDialog(savedInstanceState: android.os.Bundle?): android.app.Dialog {
        val builder = android.app.AlertDialog.Builder(activity)
        val content = android.view.LayoutInflater.from(activity).inflate(ca.yyx.hu.R.layout.fragment_add_network_address, null, false)


        val first = content.findViewById(ca.yyx.hu.R.id.first) as android.widget.EditText
        val second = content.findViewById(ca.yyx.hu.R.id.second) as android.widget.EditText
        val third = content.findViewById(ca.yyx.hu.R.id.third) as android.widget.EditText
        val fourth = content.findViewById(ca.yyx.hu.R.id.fourth) as android.widget.EditText

        val ip = arguments.getSerializable("ip") as java.net.InetAddress
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
                        newAddr[0] = ca.yyx.hu.main.AddNetworkAddressDialog.Companion.strToByte(first.text.toString())
                        newAddr[1] = ca.yyx.hu.main.AddNetworkAddressDialog.Companion.strToByte(second.text.toString())
                        newAddr[2] = ca.yyx.hu.main.AddNetworkAddressDialog.Companion.strToByte(third.text.toString())
                        newAddr[3] = ca.yyx.hu.main.AddNetworkAddressDialog.Companion.strToByte(fourth.text.toString())

                        val f = fragmentManager.findFragmentByTag(NetworkListFragment.Companion.TAG) as NetworkListFragment
                        f.addAddress(java.net.InetAddress.getByAddress(newAddr))
                    } catch (e: java.net.UnknownHostException) {
                        ca.yyx.hu.utils.AppLog.e(e)
                    } catch (e: NumberFormatException) {
                        ca.yyx.hu.utils.AppLog.e(e)
                    }
                }
                .setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.cancel() }
        return builder.create()
    }

    companion object {

        fun create(ip: java.net.InetAddress?): ca.yyx.hu.main.AddNetworkAddressDialog {

            val dialog = ca.yyx.hu.main.AddNetworkAddressDialog()

            val args = android.os.Bundle()
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

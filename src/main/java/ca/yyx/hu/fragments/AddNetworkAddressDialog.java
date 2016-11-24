package ca.yyx.hu.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import java.net.InetAddress;
import java.net.UnknownHostException;

import ca.yyx.hu.R;
import ca.yyx.hu.utils.AppLog;

/**
 * @author algavris
 * @date 15/11/2016.
 */

public class AddNetworkAddressDialog extends DialogFragment {

    public static AddNetworkAddressDialog create(InetAddress ip) {

        AddNetworkAddressDialog dialog = new AddNetworkAddressDialog();

        Bundle args = new Bundle();
        if (ip != null) {
            args.putSerializable("ip", ip);
        }
        dialog.setArguments(args);

        return dialog;

    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {


        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        View content = LayoutInflater.from(getActivity()).inflate(R.layout.fragment_add_network_address, null, false);


        final EditText first = (EditText) content.findViewById(R.id.first);
        final EditText second = (EditText) content.findViewById(R.id.second);
        final EditText third = (EditText) content.findViewById(R.id.third);
        final EditText fourth = (EditText) content.findViewById(R.id.fourth);

        InetAddress ip = (InetAddress) getArguments().getSerializable("ip");
        if (ip != null) {
            byte[] addr = ip.getAddress();

            first.setText(String.valueOf(addr[0] & 0xFF));
            second.setText(String.valueOf(addr[1] & 0xFF));
            third.setText(String.valueOf(addr[2] & 0xFF));
        }
        fourth.requestFocus();

        builder.setView(content)
                .setTitle("Enter ip address")
                .setPositiveButton("Add", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        byte[] addr = new byte[4];
                        try {
                            addr[0] = strToByte(first.getText().toString());
                            addr[1] = strToByte(second.getText().toString());
                            addr[2] = strToByte(third.getText().toString());
                            addr[3] = strToByte(fourth.getText().toString());

                            InetAddress ip = InetAddress.getByAddress(addr);
                            NetworkListFragment f= (NetworkListFragment) getFragmentManager().findFragmentByTag(NetworkListFragment.TAG);
                            f.addAddress(ip);
                        } catch (UnknownHostException | NumberFormatException e) {
                            AppLog.e(e);
                        }
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        AddNetworkAddressDialog.this.getDialog().cancel();
                    }
                });
        return builder.create();
    }

    static byte strToByte(String str) {
        int i = Integer.valueOf(str);
        return (byte)i;
    }
}

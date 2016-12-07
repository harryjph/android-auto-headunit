package ca.yyx.hu.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import net.hockeyapp.android.CrashManager;
import net.hockeyapp.android.UpdateManager;

import ca.yyx.hu.App;
import ca.yyx.hu.R;
import ca.yyx.hu.aap.AapProjectionActivity;
import ca.yyx.hu.aap.AapService;
import ca.yyx.hu.fragments.NetworkListFragment;
import ca.yyx.hu.fragments.UsbListFragment;
import ca.yyx.hu.utils.SystemUI;

public class MainActivity extends Activity {
    private View mVideoButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        mVideoButton = findViewById(R.id.video_button);
        mVideoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent aapIntent = new Intent(MainActivity.this, AapProjectionActivity.class);
                aapIntent.putExtra(AapProjectionActivity.EXTRA_FOCUS, true);
                startActivity(aapIntent);
            }
        });

        findViewById(R.id.usb).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getFragmentManager()
                        .beginTransaction()
                        .replace(R.id.main_content, new UsbListFragment())
                        .commit();
            }
        });

        UpdateManager.register(this);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        SystemUI.hide(getWindow().getDecorView());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (App.get(this).transport().isAlive()) {
            mVideoButton.setEnabled(true);
        } else {
            mVideoButton.setEnabled(false);
        }
        CrashManager.register(this);
    }

}

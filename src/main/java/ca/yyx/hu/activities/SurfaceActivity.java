package ca.yyx.hu.activities;

import android.app.Activity;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import ca.yyx.hu.App;
import ca.yyx.hu.R;
import ca.yyx.hu.decoder.VideoDecoder;
import ca.yyx.hu.utils.SystemUI;
import ca.yyx.hu.utils.AppLog;


public class SurfaceActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);   // !! Keep Screen on !!
        setContentView(R.layout.activity_headunit);
        getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int visibility) {
                SystemUI.hide(getWindow().getDecorView());
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        SystemUI.hide(getWindow().getDecorView());
    }

}

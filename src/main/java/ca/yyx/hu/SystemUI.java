package ca.yyx.hu;

import android.support.v4.widget.DrawerLayout;
import android.view.Gravity;
import android.view.View;

/**
 * @author algavris
 * @date 28/04/2016.
 */
public class SystemUI {

    public static void hide(View content, DrawerLayout drawer) {

        if (drawer != null) {
            drawer.closeDrawer(Gravity.LEFT);
        }
        // Set the IMMERSIVE flag. Set the content to appear under the system bars so that the content doesn't resize when the system bars hide and show.
        content.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                        | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                        | View.SYSTEM_UI_FLAG_IMMERSIVE);//_STICKY);
    }

    // Show the system bars by removing all the flags except for the ones that make the content appear under the system bars.
    public static void show(View content) {
        content.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }
}

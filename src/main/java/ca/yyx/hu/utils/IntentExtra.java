package ca.yyx.hu.utils;

import android.content.Intent;
import android.support.annotation.Nullable;

/**
 * @author algavris
 * @date 18/06/2016.
 */

public class IntentExtra {

    public static Boolean get(String name, boolean defaultValue,@Nullable Intent intent) {
        return intent != null && intent.getBooleanExtra(name, defaultValue);
    }

}

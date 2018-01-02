package info.anodsplace.headunit.utils

import android.content.IntentFilter
import info.anodsplace.headunit.contract.*

/**
 * @author algavris
 * @date 22/12/2017
 */
object IntentFilters {
    val disconnect = IntentFilter(DisconnectIntent.action)
    val locationUpdate = IntentFilter(LocationUpdateIntent.action)
    val keyEvent = IntentFilter(KeyIntent.action)
    val mediaKeyEvent = IntentFilter(MediaKeyIntent.action)
}
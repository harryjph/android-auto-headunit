package info.anodsplace.headunit.utils

import android.content.IntentFilter
import info.anodsplace.headunit.contract.*

object IntentFilters {
    val disconnect = IntentFilter(DisconnectIntent.action)
    val keyEvent = IntentFilter(KeyIntent.action)
}

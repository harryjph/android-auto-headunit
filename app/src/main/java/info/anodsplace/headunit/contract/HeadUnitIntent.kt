package info.anodsplace.headunit.contract

import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.view.KeyEvent

object HeadUnit {
    const val packageName = "info.anodsplace.info.headunit"
}

class ConnectedIntent: Intent(action) {
    companion object {
        const val action = "${HeadUnit.packageName}.ACTION_CONNECTED"
    }
}

class DisconnectIntent : Intent(action) {

    companion object {
        const val action = "${HeadUnit.packageName}.ACTION_DISCONNECT"
    }
}

class KeyIntent(event: KeyEvent): Intent(action) {
    init {
        putExtra(extraEvent, event)
    }

    companion object {
        const val extraEvent = "event"
        const val action = "${HeadUnit.packageName}.ACTION_KEYPRESS"
    }
}

class MediaKeyIntent(event: KeyEvent): Intent(action) {
    init {
        putExtra(KeyIntent.extraEvent, event)
    }

    companion object {
        const val action = "${HeadUnit.packageName}.ACTION_MEDIA_KEYPRESS"
    }
}

class LocationUpdateIntent(location: Location): Intent(action) {
    init {
        putExtra(LocationManager.KEY_LOCATION_CHANGED, location)
    }

    companion object {
        const val action = "${HeadUnit.packageName}.LOCATION_UPDATE"

        fun extractLocation(intent: Intent): Location {
            return intent.getParcelableExtra(LocationManager.KEY_LOCATION_CHANGED)
        }
    }
}

class ProjectionActivityRequest: Intent(action) {
    companion object {
        const val action = "${HeadUnit.packageName}.ACTION_REQUEST_PROJECTION"
    }
}
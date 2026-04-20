package com.dangerfield.hiittimer.libraries.inappmessages

import com.dangerfield.hiittimer.libraries.preferences.Preference

class MessageShownCountPref(messageId: String) : Preference<Int>() {
    override val key: String = "app.inappmessage.$messageId.shown_count"
    override val default: Int = 0
}

class MessageLastShownMsPref(messageId: String) : Preference<Long>() {
    override val key: String = "app.inappmessage.$messageId.last_shown_ms"
    override val default: Long = 0L
}

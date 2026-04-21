package com.dangerfield.hiittimer.features.settings

import com.dangerfield.hiittimer.libraries.preferences.Preference

/**
 * Master toggle for the shake-to-report-bug flow. Enabled by default so feedback is easy to
 * surface, but users who shake their phone while working out can opt out from the dialog itself.
 */
object ShakeToReportEnabledPref : Preference<Boolean>() {
    override val key: String = "settings.shake_to_report_enabled"
    override val default: Boolean = true
}

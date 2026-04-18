package com.dangerfield.hiittimer.libraries.core

import com.dangerfield.hiittimer.buildinfo.HIITTimerBuildConfig

object SupabaseInfo {
    val projectId: String
        get() = HIITTimerBuildConfig.SUPABASE_PROJECT_ID

    val url: String
        get() = HIITTimerBuildConfig.SUPABASE_URL

    val anonKey: String
        get() = HIITTimerBuildConfig.SUPABASE_ANON_KEY
}

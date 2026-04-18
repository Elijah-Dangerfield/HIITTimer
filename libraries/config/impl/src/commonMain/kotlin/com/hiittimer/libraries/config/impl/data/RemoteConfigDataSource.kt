package com.dangerfield.hiittimer.libraries.config.impl.data

import com.dangerfield.hiittimer.libraries.config.AppConfigMap
import com.dangerfield.hiittimer.libraries.core.Catching

interface RemoteConfigDataSource {
    suspend fun getConfig(): Catching<AppConfigMap>
}

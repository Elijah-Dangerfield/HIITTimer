package com.dangerfield.hiittimer.libraries.storage.impl.db

import com.dangerfield.hiittimer.features.timers.storage.db.TimerDao
import com.dangerfield.hiittimer.libraries.hiittimer.storage.db.SessionDao
import com.dangerfield.hiittimer.libraries.hiittimer.storage.db.UserDao
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, boundType = UserDao::class)
class ProvideUserDao @Inject constructor(
    provider: AppDatabaseProvider
) : UserDao by provider.database.userDao()

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, boundType = SessionDao::class)
class ProvideSessionDao @Inject constructor(
    provider: AppDatabaseProvider
) : SessionDao by provider.database.sessionDao()

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, boundType = TimerDao::class)
class ProvideTimerDao @Inject constructor(
    provider: AppDatabaseProvider
) : TimerDao by provider.database.timerDao()

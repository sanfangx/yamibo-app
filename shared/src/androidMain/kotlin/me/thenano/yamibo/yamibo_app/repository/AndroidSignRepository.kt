package me.thenano.yamibo.yamibo_app.repository

import me.thenano.yamibo.yamibo_app.Database
import me.thenano.yamibo.yamibo_app.db.DatabaseFactory
import me.thenano.yamibo.yamibo_app.repository.sign.SignRepositoryImpl
import me.thenano.yamibo.yamibo_app.repository.settings.AppSettingsRepository

class AndroidSignRepository(
    dbFactory: DatabaseFactory,
    authRepository: AuthRepository,
    appSettingsRepository: AppSettingsRepository,
) : SignRepository by SignRepositoryImpl(
    db = Database(dbFactory.createDriver()),
    authRepository = authRepository,
    appSettingsRepository = appSettingsRepository,
)

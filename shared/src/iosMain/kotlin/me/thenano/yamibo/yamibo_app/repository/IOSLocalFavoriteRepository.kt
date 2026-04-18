package me.thenano.yamibo.yamibo_app.repository

import me.thenano.yamibo.yamibo_app.Database
import me.thenano.yamibo.yamibo_app.db.DatabaseFactory
import me.thenano.yamibo.yamibo_app.repository.favorite.LocalFavoriteRepositoryImpl

class IOSLocalFavoriteRepository(
    dbFactory: DatabaseFactory
) : LocalFavoriteRepository by LocalFavoriteRepositoryImpl(
    db = Database(dbFactory.createDriver())
)

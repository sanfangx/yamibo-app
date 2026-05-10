package me.thenano.yamibo.yamibo_app.repository

import me.thenano.yamibo.yamibo_app.Database
import me.thenano.yamibo.yamibo_app.db.DatabaseFactory
import me.thenano.yamibo.yamibo_app.repository.bookmark.LocalBookMarkRepositoryImpl

class AndroidLocalBookMarkRepository(
    dbFactory: DatabaseFactory,
) : LocalBookMarkRepository by LocalBookMarkRepositoryImpl(
    db = Database(dbFactory.createDriver()),
)

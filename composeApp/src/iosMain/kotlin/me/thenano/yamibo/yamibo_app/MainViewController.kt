package me.thenano.yamibo.yamibo_app

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import io.github.littlesurvival.YamiboClient
import me.thenano.yamibo.yamibo_app.navigation.ComposableNavigator
import me.thenano.yamibo.yamibo_app.navigation.LocalNavigator
import me.thenano.yamibo.yamibo_app.repository.IOSAuthRepository
import me.thenano.yamibo.yamibo_app.repository.IOSForumRepository
import me.thenano.yamibo.yamibo_app.repository.IOSThemeRepository
import me.thenano.yamibo.yamibo_app.repository.IOSThreadRepository
import me.thenano.yamibo.yamibo_app.store.IOSCookieStore
import me.thenano.yamibo.yamibo_app.store.IOSUserStore

fun MainViewController() = ComposeUIViewController {
    /** Navigator Logic */
    val navigator = remember { ComposableNavigator() }

    /** Store Logic */
    val cookieStore = remember { IOSCookieStore() }
    val userStore = remember { IOSUserStore() }

    /** Repository Logic */
    val yamiboClient = remember { YamiboClient() }
    val authRepository = remember { IOSAuthRepository(cookieStore, userStore, yamiboClient) }
    val forumRepository = remember { IOSForumRepository(cookieStore, yamiboClient) }
    val threadRepository = remember { IOSThreadRepository(cookieStore, yamiboClient) }
    val themeRepository = remember { IOSThemeRepository() }

    /** Provide Repositories */
    CompositionLocalProvider(
            LocalNavigator provides navigator,
            LocalAuthRepository provides authRepository,
            LocalForumRepository provides forumRepository,
            LocalThreadRepository provides threadRepository,
            LocalThemeRepository provides themeRepository,
    ) { App() }
}

package me.thenano.yamibo.yamibo_app

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import me.thenano.yamibo.yamibo_app.home.HomePageScreen
import me.thenano.yamibo.yamibo_app.navigation.LocalNavigator
import me.thenano.yamibo.yamibo_app.navigation.NavAction
import org.jetbrains.compose.ui.tooling.preview.Preview
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory

@Composable
fun HomeScreenContent() {
    HomePageScreen()
}

@Composable
@Preview
fun App() {
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .components {
                add(KtorNetworkFetcherFactory())
            }
            .build()
    }

    val navigator = LocalNavigator.current
    val holder = rememberSaveableStateHolder()
    navigator.stateHolder = holder

    val navigatable = navigator.currentScreen

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = me.thenano.yamibo.yamibo_app.theme.YamiboTheme.colors.creamBackground
    ) {
        AnimatedContent(
            targetState = navigatable,
            transitionSpec = {
                val duration = 300
                when (navigator.lastAction) {
                    NavAction.Pop -> {
                        slideIntoContainer(
                            towards =
                                AnimatedContentTransitionScope.SlideDirection.Right,
                            animationSpec = tween(duration)
                        )
                            .togetherWith(
                                slideOutOfContainer(
                                    towards =
                                        AnimatedContentTransitionScope
                                            .SlideDirection.Right,
                                    animationSpec = tween(duration)
                                )
                            )
                    }

                    NavAction.Push -> {
                        slideIntoContainer(
                            towards =
                                AnimatedContentTransitionScope.SlideDirection.Left,
                            animationSpec = tween(duration)
                        )
                            .togetherWith(
                                slideOutOfContainer(
                                    towards =
                                        AnimatedContentTransitionScope
                                            .SlideDirection.Left,
                                    animationSpec = tween(duration)
                                )
                            )
                    }

                    else -> {
                        fadeIn(animationSpec = tween(duration))
                            .togetherWith(fadeOut(animationSpec = tween(duration)))
                    }
                }
            },
            label = "app_navigation"
        ) { targetNavigatable ->
            holder.SaveableStateProvider(targetNavigatable.id) { targetNavigatable.Content() }
        }
    }
}

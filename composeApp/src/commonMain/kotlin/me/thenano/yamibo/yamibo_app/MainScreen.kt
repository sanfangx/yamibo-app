package me.thenano.yamibo.yamibo_app

import YamiboIcons
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.serialization.Serializable
import me.thenano.yamibo.yamibo_app.favorite.FavoritePage
import me.thenano.yamibo.yamibo_app.history.ReadHistoryPage
import me.thenano.yamibo.yamibo_app.message.MessageCenterScreen
import me.thenano.yamibo.yamibo_app.message.MessageCenterTab
import me.thenano.yamibo.yamibo_app.navigation.LocalNavigator
import me.thenano.yamibo.yamibo_app.navigation.Navigatable
import me.thenano.yamibo.yamibo_app.navigation.RestorableNavigatable
import me.thenano.yamibo.yamibo_app.navigation.RestorableScreenEntry
import me.thenano.yamibo.yamibo_app.navigation.RestorableScreenSnapshot
import me.thenano.yamibo.yamibo_app.navigation.TypedRestorableNavigatableDecoder
import me.thenano.yamibo.yamibo_app.navigation.decodeRestorePayload
import me.thenano.yamibo.yamibo_app.navigation.restoreSnapshot
import me.thenano.yamibo.yamibo_app.profile.ProfilePage
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import yamibo_app.composeapp.generated.resources.Res
import yamibo_app.composeapp.generated.resources.*

enum class MainTab(val icon: ImageVector) {
    Home(YamiboIcons.Home),
    History(YamiboIcons.History),
    Message(YamiboIcons.Message),
    Favorite(YamiboIcons.Explore),
    Profile(YamiboIcons.Profile)
}

@Serializable
private data class MainScreenRestorePayload(
    val initialTabName: String = MainTab.Home.name,
)
@RestorableScreenEntry
class IMainScreen(val initialTab: MainTab = MainTab.Home) : RestorableNavigatable {
    override val id = buildId(initialTab.name)
    override val restoreDecoder = Decoder

    override fun toRestoreSnapshot(): RestorableScreenSnapshot = restoreSnapshot(
        decoder = restoreDecoder,
        payload = MainScreenRestorePayload(initialTabName = initialTab.name),
    )

    @Composable
    override fun Content() {
        MainScreen(initialTab)
    }

    companion object Decoder : TypedRestorableNavigatableDecoder<IMainScreen>(IMainScreen::class) {
        override fun decode(payload: String): RestorableNavigatable {
            val data = decodeRestorePayload<MainScreenRestorePayload>(payload)
            return IMainScreen(initialTab = MainTab.valueOf(data.initialTabName))
        }
    }
}

data class BottomNavItem(
    val tab: MainTab,
    val title: String,
    val icon: ImageVector,
    val showBadge: Boolean = false,
)

@Composable
fun MainScreen(initialTab: MainTab = MainTab.Home) {
    val colors = YamiboTheme.colors
    val navigator = LocalNavigator.current
    var currentTab by rememberSaveable { mutableStateOf(initialTab) }
    var reTapHistoryToken by remember { mutableIntStateOf(0) }
    var hasNewMessage by rememberSaveable { mutableStateOf(false) }

    DisposableEffect(currentTab) {
        val handler = {
            if (navigator.currentScreen is IMainScreen && currentTab != MainTab.Home) {
                currentTab = MainTab.Home
                true
            } else {
                false
            }
        }
        navigator.backHandlers.add(handler)
        onDispose { navigator.backHandlers.remove(handler) }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().navigationBarsPadding(),
        containerColor = colors.creamBackground,
        contentWindowInsets = WindowInsets(0.dp),
        bottomBar = {
            MainScreenBottomBar(
                tabs = MainTab.entries.map {
                    BottomNavItem(
                        tab = it,
                        title = stringResource(it.titleResource()),
                        icon = it.icon,
                        showBadge = it == MainTab.Message && hasNewMessage,
                    )
                },
                currentTab = BottomNavItem(
                    tab = currentTab,
                    title = stringResource(currentTab.titleResource()),
                    icon = currentTab.icon,
                    showBadge = currentTab == MainTab.Message && hasNewMessage,
                ),
                onTabSelected = { selected ->
                    val newTab = selected.tab
                    if (newTab == MainTab.History && currentTab == MainTab.History) {
                        reTapHistoryToken++
                    }
                    currentTab = newTab
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier =
                Modifier.padding(paddingValues)
                    .fillMaxSize()
                    .background(colors.creamBackground)
        ) {
            MainTab.entries.forEach { tab ->
                val selected = tab == currentTab
                val alpha by animateFloatAsState(
                    targetValue = if (selected) 1f else 0f,
                    animationSpec = tween(durationMillis = 220),
                    label = "MainScreenTabAlpha_${tab.name}",
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(if (selected) Modifier else Modifier.size(0.dp))
                        .zIndex(if (selected) 1f else 0f)
                        .graphicsLayer {
                            this.alpha = alpha
                        }
                ) {
                    when (tab) {
                        MainTab.Home -> HomeScreenContent(
                            onNewMessageStatusChange = { hasNewMessage = it },
                        )
                        MainTab.History -> ReadHistoryPage(reTapHistoryToken)
                        MainTab.Message -> MessageCenterScreen(
                            initialTab = MessageCenterTab.PrivateMessages,
                            mainTabTopBar = true,
                            onPrivateMessageUnreadChange = { hasNewMessage = it },
                        )
                        MainTab.Favorite -> FavoritePage()
                        MainTab.Profile -> ProfilePage()
                    }
                }
            }
        }
    }
}

private fun MainTab.titleResource(): StringResource {
    return when (this) {
        MainTab.Home -> Res.string.main_home
        MainTab.History -> Res.string.main_history
        MainTab.Message -> Res.string.main_message
        MainTab.Favorite -> Res.string.main_favorite
        MainTab.Profile -> Res.string.main_profile
    }
}

@Composable
fun MainScreenBottomBar(
    tabs: List<BottomNavItem>,
    currentTab: BottomNavItem,
    onTabSelected: (BottomNavItem) -> Unit
) {
    val colors = YamiboTheme.colors
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .height(56.dp)
                .background(colors.navBarBg)
                .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tabs.forEach { tab ->
            val selected = tab == currentTab
            val color by
            animateColorAsState(
                targetValue = if (selected) colors.navBarIconSelected else colors.navBarIconUnselected,
                animationSpec = spring(stiffness = Spring.StiffnessLow)
            )

            Column(
                modifier =
                    Modifier.weight(1f).clickable(
                        interactionSource =
                            remember { MutableInteractionSource() },
                        indication = null
                    ) { onTabSelected(tab) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.title,
                        tint = color,
                        modifier = Modifier.size(24.dp)
                    )
                    if (tab.showBadge) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 3.dp, y = (-2).dp)
                                .size(8.dp)
                                .background(colors.redAccent, CircleShape)
                        )
                    }
                }
                Text(text = tab.title, color = color, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun PlaceholderScreen(name: String) {
    val colors = YamiboTheme.colors
    Box(
        modifier = Modifier.fillMaxSize().background(colors.creamBackground),
        contentAlignment = Alignment.Center
    ) { Text("Content for $name") }
}

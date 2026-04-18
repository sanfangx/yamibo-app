package me.thenano.yamibo.yamibo_app.profile

import YamiboIcons
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.littlesurvival.core.YamiboResult
import kotlinx.coroutines.launch
import me.thenano.yamibo.yamibo_app.LocalAppSettingsRepository
import me.thenano.yamibo.yamibo_app.LocalAuthRepository
import me.thenano.yamibo.yamibo_app.LocalSignRepository
import me.thenano.yamibo.yamibo_app.event.AppEventBus
import me.thenano.yamibo.yamibo_app.event.events.LoginSuccessEvent
import me.thenano.yamibo.yamibo_app.navigation.LocalNavigator
import me.thenano.yamibo.yamibo_app.profile.settings.ISettingsScreen
import me.thenano.yamibo.yamibo_app.profile.sign.ISignInfoScreen
import me.thenano.yamibo.yamibo_app.profile.sign.ISignWebView
import me.thenano.yamibo.yamibo_app.repository.settings.SignInMode
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme.colors

@Composable
fun ProfilePage() {
    val authRepository = LocalAuthRepository.current
    val signRepository = LocalSignRepository.current
    val appSettingsRepository = LocalAppSettingsRepository.current
    val navigator = LocalNavigator.current
    val coroutineScope = rememberCoroutineScope()
    val colors = colors
    val snackbarHostState = remember { SnackbarHostState() }

    var userInfo by remember { mutableStateOf(authRepository.currentUser()) }
    var isLoading by remember { mutableStateOf(false) }
    var signButtonTitle by remember { mutableStateOf("每日簽到") }
    var signRefreshKey by remember { mutableIntStateOf(0) }

    fun refreshSignStatus() {
        coroutineScope.launch {
            signButtonTitle = when {
                userInfo == null -> "每日簽到"
                signRepository.getCachedPageInfo()?.hasSignedToday == true -> "今日已簽到"
                signRepository.getCachedPageInfo()?.hasSignedToday == false -> "每日簽到"
                signRepository.isSignedToday() -> "今日已簽到"
                else -> {
                    when (val result = signRepository.fetchPageInfo()) {
                        is YamiboResult.Success -> {
                            if (result.value.hasSignedToday) "今日已簽到" else "每日簽到"
                        }

                        is YamiboResult.Failure -> {
                            when {
                                signRepository.getCachedPageInfo()?.hasSignedToday == true -> "今日已簽到"
                                else -> "每日簽到"
                            }
                        }

                        is YamiboResult.NotLoggedIn -> "每日簽到"
                        is YamiboResult.NoPermission -> "每日簽到"
                        is YamiboResult.Maintenance -> "每日簽到"
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        AppEventBus.events.collect { event ->
            if (event == LoginSuccessEvent) {
                userInfo = authRepository.currentUser()
                signRefreshKey += 1
            }
        }
    }

    LaunchedEffect(userInfo, signRefreshKey) {
        refreshSignStatus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.creamBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            UserProfileCard(
                userInfo = userInfo,
                isLoading = isLoading,
                onRefresh = {
                    coroutineScope.launch {
                        isLoading = true
                        authRepository.fetchStatus()
                        userInfo = authRepository.currentUser()
                        signRefreshKey += 1
                        isLoading = false
                    }
                },
                onLogout = {
                    coroutineScope.launch {
                        isLoading = true
                        authRepository.logOut()
                        userInfo = authRepository.currentUser()
                        signRefreshKey += 1
                        isLoading = false
                    }
                },
                modifier = Modifier.padding(top = 12.dp)
            )
            Spacer(Modifier.height(8.dp))

            SignEntryCard(
                title = signButtonTitle,
                onSignClick = {
                    if (userInfo == null) {
                        return@SignEntryCard
                    }
                    when (appSettingsRepository.signInMode.getValue()) {
                        SignInMode.FULL_MANUAL -> {
                            navigator.navigate(
                                ISignWebView(
                                    semiAutomatic = false,
                                    onResultObserved = {
                                        coroutineScope.launch {
                                            authRepository.syncCookieFromWebView()
                                            when (signRepository.fetchPageInfo()) {
                                                is YamiboResult.Success -> {
                                                    signRefreshKey += 1
                                                    refreshSignStatus()
                                                }

                                                else -> Unit
                                            }
                                        }
                                    }
                                )
                            )
                        }

                        SignInMode.SEMI_AUTOMATIC -> {
                            navigator.navigate(
                                ISignWebView(
                                    semiAutomatic = true,
                                    onSemiAutoReady = {
                                        coroutineScope.launch {
                                            authRepository.syncCookieFromWebView()
                                            when (val result = signRepository.runAutoSign(appSettingsRepository.signInAllowRepair.getValue())) {
                                                is YamiboResult.Success -> {
                                                    signRefreshKey += 1
                                                }

                                                else -> Unit
                                            }
                                            refreshSignStatus()
                                        }
                                    },
                                    onMaintenanceObserved = {
                                        coroutineScope.launch {
                                            snackbarHostState.currentSnackbarData?.dismiss()
                                            snackbarHostState.showSnackbar("百合會維護中...現在不是簽到的好時機呢")
                                        }
                                    },
                                )
                            )
                        }
                    }
                },
                onInfoClick = {
                    navigator.navigate(
                        ISignInfoScreen(
                            onInfoLoaded = {
                                signRefreshKey += 1
                            }
                        )
                    )
                },
            )

            EntryCard(
                title = "設定",
                icon = YamiboIcons.Setting,
                onClick = { navigator.navigate(ISettingsScreen()) }
            )

            Spacer(Modifier.height(24.dp))
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp)
        ) { data ->
            Snackbar(
                snackbarData = data,
                containerColor = colors.brownDeep,
                contentColor = colors.creamSurface,
                shape = RoundedCornerShape(12.dp),
            )
        }
    }
}

@Composable
private fun SignEntryCard(
    title: String,
    onSignClick: () -> Unit,
    onInfoClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = colors.creamSurface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onSignClick)
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = YamiboIcons.EditOrSign,
                    contentDescription = "每日簽到",
                    tint = colors.brownPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.size(16.dp))
                Column {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.textDark
                    )
                }
            }

            Row(
                modifier = Modifier
                    .clickable(onClick = onInfoClick)
                    .padding(start = 0.dp, end = 8.dp, top = 18.dp, bottom = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = "|",
                    fontSize = 18.sp,
                    color = colors.brownLight.copy(alpha = 0.5f),
                )
                Text(
                    text = "!",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.brownPrimary,
                )
            }
        }
    }
}

@Composable
private fun EntryCard(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit = {},
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = colors.creamSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = colors.brownPrimary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.size(16.dp))
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textDark
            )
        }
    }
}

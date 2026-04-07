package me.thenano.yamibo.yamibo_app.profile

import YamiboIcons
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.thenano.yamibo.yamibo_app.LocalAuthRepository
import me.thenano.yamibo.yamibo_app.event.AppEventBus
import me.thenano.yamibo.yamibo_app.event.events.LoginSuccessEvent
import me.thenano.yamibo.yamibo_app.navigation.LocalNavigator
import me.thenano.yamibo.yamibo_app.profile.settings.ISettingsScreen
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme.colors

@Composable
fun ProfilePage() {
    val authRepository = LocalAuthRepository.current
    val navigator = LocalNavigator.current
    val colors = colors
    var userInfo by remember { mutableStateOf(authRepository.currentUser()) }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        AppEventBus.events.collect { event ->
            if (event == LoginSuccessEvent) {
                userInfo = authRepository.currentUser()
            }
        }
    }
    Column(
        modifier =
            Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .background(colors.creamBackground)
    ) {
        // top profile card
        UserProfileCard(
            userInfo = userInfo,
            isLoading = isLoading,
            onRefresh = {
                isLoading = true
                authRepository.fetchStatus()
                userInfo = authRepository.currentUser()
                isLoading = false
            },
            onLogout = {
                isLoading = true
                authRepository.logOut()
                userInfo = authRepository.currentUser()
                isLoading = false
            },
            modifier = Modifier.padding(top = 12.dp)
        )
        Spacer(Modifier.height(8.dp))

        // Settings entry
        EntryCard(title = "設定", icon = YamiboIcons.Setting, onClick = { navigator.navigate(ISettingsScreen()) })

        Spacer(Modifier.height(24.dp))
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
            Spacer(Modifier.width(16.dp))
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textDark
            )
        }
    }
}
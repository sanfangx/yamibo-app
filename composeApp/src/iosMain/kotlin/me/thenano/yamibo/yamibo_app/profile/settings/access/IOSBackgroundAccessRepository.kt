package me.thenano.yamibo.yamibo_app.profile.settings.access

import me.thenano.yamibo.yamibo_app.i18n.appString
import yamibo_app.composeapp.generated.resources.Res
import yamibo_app.composeapp.generated.resources.*

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class IOSBackgroundAccessRepository : BackgroundAccessRepository {
    private val _state = MutableStateFlow(
        BackgroundAccessRepository.SetupState(
            summary = appString(Res.string.auto_cc3c3071a5),
            items = listOf(
                BackgroundAccessRepository.SetupItem(
                    title = appString(Res.string.auto_f550e6a558),
                    subtitle = appString(Res.string.auto_a21ad701d5),
                    status = BackgroundAccessRepository.SetupStatus.Info,
                ),
            ),
            platformNote = appString(Res.string.auto_48d3656712),
        ),
    )

    override val state: StateFlow<BackgroundAccessRepository.SetupState> = _state

    override suspend fun refresh() = Unit

    override fun runAction(action: BackgroundAccessRepository.SetupAction) = Unit
}


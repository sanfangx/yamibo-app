package me.thenano.yamibo.yamibo_app.history.components

import YamiboIcons
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme
import me.thenano.yamibo.yamibo_app.components.YamiboMainTabIconAction
import me.thenano.yamibo.yamibo_app.components.YamiboMainTabTopBar
import org.jetbrains.compose.resources.stringResource
import yamibo_app.composeapp.generated.resources.Res
import yamibo_app.composeapp.generated.resources.*

@Composable
internal fun NormalTopBar(
    onSearch: () -> Unit,
    onMultiSelect: () -> Unit,
) {
    YamiboMainTabTopBar(
        title = stringResource(Res.string.read_history_title),
    ) {
        YamiboMainTabIconAction(YamiboIcons.Search, stringResource(Res.string.read_history_search), onSearch, iconSize = 28, iconOffsetY = 4)
        YamiboMainTabIconAction(YamiboIcons.Trashcan, stringResource(Res.string.read_history_multi_delete), onMultiSelect)
    }
}

@Composable
internal fun SearchTopBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onBack: () -> Unit,
    focusRequester: FocusRequester,
) {
    val colors = YamiboTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) { Text(YamiboIcons.Back, color = colors.brownDeep, fontSize = 18.sp) }
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f).focusRequester(focusRequester),
            placeholder = { Text(stringResource(Res.string.read_history_search_placeholder), color = colors.textDark.copy(alpha = 0.4f), fontSize = 15.sp) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = colors.textDark,
                unfocusedTextColor = colors.textDark,
                cursorColor = colors.brownDeep,
                focusedBorderColor = colors.brownDeep,
                unfocusedBorderColor = colors.brownPrimary.copy(alpha = 0.3f),
                focusedContainerColor = colors.brownPrimary.copy(alpha = 0.05f),
                unfocusedContainerColor = colors.brownPrimary.copy(alpha = 0.03f),
            ),
            shape = RoundedCornerShape(12.dp),
            textStyle = LocalTextStyle.current.copy(fontSize = 15.sp),
        )
        Spacer(Modifier.width(6.dp))
        Surface(onClick = onSearch, shape = RoundedCornerShape(12.dp), color = colors.brownDeep) {
            Text(
                text = stringResource(Res.string.read_history_search),
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.width(8.dp))
    }
}

@Composable
internal fun SelectTopBar(
    onSelectAll: () -> Unit,
    onClearAll: () -> Unit,
    onCancel: () -> Unit,
    onDeleteSelected: () -> Unit,
    selectedCount: Int,
) {
    val colors = YamiboTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = stringResource(Res.string.selected_items, selectedCount),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.brownDeep,
            modifier = Modifier.weight(1f),
        )
        Surface(onClick = onSelectAll, shape = RoundedCornerShape(10.dp), color = colors.brownPrimary.copy(alpha = 0.12f)) {
            Text(stringResource(Res.string.common_select_all), modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), fontSize = 13.sp, fontWeight = FontWeight.Medium, color = colors.brownDeep)
        }
        if (selectedCount > 0) {
            Surface(onClick = onDeleteSelected, shape = RoundedCornerShape(10.dp), color = Color(0xFFE53935).copy(alpha = 0.15f)) {
                Text(stringResource(Res.string.common_delete), modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFFE53935))
            }
        }
        Surface(onClick = onClearAll, shape = RoundedCornerShape(10.dp), color = Color(0xFFE53935).copy(alpha = 0.1f)) {
            Text(stringResource(Res.string.common_clear_selected), modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFFE53935))
        }
        Surface(onClick = onCancel, shape = RoundedCornerShape(10.dp), color = colors.brownPrimary.copy(alpha = 0.12f)) {
            Text(stringResource(Res.string.common_cancel), modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), fontSize = 13.sp, fontWeight = FontWeight.Medium, color = colors.brownDeep)
        }
    }
}

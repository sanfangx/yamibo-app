package me.thenano.yamibo.yamibo_app.components.controls

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme

/**
 * Reusable low-emphasis filter/sort chip.
 *
 * Use for option triggers such as Favorite sort/layout, Forum filter/order,
 * UserSpace blog filters, reader settings chips, and other compact controls
 * that open a dialog or immediately select a mode. For primary actions, use
 * [YamiboPrimaryButton] instead.
 *
 * @param text Label rendered inside the chip.
 * @param onClick Called when the chip is tapped.
 * @param selected Whether the chip should use the selected/brown-filled style.
 */
@Composable
fun YamiboActionChip(
    text: String,
    onClick: () -> Unit,
    selected: Boolean = false,
) {
    val colors = YamiboTheme.colors
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (selected) colors.brownDeep else colors.brownPrimary.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, colors.brownPrimary.copy(alpha = 0.12f)),
    ) {
        Text(
            text,
            color = if (selected) Color.White else colors.brownDeep,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
        )
    }
}

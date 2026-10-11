package pro.progr.authvk

import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource

@Composable
fun VkLoginButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String = ""
) {
    OutlinedButton(onClick = onClick, modifier = modifier) {
        Text(text.ifEmpty { stringResource(R.string.vk_login) })
    }
}

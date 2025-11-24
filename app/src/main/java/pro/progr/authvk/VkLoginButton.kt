package pro.progr.authvk

import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun VkLoginButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String = "Войти через VK ID"
) {
    OutlinedButton(onClick = onClick, modifier = modifier) {
        Text(text)
    }
}
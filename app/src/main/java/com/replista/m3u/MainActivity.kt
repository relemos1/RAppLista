package com.replista.m3u

import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.replista.m3u.ui.screens.PlayerScreen
import com.replista.m3u.ui.theme.RepListaM3uTheme
import com.replista.m3u.ui.viewmodel.PlayerViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: PlayerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Mantém a tela ligada durante a reprodução da TV
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Configura tela cheia imersiva de forma compatível
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            RepListaM3uTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    PlayerScreen(viewModel = viewModel)
                }
            }
        }
    }

    /**
     * Captura os eventos de teclas físicas do controle remoto do Android TV
     */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_UP -> {
                    viewModel.onNavigateChannelUp()
                    return true
                }
                KeyEvent.KEYCODE_DPAD_DOWN -> {
                    viewModel.onNavigateChannelDown()
                    return true
                }
                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    if (viewModel.isGuideVisible.value) {
                        viewModel.onNavigateProgramRight()
                        return true
                    }
                }
                KeyEvent.KEYCODE_DPAD_LEFT -> {
                    if (viewModel.isGuideVisible.value) {
                        viewModel.onNavigateProgramLeft()
                        return true
                    }
                }
                KeyEvent.KEYCODE_DPAD_CENTER,
                KeyEvent.KEYCODE_ENTER,
                KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                    viewModel.onConfirmOk()
                    return true
                }
                KeyEvent.KEYCODE_BACK -> {
                    if (viewModel.isGuideVisible.value) {
                        viewModel.closeGuide()
                        return true
                    }
                }
                KeyEvent.KEYCODE_MENU,
                KeyEvent.KEYCODE_GUIDE,
                KeyEvent.KEYCODE_INFO -> {
                    viewModel.toggleGuide()
                    return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemUI()
        }
    }

    private fun hideSystemUI() {
        try {
            val decorView = window.peekDecorView() ?: window.decorView
            val controller = WindowCompat.getInsetsController(window, decorView)
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } catch (_: Exception) {
            // Garante que nenhuma exceção de insets impeça a inicialização da tela
        }
    }
}

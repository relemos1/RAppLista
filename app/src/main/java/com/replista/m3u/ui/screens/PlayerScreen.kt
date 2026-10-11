package com.replista.m3u.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.replista.m3u.ui.components.FloatingMiniGuide
import com.replista.m3u.ui.components.VideoPlayerSurface
import com.replista.m3u.ui.theme.ProgressBlue
import com.replista.m3u.ui.theme.SurfaceDarkOverlay
import com.replista.m3u.ui.theme.TextPrimary
import com.replista.m3u.ui.theme.TextSecondary
import com.replista.m3u.ui.viewmodel.PlayerViewModel

@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    modifier: Modifier = Modifier
) {
    val playingChannel by viewModel.playingChannel.collectAsState()
    val selectedChannel by viewModel.selectedGuideChannel.collectAsState()
    val isGuideVisible by viewModel.isGuideVisible.collectAsState()

    val currentProgram by viewModel.currentProgram.collectAsState()
    val nextProgram by viewModel.nextProgram.collectAsState()

    val isLoadingChannels by viewModel.isLoadingChannels.collectAsState()
    val loadError by viewModel.loadError.collectAsState()

    val isBuffering by viewModel.playerController.isBuffering.collectAsState()
    val playbackError by viewModel.playerController.playbackError.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                // Toque na tela (Mobile ou controle com ponteiro) para alternar o mini-guia
                detectTapGestures(
                    onTap = {
                        viewModel.toggleGuide()
                    }
                )
            }
    ) {
        // 1. CAMADA DE VÍDEO (ExoPlayer Fullscreen)
        VideoPlayerSurface(
            playerController = viewModel.playerController,
            modifier = Modifier.fillMaxSize()
        )

        // 2. INDICADOR DE BUFFERING / CARREGAMENTO DE STREAM
        if (isBuffering) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(24.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x88000000))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = ProgressBlue,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Conectando...",
                        color = TextPrimary,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // 3. TELA DE CARREGAMENTO INICIAL DOS CANAIS DA LISTA M3U
        if (isLoadingChannels && playingChannel == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        color = ProgressBlue,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Carregando canais da lista...",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Aguarde enquanto sincronizamos a programação",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // 4. MENSAGEM DE ERRO (CASO HAJA FALHA DE REDE OU STREAM)
        if (loadError != null || playbackError != null) {
            val errorMsg = loadError ?: playbackError ?: ""
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xDDF43F5E))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    text = errorMsg,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // 5. MINI GUIA FLUTUANTE INDEPENDENTE (CAMADA SUPERIOR)
        FloatingMiniGuide(
            isVisible = isGuideVisible,
            selectedChannel = selectedChannel,
            playingChannel = playingChannel,
            currentProgram = currentProgram,
            nextProgram = nextProgram,
            onPreviousChannel = { viewModel.onNavigateChannelUp() },
            onNextChannel = { viewModel.onNavigateChannelDown() },
            onNextProgramSchedule = { viewModel.onNavigateProgramRight() },
            onConfirmWatch = { viewModel.onConfirmOk() },
            onClose = { viewModel.closeGuide() },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

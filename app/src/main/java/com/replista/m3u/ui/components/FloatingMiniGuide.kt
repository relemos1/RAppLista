package com.replista.m3u.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.replista.m3u.R
import com.replista.m3u.data.model.Channel
import com.replista.m3u.data.model.Program
import com.replista.m3u.ui.theme.BadgeGreenBg
import com.replista.m3u.ui.theme.BadgeGreenText
import com.replista.m3u.ui.theme.BorderSubtle
import com.replista.m3u.ui.theme.CardBackground
import com.replista.m3u.ui.theme.SurfaceDarkOverlay
import com.replista.m3u.ui.theme.TextMuted
import com.replista.m3u.ui.theme.TextPrimary
import com.replista.m3u.ui.theme.TextSecondary

@Composable
fun FloatingMiniGuide(
    isVisible: Boolean,
    selectedChannel: Channel?,
    playingChannel: Channel?,
    currentProgram: Program?,
    nextProgram: Program?,
    onPreviousChannel: () -> Unit,
    onNextChannel: () -> Unit,
    onNextProgramSchedule: () -> Unit,
    onConfirmWatch: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible && selectedChannel != null,
        enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut(),
        modifier = modifier
    ) {
        if (selectedChannel == null) return@AnimatedVisibility

        val isSelectedPlaying = selectedChannel.id == playingChannel?.id

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 860.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceDarkOverlay)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
                    .padding(20.dp)
            ) {
                // 1. TOPO: ÍCONE, NOME DO CANAL E BADGE DE STATUS
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        // Caixa do ícone ou logo do canal
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(CardBackground)
                                .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!selectedChannel.logoUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = selectedChannel.logoUrl,
                                    contentDescription = selectedChannel.name,
                                    modifier = Modifier
                                        .size(34.dp)
                                        .padding(2.dp),
                                    contentScale = ContentScale.Fit
                                )
                            } else {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_tv),
                                    contentDescription = "TV",
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        // Nome e status do canal
                        Column {
                            Text(
                                text = "${selectedChannel.number} • ${selectedChannel.name}",
                                color = TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Canal selecionado • Ao vivo",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }

                    // Badge da direita: EM EXIBIÇÃO (quando é o canal sintonizado no player)
                    if (isSelectedPlaying) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(BadgeGreenBg)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "EM EXIBIÇÃO",
                                color = BadgeGreenText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    } else {
                        // Badge indicando que pode apertar OK para sintonizar
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0x333B82F6))
                                .clickable { onConfirmWatch() }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "OK ASSISTIR",
                                color = Color(0xFF60A5FA),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 2. PROGRAMA ATUAL EM EXIBIÇÃO
                val currentTitle = currentProgram?.title ?: selectedChannel.name
                val currentTime = currentProgram?.formattedTime ?: "Ao vivo"
                val progress = currentProgram?.progress ?: 0.45f

                Text(
                    text = currentTitle,
                    color = TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = currentTime,
                    color = TextSecondary,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Barra de progresso azul ao vivo
                ProgramProgressBar(progress = progress)

                Spacer(modifier = Modifier.height(16.dp))

                // 3. CARD "PRÓXIMO PROGRAMA"
                val nextTitle = nextProgram?.title ?: "Sequência da Programação"
                val nextTime = nextProgram?.formattedTime ?: "A seguir"

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardBackground)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                        .clickable { onNextProgramSchedule() }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0x22FFFFFF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_clock),
                            contentDescription = "Próximo",
                            tint = Color(0xFFD1D5DB),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "PRÓXIMO PROGRAMA",
                            color = TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.8.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = nextTitle,
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = nextTime,
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Avançar",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 4. RODAPÉ DE ATALHOS (Controle remoto TV e toques no Mobile)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.clickable { onNextChannel() },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "↑↓ Canais",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Row(
                        modifier = Modifier.clickable { onNextProgramSchedule() },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "→ Próximo",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Row(
                        modifier = Modifier.clickable { onConfirmWatch() },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "OK Assistir",
                            color = if (isSelectedPlaying) TextMuted else Color(0xFF60A5FA),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Row(
                        modifier = Modifier.clickable { onClose() },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Voltar Fechar",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

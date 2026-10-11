package com.replista.m3u.data.repository

import android.util.Log
import com.replista.m3u.data.Constants
import com.replista.m3u.data.model.Channel
import com.replista.m3u.data.model.Program
import com.replista.m3u.data.parser.EpgParser
import com.replista.m3u.data.parser.M3uParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class PlaylistRepository(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
) {

    private val _channels = MutableStateFlow<List<Channel>>(emptyList())
    val channels: StateFlow<List<Channel>> = _channels.asStateFlow()

    private val _isLoading = MutableStateFlow<Boolean>(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Cache do EPG indexado por tvgId
    private var epgData: Map<String, List<Program>> = emptyMap()

    suspend fun loadPlaylistAndEpg(m3uUrl: String = Constants.M3U_URL, epgUrl: String = Constants.EPG_URL) {
        withContext(Dispatchers.IO) {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                // 1. Download e parse da lista M3U
                val m3uRequest = Request.Builder().url(m3uUrl).build()
                client.newCall(m3uRequest).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw Exception("Falha ao carregar M3U: HTTP ${response.code}")
                    }
                    val body = response.body ?: throw Exception("Corpo da resposta M3U vazio")
                    val parsedChannels = M3uParser.parse(body.byteStream())
                    _channels.value = parsedChannels
                    Log.d("PlaylistRepo", "Canais carregados com sucesso: ${parsedChannels.size}")
                }

                // 2. Download e parse do EPG (assíncrono e não-bloqueante)
                try {
                    val epgRequest = Request.Builder().url(epgUrl).build()
                    client.newCall(epgRequest).execute().use { response ->
                        if (response.isSuccessful && response.body != null) {
                            epgData = EpgParser.parse(response.body!!.byteStream())
                            Log.d("PlaylistRepo", "EPG carregado para ${epgData.size} canais")
                        }
                    }
                } catch (epgErr: Exception) {
                    Log.w("PlaylistRepo", "Não foi possível carregar o EPG completo: ${epgErr.message}")
                }

            } catch (e: Exception) {
                Log.e("PlaylistRepo", "Erro ao carregar lista: ${e.message}", e)
                _errorMessage.value = e.message ?: "Erro desconhecido ao carregar canais"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Retorna a lista de programas para o canal informado ou cria um fallback elegante
     * para que a UI sempre tenha informações do programa atual e do próximo.
     */
    fun getProgramsForChannel(channel: Channel): List<Program> {
        val now = System.currentTimeMillis()
        val tvgId = channel.tvgId

        if (!tvgId.isNullOrBlank() && epgData.containsKey(tvgId)) {
            val list = epgData[tvgId] ?: emptyList()
            if (list.isNotEmpty()) {
                return list
            }
        }

        // Fallback dinâmico quando não houver EPG específico para o canal
        // Garante que o mini guia flutuante exiba horários e barra de progresso perfeitamente
        val hourMillis = 60 * 60 * 1000L
        val currentSlotStart = (now / hourMillis) * hourMillis
        val currentSlotEnd = currentSlotStart + 2 * hourMillis
        val nextSlotEnd = currentSlotEnd + 2 * hourMillis

        return listOf(
            Program(
                title = "${channel.name} - Ao Vivo",
                description = "Transmissão contínua em tempo real.",
                startTimeMs = currentSlotStart,
                endTimeMs = currentSlotEnd,
                channelTvgId = tvgId ?: channel.id
            ),
            Program(
                title = "Programação Regular",
                description = "Sequência da programação do canal.",
                startTimeMs = currentSlotEnd,
                endTimeMs = nextSlotEnd,
                channelTvgId = tvgId ?: channel.id
            )
        )
    }

    /**
     * Retorna o programa atual e o próximo programa para o canal.
     */
    fun getCurrentAndNextProgram(channel: Channel): Pair<Program, Program?> {
        val programs = getProgramsForChannel(channel)
        val now = System.currentTimeMillis()

        val currentIndex = programs.indexOfFirst { now in it.startTimeMs until it.endTimeMs }
        val current = if (currentIndex != -1) {
            programs[currentIndex]
        } else {
            programs.firstOrNull() ?: Program(
                title = channel.name,
                startTimeMs = now - 30 * 60 * 1000L,
                endTimeMs = now + 90 * 60 * 1000L,
                channelTvgId = channel.tvgId ?: channel.id
            )
        }

        val next = if (currentIndex != -1 && currentIndex + 1 < programs.size) {
            programs[currentIndex + 1]
        } else if (programs.size > 1) {
            programs[1]
        } else {
            null
        }

        return Pair(current, next)
    }
}

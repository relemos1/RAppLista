package com.replista.m3u.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.replista.m3u.data.Constants
import com.replista.m3u.data.model.Channel
import com.replista.m3u.data.model.Program
import com.replista.m3u.data.repository.PlaylistRepository
import com.replista.m3u.player.PlayerController
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PlayerViewModel(
    application: Application,
    private val repository: PlaylistRepository = PlaylistRepository(),
    val playerController: PlayerController = PlayerController(application)
) : AndroidViewModel(application) {

    val channels: StateFlow<List<Channel>> = repository.channels
    val isLoadingChannels: StateFlow<Boolean> = repository.isLoading
    val loadError: StateFlow<String?> = repository.errorMessage

    // ESTADO 1 (A Regra de Ouro): Canal que o ExoPlayer está reproduzindo no fundo
    private val _playingChannel = MutableStateFlow<Channel?>(null)
    val playingChannel: StateFlow<Channel?> = _playingChannel.asStateFlow()

    // ESTADO 2 (A Regra de Ouro): Canal focado no mini-guia flutuante independente
    private val _selectedGuideChannel = MutableStateFlow<Channel?>(null)
    val selectedGuideChannel: StateFlow<Channel?> = _selectedGuideChannel.asStateFlow()

    // Controle de visibilidade do painel flutuante
    private val _isGuideVisible = MutableStateFlow(false)
    val isGuideVisible: StateFlow<Boolean> = _isGuideVisible.asStateFlow()

    // Programação do canal selecionado no guia
    private val _currentProgram = MutableStateFlow<Program?>(null)
    val currentProgram: StateFlow<Program?> = _currentProgram.asStateFlow()

    private val _nextProgram = MutableStateFlow<Program?>(null)
    val nextProgram: StateFlow<Program?> = _nextProgram.asStateFlow()

    private val _allProgramsForSelected = MutableStateFlow<List<Program>>(emptyList())
    val allProgramsForSelected: StateFlow<List<Program>> = _allProgramsForSelected.asStateFlow()

    // Offset de navegação horizontal de programas (0 = atual, 1 = próximo, 2 = seguinte...)
    private val _programHorizontalOffset = MutableStateFlow(0)
    val programHorizontalOffset: StateFlow<Int> = _programHorizontalOffset.asStateFlow()

    // Job para fechar o guia automaticamente após inatividade
    private var autoDismissJob: Job? = null

    init {
        playerController.initialize()
        loadPlaylist()
    }

    fun loadPlaylist() {
        viewModelScope.launch {
            repository.loadPlaylistAndEpg()
            val list = repository.channels.value
            if (list.isNotEmpty() && _playingChannel.value == null) {
                // Inicia reprodução do primeiro canal da lista
                playChannel(list.first())
            }
        }
    }

    /**
     * Inicia a reprodução de um canal no ExoPlayer
     */
    fun playChannel(channel: Channel) {
        _playingChannel.value = channel
        playerController.playStream(channel.streamUrl)
        updateEpgForSelectedChannel(channel)
    }

    /**
     * Abre o painel do mini-guia flutuante.
     * Conforme recomendação de UX: o canal selecionado começa sendo o canal em reprodução.
     */
    fun openGuide() {
        val current = _playingChannel.value ?: channels.value.firstOrNull()
        if (current != null) {
            _selectedGuideChannel.value = current
            updateEpgForSelectedChannel(current)
        }
        _programHorizontalOffset.value = 0
        _isGuideVisible.value = true
        resetAutoDismissTimer()
    }

    /**
     * Alterna a visibilidade do mini-guia.
     */
    fun toggleGuide() {
        if (_isGuideVisible.value) {
            closeGuide()
        } else {
            openGuide()
        }
    }

    /**
     * Fecha o mini-guia flutuante sem alterar a transmissão atual.
     */
    fun closeGuide() {
        _isGuideVisible.value = false
        autoDismissJob?.cancel()
    }

    /**
     * Navegação com a tecla ↑ (Canal anterior no guia).
     * IMPORTANTE: Não interrompe nem altera o canal em reprodução (playingChannel).
     */
    fun onNavigateChannelUp() {
        if (!_isGuideVisible.value) {
            openGuide()
            return
        }
        val list = channels.value
        if (list.isEmpty()) return

        val currentSelected = _selectedGuideChannel.value
        val currentIndex = list.indexOfFirst { it.id == currentSelected?.id }
        val newIndex = if (currentIndex <= 0) list.size - 1 else currentIndex - 1

        val newSelected = list[newIndex]
        _selectedGuideChannel.value = newSelected
        _programHorizontalOffset.value = 0
        updateEpgForSelectedChannel(newSelected)
        resetAutoDismissTimer()
    }

    /**
     * Navegação com a tecla ↓ (Próximo canal no guia).
     * IMPORTANTE: Não interrompe nem altera o canal em reprodução (playingChannel).
     */
    fun onNavigateChannelDown() {
        if (!_isGuideVisible.value) {
            openGuide()
            return
        }
        val list = channels.value
        if (list.isEmpty()) return

        val currentSelected = _selectedGuideChannel.value
        val currentIndex = list.indexOfFirst { it.id == currentSelected?.id }
        val newIndex = if (currentIndex == -1 || currentIndex >= list.size - 1) 0 else currentIndex + 1

        val newSelected = list[newIndex]
        _selectedGuideChannel.value = newSelected
        _programHorizontalOffset.value = 0
        updateEpgForSelectedChannel(newSelected)
        resetAutoDismissTimer()
    }

    /**
     * Navegação com a tecla → (Avança na programação horizontal do canal selecionado).
     */
    fun onNavigateProgramRight() {
        if (!_isGuideVisible.value) return
        val total = _allProgramsForSelected.value.size
        if (_programHorizontalOffset.value < total - 1) {
            _programHorizontalOffset.value += 1
            resetAutoDismissTimer()
        }
    }

    /**
     * Navegação com a tecla ← (Retorna na programação horizontal do canal selecionado).
     */
    fun onNavigateProgramLeft() {
        if (!_isGuideVisible.value) return
        if (_programHorizontalOffset.value > 0) {
            _programHorizontalOffset.value -= 1
            resetAutoDismissTimer()
        }
    }

    /**
     * Tecla OK: Confirma a escolha e troca a transmissão para o canal selecionado!
     */
    fun onConfirmOk() {
        if (!_isGuideVisible.value) {
            openGuide()
            return
        }
        val target = _selectedGuideChannel.value ?: return
        if (target.id != _playingChannel.value?.id) {
            playChannel(target)
        }
        resetAutoDismissTimer()
    }

    /**
     * Seleciona diretamente um canal via toque (mobile).
     */
    fun onTouchSelectChannel(channel: Channel) {
        _selectedGuideChannel.value = channel
        _programHorizontalOffset.value = 0
        updateEpgForSelectedChannel(channel)
        resetAutoDismissTimer()
    }

    private fun updateEpgForSelectedChannel(channel: Channel) {
        val (current, next) = repository.getCurrentAndNextProgram(channel)
        _currentProgram.value = current
        _nextProgram.value = next
        _allProgramsForSelected.value = repository.getProgramsForChannel(channel)
    }

    private fun resetAutoDismissTimer() {
        autoDismissJob?.cancel()
        autoDismissJob = viewModelScope.launch {
            delay(Constants.GUIDE_AUTO_DISMISS_DELAY_MS)
            _isGuideVisible.value = false
        }
    }

    override fun onCleared() {
        super.onCleared()
        autoDismissJob?.cancel()
        playerController.release()
    }
}

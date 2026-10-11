package com.replista.m3u.data.parser

import com.replista.m3u.data.model.Channel
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

object M3uParser {

    private val TVG_ID_REGEX = """tvg-id="([^"]*)"""".toRegex()
    private val TVG_NAME_REGEX = """tvg-name="([^"]*)"""".toRegex()
    private val TVG_LOGO_REGEX = """tvg-logo="([^"]*)"""".toRegex()
    private val GROUP_TITLE_REGEX = """group-title="([^"]*)"""".toRegex()

    fun parse(inputStream: InputStream): List<Channel> {
        val channels = mutableListOf<Channel>()
        val reader = BufferedReader(InputStreamReader(inputStream))
        var currentTvgId: String? = null
        var currentName: String? = null
        var currentLogo: String? = null
        var currentGroup = "Geral"
        var channelIndex = 1

        var line = reader.readLine()
        while (line != null) {
            val trimmed = line.trim()
            if (trimmed.startsWith("#EXTINF:")) {
                currentTvgId = TVG_ID_REGEX.find(trimmed)?.groupValues?.get(1)?.takeIf { it.isNotBlank() }
                val tvgName = TVG_NAME_REGEX.find(trimmed)?.groupValues?.get(1)
                currentLogo = TVG_LOGO_REGEX.find(trimmed)?.groupValues?.get(1)?.takeIf { it.isNotBlank() }
                val group = GROUP_TITLE_REGEX.find(trimmed)?.groupValues?.get(1)
                if (!group.isNullOrBlank()) {
                    currentGroup = group
                }

                // Nome do canal após a última vírgula
                val commaIndex = trimmed.lastIndexOf(',')
                currentName = if (commaIndex != -1 && commaIndex < trimmed.length - 1) {
                    trimmed.substring(commaIndex + 1).trim()
                } else {
                    tvgName ?: "Canal $channelIndex"
                }
            } else if (!trimmed.startsWith("#") && trimmed.isNotBlank()) {
                // Linha de URL de stream
                val streamUrl = trimmed
                val name = currentName ?: "Canal $channelIndex"
                val channel = Channel(
                    id = "ch_$channelIndex",
                    number = channelIndex,
                    name = name,
                    streamUrl = streamUrl,
                    logoUrl = currentLogo,
                    group = currentGroup,
                    tvgId = currentTvgId
                )
                channels.add(channel)
                channelIndex++

                // Reset para o próximo canal
                currentTvgId = null
                currentName = null
                currentLogo = null
            }
            line = reader.readLine()
        }

        return channels
    }
}

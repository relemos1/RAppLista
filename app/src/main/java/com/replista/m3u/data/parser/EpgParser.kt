package com.replista.m3u.data.parser

import android.util.Log
import com.replista.m3u.data.model.Program
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

object EpgParser {

    private const val TAG = "EpgParser"

    private fun parseDate(dateStr: String): Long? {
        val trimmed = dateStr.trim()
        val formats = listOf(
            "yyyyMMddHHmmss Z",
            "yyyyMMddHHmmss",
            "yyyyMMddHHmm"
        )
        for (pattern in formats) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US)
                val date = sdf.parse(trimmed)
                if (date != null) return date.time
            } catch (_: Exception) { }
        }
        return null
    }

    /**
     * Faz o parse do arquivo XMLTV e retorna um Map agrupado por tvgId (channel ID do XML)
     * contendo a lista ordenada de programas.
     */
    fun parse(inputStream: InputStream): Map<String, List<Program>> {
        val result = mutableMapOf<String, MutableList<Program>>()

        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = false
            val parser = factory.newPullParser()
            parser.setInput(inputStream, "UTF-8")

            var eventType = parser.eventType
            var currentChannelId: String? = null
            var currentStart: Long? = null
            var currentStop: Long? = null
            var currentTitle: String? = null
            var currentDesc: String = ""

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (parser.name) {
                            "programme" -> {
                                val channel = parser.getAttributeValue(null, "channel")
                                val startStr = parser.getAttributeValue(null, "start")
                                val stopStr = parser.getAttributeValue(null, "stop")

                                currentChannelId = channel
                                currentStart = if (startStr != null) parseDate(startStr) else null
                                currentStop = if (stopStr != null) parseDate(stopStr) else null
                                currentTitle = null
                                currentDesc = ""
                            }
                            "title" -> {
                                if (currentChannelId != null) {
                                    currentTitle = parser.nextText()
                                }
                            }
                            "desc" -> {
                                if (currentChannelId != null) {
                                    currentDesc = parser.nextText()
                                }
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name == "programme") {
                            if (currentChannelId != null && currentStart != null && currentStop != null && !currentTitle.isNullOrBlank()) {
                                val program = Program(
                                    title = currentTitle,
                                    description = currentDesc,
                                    startTimeMs = currentStart,
                                    endTimeMs = currentStop,
                                    channelTvgId = currentChannelId
                                )
                                val list = result.getOrPut(currentChannelId) { mutableListOf() }
                                list.add(program)
                            }
                            currentChannelId = null
                            currentStart = null
                            currentStop = null
                            currentTitle = null
                            currentDesc = ""
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao processar EPG XMLTV: ${e.message}", e)
        }

        // Ordena programas de cada canal por horário de início
        return result.mapValues { entry ->
            entry.value.sortedBy { it.startTimeMs }
        }
    }
}

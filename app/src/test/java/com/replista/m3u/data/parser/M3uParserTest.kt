package com.replista.m3u.data.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.io.ByteArrayInputStream

class M3uParserTest {

    @Test
    fun testParseM3uPlus() {
        val sampleM3u = """
            #EXTM3U
            #EXTINF:-1 tvg-id="108" tvg-name="TNT" tvg-logo="http://example.com/tnt.png" group-title="Filmes",TNT HD
            http://simao.pro:80/stream/108.ts
            #EXTINF:-1 tvg-id="109" tvg-name="Megapix" tvg-logo="http://example.com/megapix.png" group-title="Filmes",Megapix HD
            http://simao.pro:80/stream/109.ts
            #EXTINF:-1 tvg-id="" tvg-name="SporTV" tvg-logo="" group-title="Esportes",SporTV
            http://simao.pro:80/stream/110.ts
        """.trimIndent()

        val inputStream = ByteArrayInputStream(sampleM3u.toByteArray())
        val channels = M3uParser.parse(inputStream)

        assertEquals(3, channels.size)

        // Canal 1: TNT HD
        val ch1 = channels[0]
        assertEquals(1, ch1.number)
        assertEquals("TNT HD", ch1.name)
        assertEquals("http://simao.pro:80/stream/108.ts", ch1.streamUrl)
        assertEquals("http://example.com/tnt.png", ch1.logoUrl)
        assertEquals("Filmes", ch1.group)
        assertEquals("108", ch1.tvgId)

        // Canal 2: Megapix HD
        val ch2 = channels[1]
        assertEquals(2, ch2.number)
        assertEquals("Megapix HD", ch2.name)
        assertEquals("http://simao.pro:80/stream/109.ts", ch2.streamUrl)
        assertEquals("109", ch2.tvgId)

        // Canal 3: SporTV
        val ch3 = channels[2]
        assertEquals(3, ch3.number)
        assertEquals("SporTV", ch3.name)
        assertEquals("Esportes", ch3.group)
    }
}

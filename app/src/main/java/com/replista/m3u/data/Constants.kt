package com.replista.m3u.data

object Constants {
    // URL fixa da lista M3U conforme solicitado pelo usuário
    const val M3U_URL = "http://simao.pro/get.php?username=renantrindade&password=vk2770&type=m3u_plus&output=ts"

    // URL fixa de EPG (XMLTV) correspondente ao servidor Xtream Codes
    const val EPG_URL = "http://simao.pro/xmltv.php?username=renantrindade&password=vk2770"

    // Tempo de auto-dismiss do mini-guia flutuante em milissegundos
    const val GUIDE_AUTO_DISMISS_DELAY_MS = 8000L
}

package com.replista.m3u.data.model

data class Channel(
    val id: String,
    val number: Int,
    val name: String,
    val streamUrl: String,
    val logoUrl: String? = null,
    val group: String = "Geral",
    val tvgId: String? = null
)

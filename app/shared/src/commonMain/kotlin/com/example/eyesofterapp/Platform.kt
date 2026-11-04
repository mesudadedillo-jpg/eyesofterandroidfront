package com.example.eyesofterapp

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
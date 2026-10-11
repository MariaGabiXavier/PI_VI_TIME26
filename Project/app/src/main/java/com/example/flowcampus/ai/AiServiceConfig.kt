package com.example.flowcampus.ai

/**
 * Onde está o servidor de IA (a pasta ai-service).
 *
 *  - Emulador do Android Studio: o PC é enxergado como 10.0.2.2
 *  - Celular de verdade (mesmo Wi-Fi do PC): troque pelo IP do PC,
 *    ex.: "http://192.168.0.15:8000"  (descubra com o comando ipconfig)
 *  - Servidor publicado na internet: use a URL https://...
 */
object AiServiceConfig {

    const val BASE_URL = "http://10.0.2.2:8000"

    const val CONNECT_TIMEOUT_MS = 5_000

    // o servidor treina 3 modelos a cada chamada (leva alguns segundos)
    const val READ_TIMEOUT_MS = 60_000
}

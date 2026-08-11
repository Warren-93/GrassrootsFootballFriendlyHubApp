package com.gffh.mobile.core.network

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.okhttp.OkHttp

actual fun httpClientEngineFactory(): HttpClientEngineFactory<*> = OkHttp

// 10.0.2.2 only resolves inside the Android emulator's virtual network: it's
// the emulator's alias for the host machine's localhost. A physical device
// needs the host's real LAN address instead, which is machine- and
// network-specific - see gffh-mobile/README.md for how to point this at
// your own setup. Hardcoded here for this session's on-device verification.
actual fun defaultApiBaseUrl(): String = "http://192.168.1.82:8090"

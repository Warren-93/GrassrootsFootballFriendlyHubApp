package com.gffh.mobile.core.network

import io.ktor.client.engine.HttpClientEngineFactory

/** Platform HTTP engine - OkHttp on Android, Darwin on iOS. */
expect fun httpClientEngineFactory(): HttpClientEngineFactory<*>

/**
 * The API's origin per platform. An Android emulator reaches the host
 * machine's localhost via the special address 10.0.2.2; an iOS simulator
 * shares the host network namespace and can use localhost directly. Neither
 * applies to a physical device, which needs the host's real LAN address -
 * see gffh-mobile/README.md.
 */
expect fun defaultApiBaseUrl(): String

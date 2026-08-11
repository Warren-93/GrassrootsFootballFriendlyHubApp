package com.gffh.mobile.core.network

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.darwin.Darwin

actual fun httpClientEngineFactory(): HttpClientEngineFactory<*> = Darwin

actual fun defaultApiBaseUrl(): String = "http://localhost:8080"

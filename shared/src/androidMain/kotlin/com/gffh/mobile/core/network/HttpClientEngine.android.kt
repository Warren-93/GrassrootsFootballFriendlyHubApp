package com.gffh.mobile.core.network

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.okhttp.OkHttp

actual fun httpClientEngineFactory(): HttpClientEngineFactory<*> = OkHttp

// 10.0.2.2 is the Android emulator's alias for the host machine's localhost.
// Real builds pass their own URL from BuildConfig (see androidApp/build.gradle.kts);
// this is only the fallback for previews and tests.
actual fun defaultApiBaseUrl(): String = "http://10.0.2.2:8090"

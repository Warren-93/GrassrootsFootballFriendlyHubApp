package com.gffh.mobile.core.network

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.darwin.Darwin
import platform.Foundation.NSBundle

actual fun httpClientEngineFactory(): HttpClientEngineFactory<*> = Darwin

// The URL comes from the app's Info.plist key GFFHApiBaseUrl, set from the
// GFFH_API_BASE_URL build setting in iosApp/project.yml (override it on the
// xcodebuild command line). A simulator shares the Mac's network, so localhost
// works when nothing is set.
actual fun defaultApiBaseUrl(): String =
    (NSBundle.mainBundle.objectForInfoDictionaryKey("GFFHApiBaseUrl") as? String)
        ?.trim()?.trimEnd('/')?.takeIf { it.isNotEmpty() && !it.startsWith("$") }
        ?: "http://localhost:8090"

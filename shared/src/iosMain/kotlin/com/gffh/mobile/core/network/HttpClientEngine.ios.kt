package com.gffh.mobile.core.network

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.darwin.Darwin

actual fun httpClientEngineFactory(): HttpClientEngineFactory<*> = Darwin

// This build runs in a cloud iOS Simulator (GitHub Actions -> Appetize.io),
// not on the same machine or network as the backend, so neither "localhost"
// nor a LAN IP is reachable here. Point this at a public tunnel to your local
// backend (e.g. `ngrok http 8090`) before triggering the iOS Simulator Build
// workflow, and keep the backend + tunnel running for the demo session.
actual fun defaultApiBaseUrl(): String = "https://YOUR-NGROK-URL.ngrok-free.app"

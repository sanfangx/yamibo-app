package me.thenano.yamibo.yamibo_app.factory

import io.ktor.client.HttpClient
import io.ktor.client.plugins.DefaultRequest

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect object HttpClientFactory {
    fun create(defaultHeaders: DefaultRequest.DefaultRequestBuilder.() -> Unit = {}): HttpClient
}
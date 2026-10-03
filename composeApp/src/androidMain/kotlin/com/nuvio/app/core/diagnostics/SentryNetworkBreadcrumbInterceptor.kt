package com.nuvio.app.core.diagnostics

import okhttp3.Interceptor
import okhttp3.Response

class SentryNetworkBreadcrumbInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        return chain.proceed(chain.request())
    }
}

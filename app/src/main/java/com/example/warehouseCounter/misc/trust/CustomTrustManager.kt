package com.example.warehouseCounter.misc.trust

import android.annotation.SuppressLint
import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager

@SuppressLint("CustomX509TrustManager")
class CustomTrustManager(
    private val defaultTrustManager: X509TrustManager,
    private val trustedDomains: MutableSet<String> = mutableSetOf()
) : X509TrustManager {

    fun addTrustedDomain(domain: String) {
        trustedDomains.add(domain)
    }

    override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {
        defaultTrustManager.checkClientTrusted(chain, authType)
    }

    override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
        try {
            defaultTrustManager.checkServerTrusted(chain, authType)
        } catch (e: Exception) {
            val serverCertDomain = chain[0].subjectX500Principal.name
            if (!trustedDomains.contains(serverCertDomain)) {
                throw e
            }
        }
    }

    override fun getAcceptedIssuers(): Array<X509Certificate> {
        return defaultTrustManager.acceptedIssuers
    }
}
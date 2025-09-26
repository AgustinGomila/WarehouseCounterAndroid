package com.example.warehouseCounter.misc.trust

import com.example.warehouseCounter.WarehouseCounterApp.Companion.context
import com.example.warehouseCounter.WarehouseCounterApp.Companion.settingsVm
import com.example.warehouseCounter.misc.trust.TrustFactory.Companion.addTrustedDomains
import com.example.warehouseCounter.misc.trust.TrustFactory.Companion.trustedDomains
import java.net.MalformedURLException
import java.net.URL
import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager

object CustomSSLContext {
    fun createCustomSSLContext(): SSLContext? {
        try {
            val urls: MutableList<String> = mutableListOf()
            val urlPanel = settingsVm.urlPanel
            val apiServer = settingsVm.apiHost

            if (urlPanel.isNotEmpty()) urls.add(URL(urlPanel).host)
            if (apiServer.isNotEmpty()) urls.add(URL(apiServer).host)

            if (urls.any())
                addTrustedDomains(urls)
        } catch (e: MalformedURLException) {
            e.printStackTrace()
        }

        return try {
            val (socketFactory, defaultTrustManager) = TrustFactory.getTrustFactoryManager(context)
            val customTrustManager = CustomTrustManager(defaultTrustManager)

            for (domain in trustedDomains) {
                customTrustManager.addTrustedDomain(domain)
            }

            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, arrayOf<TrustManager>(customTrustManager), null)
            sslContext
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
            null
        } catch (e: KeyManagementException) {
            e.printStackTrace()
            null
        }
    }
}
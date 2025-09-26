package com.example.warehouseCounter.misc.trust

import android.content.Context
import com.example.warehouseCounter.R
import java.security.KeyStore
import java.security.cert.Certificate
import java.security.cert.CertificateFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

class TrustFactory {
    companion object {

        private const val EXAMPLE_CONFIG = "config.dacosys.com"
        private const val EXAMPLE_CLIENT = "client.dacosys.com"

        private var trustedList: MutableList<String> = mutableListOf(
            EXAMPLE_CONFIG,
            EXAMPLE_CLIENT,
        )

        val trustedDomains: List<String>
            get() = trustedList

        fun addTrustedDomains(domains: List<String>) {
            domains
                .filterNot { trustedList.contains(it) }
                .forEach { trustedList.add(it) }
        }

        fun getTrustFactoryManager(context: Context): Pair<SSLSocketFactory, X509TrustManager> {
            val cf = CertificateFactory.getInstance("X.509")

            // Cargar los certificados desde los recursos raw
            val isrgRoot1Input = context.resources.openRawResource(R.raw.isrgrootx1)
            val isrgRoot1Certificate: Certificate = isrgRoot1Input.use {
                cf.generateCertificate(it)
            }

            val isrgRoot2Input = context.resources.openRawResource(R.raw.isrgrootx2)
            val isrgRoot2Certificate: Certificate = isrgRoot2Input.use {
                cf.generateCertificate(it)
            }

            val keyStoreType = KeyStore.getDefaultType()
            val keyStore = KeyStore.getInstance(keyStoreType).apply {
                load(null, null)
                setCertificateEntry("isrgrootx1", isrgRoot1Certificate)
                setCertificateEntry("isrgrootx2", isrgRoot2Certificate)
            }

            val tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm()
            val tmf = TrustManagerFactory.getInstance(tmfAlgorithm).apply {
                init(keyStore)
            }

            val sslContext = SSLContext.getInstance("TLS").apply {
                init(null, tmf.trustManagers, null)
            }

            return Pair(sslContext.socketFactory, tmf.trustManagers[0] as X509TrustManager)
        }
    }
}
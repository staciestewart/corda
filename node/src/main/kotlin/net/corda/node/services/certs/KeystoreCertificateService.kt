package net.corda.node.services.certs

import net.corda.node.services.config.NodeConfiguration
import java.security.PrivateKey
import java.security.cert.X509Certificate

class KeystoreCertificateService(nodeConf: NodeConfiguration) : CertificateService {

    private val keyStore = nodeConf.signingCertificateStore.get(createNew = true).value

    override fun storeCertificates(alias: String, certificatesList: List<X509Certificate>, privateKey: PrivateKey) {
        return keyStore.setPrivateKey(alias, privateKey, certificatesList)
    }

    override fun getCertificates(alias: String): List<X509Certificate> {
        return keyStore.getCertificateChain(alias)
    }

    override fun containsAlias(alias: String): Boolean {
        return keyStore.contains(alias)
    }

    override fun deleteCertificates(alias: String) {
        keyStore.internal.deleteEntry(alias)
    }

    override fun getPrivateKey(alias: String): PrivateKey? {
        return keyStore.getPrivateKey(alias)
    }
}

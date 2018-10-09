package net.corda.node.services.certs

import java.security.PrivateKey
import java.security.cert.X509Certificate

/** Interface to store the certificate-chain for each key. */
interface CertificateService {
    fun storeCertificates(alias: String, certificatesList: List<X509Certificate>, privateKey: PrivateKey)
    fun getCertificates(alias: String): List<X509Certificate>
    fun containsAlias(alias: String): Boolean
    fun deleteCertificates(alias: String)
    fun getPrivateKey(alias: String): PrivateKey?
}

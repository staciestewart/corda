package net.corda.node.services.keys

import net.corda.core.crypto.Crypto
import net.corda.cryptoservice.CryptoService
import net.corda.node.services.config.NodeConfiguration
import net.corda.nodeapi.internal.crypto.X509Utilities
import java.security.PublicKey

class BCCryptoService(private val nodeConf: NodeConfiguration) : CryptoService {

    private val keystore = nodeConf.signingCertificateStore.get().value // TODO check if keystore exists.

    override fun generateKeyPair(alias: String, schemeNumberID: String): PublicKey {
        val keyPair = Crypto.generateKeyPair(Crypto.findSignatureScheme(schemeNumberID))
        // Store a self-signed certificate, as Keystore requires to store certificates instead of public keys.
        val cert = X509Utilities.createSelfSignedCACertificate(nodeConf.myLegalName.x500Principal, keyPair)
        keystore.setPrivateKey(alias, keyPair.private, listOf(cert))
        return keyPair.public
    }

    override fun containsKey(alias: String): Boolean {
        return keystore.contains(alias)
    }

    override fun getPublicKey(alias: String): PublicKey {
        return keystore.getPublicKey(alias)
    }

    override fun sign(alias: String, data: ByteArray): ByteArray {
        return Crypto.doSign(keystore.getPrivateKey(alias), data)
    }
}

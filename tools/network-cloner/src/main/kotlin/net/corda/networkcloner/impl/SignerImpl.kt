package net.corda.networkcloner.impl

import net.corda.core.crypto.SignableData
import net.corda.core.crypto.SignatureMetadata
import net.corda.core.crypto.TransactionSignature
import net.corda.core.transactions.CoreTransaction
import net.corda.networkcloner.api.IdentityRepository
import net.corda.networkcloner.api.Signer
import java.security.PublicKey
import javax.naming.OperationNotSupportedException

class SignerImpl(val identityRepository : IdentityRepository) : Signer {

    override fun sign(transaction: CoreTransaction, originalSigners: List<PublicKey>): List<TransactionSignature> {
        val signableData = SignableData(transaction.id, SignatureMetadata(9, 4))

        /*
        return originalSigners.map {
            val destinationIdentity = identityMapper.mapPublicKeyToDestinationIdentity(it)
            destinationIdentity.identityKey.sign(signableData)
        }
         */
        throw OperationNotSupportedException()
    }

}
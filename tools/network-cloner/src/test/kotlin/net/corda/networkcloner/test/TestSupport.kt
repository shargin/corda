package net.corda.networkcloner.test

import net.corda.core.identity.CordaX500Name
import net.corda.core.internal.toPath
import net.corda.networkcloner.api.CordappsRepository
import net.corda.networkcloner.api.NodeDatabase
import net.corda.networkcloner.api.PartyRepository
import net.corda.networkcloner.api.Serializer
import net.corda.networkcloner.impl.CordappsRepositoryImpl
import net.corda.networkcloner.impl.NodeDatabaseImpl
import net.corda.networkcloner.impl.NodesDirPartyRepository
import net.corda.networkcloner.impl.SerializerImpl
import java.io.File
import java.util.*

open class TestSupport {

    val clientX500Name = CordaX500Name.parse("O=Client,L=London,C=GB")
    val operatorX500Name = CordaX500Name.parse("O=Operator,L=New York,C=US")

    //@todo this storing to a static property doesn't really work if different tests ask for different snapshot
    fun getSerializer(snapshot: String) : Serializer {
        return if (serializer == null) {
            val cordappLoader = getCordappsRepository(snapshot).getCordappLoader()
            SerializerImpl(cordappLoader).also {
                serializer = it
            }
        } else {
            serializer!!
        }
    }

    private fun getSnapshotsDirectory() : File {
        return TestSupport::class.java.getResource("/snapshots").toPath().toFile()
    }

    fun getPartyRepository(snapshot : String, sourceOrDestination: String) : PartyRepository {
        val nodesDir = File(getSnapshotDirectory(snapshot), sourceOrDestination)
        return NodesDirPartyRepository(nodesDir)
    }

    fun getNodeDatabase(snapshot: String, sourceOrDestination: String, node: String) : NodeDatabase {
        val pathToDbFileWithoutSuffix = TxEditorTests::class.java.getResource("/snapshots/$snapshot/$sourceOrDestination/$node/persistence.mv.db").path.removeSuffix(".mv.db")
        return NodeDatabaseImpl("jdbc:h2:$pathToDbFileWithoutSuffix","sa","")
    }

    fun getCordappsRepository(snapshot: String) : CordappsRepository {
        return if (cordappsRepository == null) {
            val pathToCordapps = File(getSnapshotDirectory(snapshot),"tx-editor-plugins")
            CordappsRepositoryImpl(pathToCordapps).also {
                cordappsRepository = it
            }
        } else {
            cordappsRepository!!
        }
    }

    fun getSnapshotDirectory(snapshot: String) : File {
        return File(getSnapshotsDirectory(), snapshot)
    }

    fun copyAndGetSnapshotDirectory(snapshot: String) : File {
        val snapshotsDirectory = getSnapshotsDirectory()
        val snapshotDirectory = getSnapshotDirectory(snapshot)
        val copyDirectory = File(snapshotsDirectory, UUID.randomUUID().toString())
        snapshotDirectory.copyRecursively(copyDirectory)
        return copyDirectory
    }

    companion object {
        var serializer : Serializer? = null
        var cordappsRepository : CordappsRepository? = null
    }

}
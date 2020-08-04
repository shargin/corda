package freightertests

import freighter.deployments.DeploymentContext
import freighter.deployments.SingleNodeDeployed
import freighter.machine.AzureMachineProvider
import freighter.machine.DeploymentMachineProvider
import freighter.machine.DockerMachineProvider
import freighter.testing.AzureTest
import freighter.testing.DockerTest
import net.corda.bn.flows.SuspendMembershipFlow
import net.corda.bn.states.MembershipState
import net.corda.bn.states.MembershipStatus
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.messaging.startFlow
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.spi.ExtendedLogger
import org.junit.jupiter.api.Test
import utility.getOrThrow
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.measureTimeMillis

@AzureTest
class ImpactOfOverlappingGroupsTest : AbstractImpactOfOverlappingGroupsTest() {

    private companion object {
        const val numberOfParticipants = 20
        const val cutOffTime: Long = 300000
    }

    override fun getLogger(): ExtendedLogger {
        return LogManager.getContext().getLogger(ImpactOfOverlappingGroupsTest::class.java.name)
    }

    override val machineProvider: DeploymentMachineProvider = AzureMachineProvider()

    @Test
    fun testScenario() {
        runBenchmark(numberOfParticipants, cutOffTime)
    }
}

@DockerTest
class DockerImpactOfOverlappingGroupsTest : AbstractImpactOfOverlappingGroupsTest() {

    private companion object {
        const val numberOfParticipants = 5
        const val cutOffTime: Long = 300000
    }

    override fun getLogger(): ExtendedLogger {
        return LogManager.getContext().getLogger(DockerImpactOfOverlappingGroupsTest::class.java.name)
    }

    override val machineProvider: DeploymentMachineProvider = DockerMachineProvider()

    @Test
    fun testScenario() {
        runBenchmark(numberOfParticipants, cutOffTime)
    }
}

abstract class AbstractImpactOfOverlappingGroupsTest : BaseBNFreighterTest() {

    override fun runScenario(numberOfParticipants: Int, deploymentContext: DeploymentContext): Map<String, Long> {
        val nodeGenerator = createDeploymentGenerator(deploymentContext)
        val bnoNode = nodeGenerator().getOrThrow()

        val networkID = UniqueIdentifier()
        val defaultGroupID = UniqueIdentifier()
        val defaultGroupName = "InitialGroup"

        getLogger().info("Setting up Business Network")

        val bnoMembershipState: MembershipState = createBusinessNetwork(bnoNode, networkID, defaultGroupID, defaultGroupName)
        val listOfGroupMembers = buildGroupMembershipNodes(numberOfParticipants, nodeGenerator)
        val nodeToMembershipIds: Map<SingleNodeDeployed, MembershipState> = requestNetworkMembership(listOfGroupMembers, bnoNode, bnoMembershipState)
        activateNetworkMembership(nodeToMembershipIds, bnoNode)

        val subsetGroupMembersKeys = nodeToMembershipIds.values.chunked(nodeToMembershipIds.size / 5)

        val groupIndex = AtomicInteger(0)
        subsetGroupMembersKeys.map { listOfMemberStates ->
            createSubGroup(listOfMemberStates, groupIndex, bnoMembershipState, bnoNode)
        }

        val overlappingGroup = subsetGroupMembersKeys.map { it.first() } as MutableList
        createSubGroup(overlappingGroup, groupIndex, bnoMembershipState, bnoNode)

        val nodeToBeSuspended: MembershipState = overlappingGroup.last()
        val singleDeployedNodeToBeSuspended = nodeToMembershipIds.filterValues { it == nodeToBeSuspended }.keys
        val membershipSuspendTimeFlow = measureTimeMillis {
            bnoNode.rpc {
                startFlow(::SuspendMembershipFlow, nodeToBeSuspended.linearId, null)
            }
        }

        val suspendedStatusCriteria = getMembershipStatusQueryCriteria(listOf(MembershipStatus.SUSPENDED))
        val membershipSuspendTimeInVault = measureTimeMillis { waitForStatusUpdate(singleDeployedNodeToBeSuspended.toList(), suspendedStatusCriteria) }

        return mapOf("Time taken to run suspend flow on node for overlapping group" to membershipSuspendTimeFlow, "Time take for this to register in vault" to membershipSuspendTimeInVault)
    }
}
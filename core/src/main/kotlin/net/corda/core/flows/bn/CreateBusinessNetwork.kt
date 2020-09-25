package net.corda.core.flows.bn

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import net.corda.core.serialization.serialize

@StartableByRPC
class CreateBusinessNetwork : FlowLogic<Membership>() {

    @Suspendable
    override fun call(): Membership = BNMembership(
            identity = MembershipIdentity(ourIdentity).serialize().bytes,
            networkId = UniqueIdentifier().toString(),
            status = MembershipStatus.ACTIVE,
            roles = setOf(BNORole()).serialize().bytes
    ).let {
        serviceHub.withEntityManager {
            persist(it)
            flush()
        }

        Membership.from(it)
    }
}
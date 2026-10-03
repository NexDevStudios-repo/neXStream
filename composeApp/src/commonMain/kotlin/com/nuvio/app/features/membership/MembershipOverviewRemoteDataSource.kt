package com.nuvio.app.features.membership

internal object MembershipOverviewRemoteDataSource {
    suspend fun getMembershipOverview(): MembershipOverview {
        return MembershipOverview(
            status = "active",
            tier = MemberTier.SUPPORTER_PLUS,
            verifiedAt = "local",
            supporterSince = "local",
            providerConnected = true,
            hasSubscription = true,
            subscriptionActive = true,
            subscriptionStatus = "active",
            billingProvider = "local",
            membershipLevel = MemberTier.SUPPORTER_PLUS,
            hasActiveGrant = true,
            grantIsLifetime = true,
            hasLifetimeGrant = true,
            lifetimeGrantTier = MemberTier.SUPPORTER_PLUS,
        )
    }
}

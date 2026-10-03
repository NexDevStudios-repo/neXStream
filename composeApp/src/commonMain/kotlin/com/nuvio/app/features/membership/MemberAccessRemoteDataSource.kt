package com.nuvio.app.features.membership

internal object MemberAccessRemoteDataSource {
    suspend fun getMemberAccess(): MemberAccess {
        return MemberAccess(
            tier = MemberTier.SUPPORTER_PLUS,
            entitlements = CosmeticEntitlements(CosmeticEntitlement.entries.toSet()),
        )
    }
}

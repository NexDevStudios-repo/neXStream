package com.nuvio.app.features.collection

object CollectionSyncService {
    var isSyncingFromRemote: Boolean = false

    fun startObserving() {}

    suspend fun pullFromServer(profileId: Int) {}

    fun triggerPush() {}
}

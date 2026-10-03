package com.nuvio.app.features.watching.sync

import com.nuvio.app.features.watched.WatchedItem

object SupabaseWatchedSyncAdapter : WatchedSyncAdapter {
    override suspend fun getDeltaCursor(profileId: Int): Long = 0L
    override suspend fun pullDelta(profileId: Int, sinceEventId: Long, limit: Int): List<WatchedDeltaEvent> = emptyList()
    override suspend fun pull(profileId: Int, pageSize: Int): List<WatchedItem> = emptyList()
    override suspend fun push(profileId: Int, items: Collection<WatchedItem>) {}
    override suspend fun delete(profileId: Int, items: Collection<WatchedItem>) {}
}

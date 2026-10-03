package com.nuvio.app.features.watching.sync

import com.nuvio.app.features.watchprogress.WatchProgressEntry

object SupabaseProgressSyncAdapter : ProgressSyncAdapter {
    override suspend fun getDeltaCursor(profileId: Int): Long = 0L
    override suspend fun pullDelta(profileId: Int, sinceEventId: Long, limit: Int): List<ProgressDeltaEvent> = emptyList()
    override suspend fun pull(profileId: Int, sinceLastWatched: Long?, limit: Int?): List<ProgressSyncRecord> = emptyList()
    override suspend fun push(profileId: Int, entries: Collection<WatchProgressEntry>) {}
    override suspend fun delete(profileId: Int, entries: Collection<WatchProgressEntry>) {}
}

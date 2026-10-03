package com.nuvio.app.features.library.sync

import com.nuvio.app.features.library.LibraryItem

object SupabaseLibrarySyncAdapter : LibrarySyncAdapter {
    override suspend fun pullSnapshot(profileId: Int, pageSize: Int): List<LibraryItem> = emptyList()
    override suspend fun getDeltaCursor(profileId: Int): Long = 0L
    override suspend fun pullDelta(profileId: Int, sinceEventId: Long, limit: Int): List<LibraryDeltaEvent> = emptyList()
    override suspend fun pushItems(profileId: Int, items: Collection<LibraryItem>) {}
    override suspend fun deleteItems(profileId: Int, keys: Collection<LibrarySyncKey>) {}
}

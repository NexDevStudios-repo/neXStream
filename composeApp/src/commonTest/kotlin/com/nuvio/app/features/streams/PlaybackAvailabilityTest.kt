package com.nuvio.app.features.streams

import com.nuvio.app.features.xtream.XtreamAccount
import com.nuvio.app.features.xtream.XtreamRepository
import com.nuvio.app.features.xtream.XtreamServer
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlaybackAvailabilityTest {
    @Test
    fun `playback is available when videoId starts with xtream prefix`() {
        assertTrue(isPlaybackAvailable(videoId = "xtream:live:123", type = "tv"))
        assertTrue(isPlaybackAvailable(videoId = "xtream:movie:456", type = "movie"))
        assertTrue(isPlaybackAvailable(videoId = "xtream:series:789:1:2", type = "series"))
    }

    @Test
    fun `playback availability depends on active server for non-xtream ids`() {
        XtreamRepository.clear()
        assertFalse(isPlaybackAvailable(videoId = "tmdb:123", type = "movie"))

        val dummyAccount = XtreamAccount("test", "test", "http://example.com")
        val dummyServer = XtreamServer(id = "1", name = "Test Server", account = dummyAccount)
        XtreamRepository.setActiveServer(dummyServer)

        assertTrue(isPlaybackAvailable(videoId = "tmdb:123", type = "movie"))
        XtreamRepository.clear()
    }
}


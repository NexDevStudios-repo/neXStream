package com.nuvio.app.features.xtream

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class XtreamCharacterizationTest {

    @Test
    fun testXtreamServerStreamUrlBuilding() {
        val server = XtreamServer(
            id = "srv_1",
            name = "Test IPTV",
            serverUrl = "http://iptv.example.com:8080/",
            username = "user123",
            password = "pass456"
        )

        val liveUrl = server.getLiveStreamUrl(streamId = 101, containerExtension = "ts")
        val vodUrl = server.getVodStreamUrl(streamId = 202, containerExtension = "mp4")
        val seriesUrl = server.getSeriesStreamUrl(streamId = 303, containerExtension = "mkv")

        assertEquals("http://iptv.example.com:8080/live/user123/pass456/101.ts", liveUrl)
        assertEquals("http://iptv.example.com:8080/movie/user123/pass456/202.mp4", vodUrl)
        assertEquals("http://iptv.example.com:8080/series/user123/pass456/303.mkv", seriesUrl)
    }

    @Test
    fun testXtreamVodMappingToPosterItem() {
        val vodStream = XtreamVodStream(
            num = 1,
            name = "Inception (2010)",
            streamId = 550,
            streamType = "movie",
            streamIcon = "http://example.com/poster.jpg",
            rating = "8.8",
            rating5based = 4.4,
            added = "1600000000",
            categoryId = "10",
            containerExtension = "mp4",
            customSid = null,
            directSource = null
        )

        val poster = vodStream.toPosterItem()
        assertEquals("xtream:vod:550", poster.id)
        assertEquals("Inception (2010)", poster.title)
        assertEquals("http://example.com/poster.jpg", poster.poster)
        assertEquals("8.8", poster.rating)
    }

    @Test
    fun testXtreamLiveMappingToPosterItem() {
        val liveStream = XtreamLiveStream(
            num = 1,
            name = "HBO HD",
            streamId = 1001,
            streamType = "live",
            streamIcon = "http://example.com/hbo.png",
            epgChannelId = "hbo.us",
            added = "1600000000",
            categoryId = "5",
            customSid = null,
            tvArchive = 0,
            directSource = null,
            tvArchiveDuration = 0
        )

        val poster = liveStream.toPosterItem()
        assertEquals("xtream:live:1001", poster.id)
        assertEquals("HBO HD", poster.title)
        assertEquals("http://example.com/hbo.png", poster.poster)
    }

    @Test
    fun testXtreamSeriesMappingToPosterItem() {
        val series = XtreamSeries(
            num = 1,
            name = "Breaking Bad",
            seriesId = 2001,
            cover = "http://example.com/bb.jpg",
            plot = "A chemistry teacher...",
            cast = "Bryan Cranston",
            director = "Vince Gilligan",
            genre = "Drama",
            releaseDate = "2008-01-20",
            lastModified = "1600000000",
            rating = "9.5",
            rating5based = 4.75,
            backdropPath = emptyList(),
            youtubeTrailer = null,
            episodeRunTime = "45",
            categoryId = "15"
        )

        val poster = series.toPosterItem()
        assertEquals("xtream:series:2001", poster.id)
        assertEquals("Breaking Bad", poster.title)
        assertEquals("http://example.com/bb.jpg", poster.poster)
        assertEquals("9.5", poster.rating)
    }
}

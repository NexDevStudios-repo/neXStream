package com.nuvio.app.features.xtream

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class XtreamServerAccount(
    val id: String,
    val name: String,
    val serverUrl: String,
    val username: String,
    val password: String,
    val isActive: Boolean = true,
    val addedAt: Long = 0L,
)

@Serializable
data class XtreamAuthResponse(
    @SerialName("user_info") val userInfo: XtreamUserInfo? = null,
    @SerialName("server_info") val serverInfo: XtreamServerInfo? = null,
)

@Serializable
data class XtreamUserInfo(
    val username: String? = null,
    val status: String? = null,
    val exp_date: String? = null,
    val is_trial: String? = null,
    val active_cons: String? = null,
    val max_connections: String? = null,
    val allowed_output_formats: List<String> = emptyList(),
)

@Serializable
data class XtreamServerInfo(
    val url: String? = null,
    val port: String? = null,
    val https_port: String? = null,
    val server_protocol: String? = null,
    val rtmp_port: String? = null,
    val timezone: String? = null,
    val timestamp_now: Long? = null,
    val time_now: String? = null,
)

@Serializable
data class XtreamCategory(
    @SerialName("category_id") val categoryId: String,
    @SerialName("category_name") val categoryName: String,
    @SerialName("parent_id") val parentId: Int = 0,
)

@Serializable
data class XtreamLiveStream(
    val num: Int? = null,
    val name: String,
    @SerialName("stream_type") val streamType: String? = null,
    @SerialName("stream_id") val streamId: Int,
    @SerialName("stream_icon") val streamIcon: String? = null,
    @SerialName("epg_channel_id") val epgChannelId: String? = null,
    val added: String? = null,
    @SerialName("category_id") val categoryId: String? = null,
    @SerialName("custom_sid") val customSid: String? = null,
    @SerialName("tv_archive") val tvArchive: Int? = null,
    @SerialName("direct_source") val directSource: String? = null,
    @SerialName("tv_archive_duration") val tvArchiveDuration: Int? = null,
)

@Serializable
data class XtreamVodStream(
    val num: Int? = null,
    val name: String,
    @SerialName("stream_type") val streamType: String? = null,
    @SerialName("stream_id") val streamId: Int,
    @SerialName("stream_icon") val streamIcon: String? = null,
    val rating: String? = null,
    @SerialName("rating_5based") val rating5Based: Double? = null,
    val added: String? = null,
    @SerialName("category_id") val categoryId: String? = null,
    @SerialName("container_extension") val containerExtension: String? = null,
    @SerialName("custom_sid") val customSid: String? = null,
    @SerialName("direct_source") val directSource: String? = null,
)

@Serializable
data class XtreamSeriesItem(
    val num: Int? = null,
    val name: String,
    @SerialName("series_id") val seriesId: Int,
    val cover: String? = null,
    val plot: String? = null,
    val cast: String? = null,
    val director: String? = null,
    val genre: String? = null,
    @SerialName("releaseDate") val releaseDate: String? = null,
    @SerialName("last_modified") val lastModified: String? = null,
    val rating: String? = null,
    @SerialName("rating_5based") val rating5Based: Double? = null,
    @SerialName("backdrop_path") val backdropPath: List<String> = emptyList(),
    @SerialName("youtube_trailer") val youtubeTrailer: String? = null,
    @SerialName("episode_run_time") val episodeRunTime: String? = null,
    @SerialName("category_id") val categoryId: String? = null,
)

@Serializable
data class XtreamSeriesInfo(
    val info: XtreamSeriesDetailInfo? = null,
    val episodes: Map<String, List<XtreamEpisode>> = emptyMap(),
)

@Serializable
data class XtreamSeriesDetailInfo(
    val name: String? = null,
    val cover: String? = null,
    val plot: String? = null,
    val cast: String? = null,
    val director: String? = null,
    val genre: String? = null,
    val releaseDate: String? = null,
    val rating: String? = null,
    @SerialName("backdrop_path") val backdropPath: List<String> = emptyList(),
    @SerialName("youtube_trailer") val youtubeTrailer: String? = null,
    @SerialName("episode_run_time") val episodeRunTime: String? = null,
    @SerialName("category_id") val categoryId: String? = null,
)

@Serializable
data class XtreamEpisode(
    val id: String,
    @SerialName("episode_num") val episodeNum: Int? = null,
    val title: String? = null,
    @SerialName("container_extension") val containerExtension: String? = "mp4",
    val info: XtreamEpisodeInfo? = null,
    val season: Int? = null,
)

@Serializable
data class XtreamEpisodeInfo(
    val plot: String? = null,
    val duration: String? = null,
    @SerialName("duration_secs") val durationSecs: Int? = null,
    @SerialName("releasedate") val releaseDate: String? = null,
    @SerialName("movie_image") val movieImage: String? = null,
    val rating: Double? = null,
)

@Serializable
data class XtreamVodInfo(
    val info: XtreamVodDetailInfo? = null,
    @SerialName("movie_data") val movieData: XtreamVodMovieData? = null,
)

@Serializable
data class XtreamVodDetailInfo(
    val name: String? = null,
    val tmdb_id: String? = null,
    val plot: String? = null,
    val cast: String? = null,
    val director: String? = null,
    val genre: String? = null,
    val release_date: String? = null,
    val rating: String? = null,
    @SerialName("movie_image") val movieImage: String? = null,
    @SerialName("backdrop_path") val backdropPath: List<String> = emptyList(),
    @SerialName("youtube_trailer") val youtubeTrailer: String? = null,
    @SerialName("duration_secs") val durationSecs: Int? = null,
    val duration: String? = null,
)

@Serializable
data class XtreamVodMovieData(
    @SerialName("stream_id") val streamId: Int? = null,
    val name: String? = null,
    @SerialName("container_extension") val containerExtension: String? = "mp4",
)

@Serializable
data class XtreamShortEpg(
    @SerialName("epg_listings") val epgListings: List<XtreamEpgProgram> = emptyList(),
)

@Serializable
data class XtreamEpgProgram(
    val id: String? = null,
    @SerialName("epg_id") val epgId: String? = null,
    val title: String? = null,
    val lang: String? = null,
    val start: String? = null,
    val end: String? = null,
    val description: String? = null,
    @SerialName("start_timestamp") val startTimestamp: Long? = null,
    @SerialName("stop_timestamp") val stopTimestamp: Long? = null,
)

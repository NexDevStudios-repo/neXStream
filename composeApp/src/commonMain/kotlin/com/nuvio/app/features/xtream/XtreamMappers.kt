package com.nuvio.app.features.xtream

import com.nuvio.app.features.catalog.CatalogTarget
import com.nuvio.app.features.details.MetaDetails
import com.nuvio.app.features.details.MetaPerson
import com.nuvio.app.features.details.MetaVideo
import com.nuvio.app.features.home.HomeCatalogSection
import com.nuvio.app.features.home.MetaPreview
import com.nuvio.app.features.home.PosterShape
import com.nuvio.app.features.streams.StreamItem

fun XtreamLiveStream.toMetaPreview(): MetaPreview =
    MetaPreview(
        id = "xtream:live:$streamId",
        type = "tv",
        name = name,
        poster = streamIcon,
        landscapePoster = streamIcon,
        posterShape = PosterShape.Landscape,
        description = "Canal en vivo",
    )

fun XtreamVodStream.toMetaPreview(): MetaPreview =
    MetaPreview(
        id = "xtream:vod:$streamId",
        type = "movie",
        name = name,
        poster = streamIcon,
        imdbRating = rating?.takeIf { it.isNotBlank() },
        posterShape = PosterShape.Poster,
    )

fun XtreamSeriesItem.toMetaPreview(): MetaPreview =
    MetaPreview(
        id = "xtream:series:$seriesId",
        type = "series",
        name = name,
        poster = cover,
        description = plot,
        releaseInfo = releaseDate,
        imdbRating = rating?.takeIf { it.isNotBlank() },
        genres = genre?.split(",")?.map(String::trim)?.filter(String::isNotBlank) ?: emptyList(),
        posterShape = PosterShape.Poster,
    )

fun XtreamCategory.toHomeCatalogSection(items: List<MetaPreview>, sectionType: String): HomeCatalogSection {
    val typeName = when (sectionType) {
        "live" -> "tv"
        "vod" -> "movie"
        else -> "series"
    }
    val subtitleText = when (sectionType) {
        "live" -> "En vivo"
        "vod" -> "Películas"
        else -> "Series"
    }
    return HomeCatalogSection(
        key = "xtream:$sectionType:$categoryId",
        title = categoryName,
        subtitle = subtitleText,
        addonName = "Xtream Codes",
        target = CatalogTarget.Addon(
            manifestUrl = "xtream",
            contentType = typeName,
            catalogId = categoryId,
        ),
        items = items,
        availableItemCount = items.size,
        hasMore = false,
    )
}

fun XtreamVodInfo.toMetaDetails(streamId: Int, fallbackName: String = "Película"): MetaDetails {
    val title = info?.name ?: movieData?.name ?: fallbackName
    val posterImg = info?.movieImage
    val backdropImg = info?.backdropPath?.firstOrNull()
    return MetaDetails(
        id = "xtream:vod:$streamId",
        type = "movie",
        name = title,
        poster = posterImg,
        background = backdropImg ?: posterImg,
        description = info?.plot,
        genres = info?.genre?.split(",")?.map(String::trim)?.filter(String::isNotBlank) ?: emptyList(),
        director = info?.director?.split(",")?.map(String::trim)?.filter(String::isNotBlank) ?: emptyList(),
        cast = info?.cast?.split(",")?.map(String::trim)?.filter(String::isNotBlank)?.map { MetaPerson(name = it) } ?: emptyList(),
        releaseInfo = info?.release_date,
        runtime = info?.duration ?: info?.durationSecs?.let { "${it / 60} min" },
        imdbRating = info?.rating,
        videos = listOf(
            MetaVideo(
                id = "xtream:vod:$streamId",
                title = title,
                overview = info?.plot,
                thumbnail = posterImg,
                available = true,
            ),
        ),
    )
}

fun XtreamSeriesInfo.toMetaDetails(seriesId: Int, fallbackName: String = "Serie"): MetaDetails {
    val title = info?.name ?: fallbackName
    val posterImg = info?.cover
    val backdropImg = info?.backdropPath?.firstOrNull()

    val videoList = episodes.flatMap { (seasonKey, eps) ->
        val seasonNum = seasonKey.toIntOrNull() ?: 1
        eps.map { ep ->
            val epNum = ep.episodeNum ?: 1
            val ext = ep.containerExtension ?: "mp4"
            MetaVideo(
                id = "xtream:series_ep:${ep.id}:$ext",
                title = ep.title?.takeIf { it.isNotBlank() } ?: "Episodio $epNum",
                season = ep.season ?: seasonNum,
                episode = epNum,
                overview = ep.info?.plot,
                thumbnail = ep.info?.movieImage ?: posterImg,
                released = ep.info?.releaseDate,
                runtime = ep.info?.durationSecs?.let { it / 60 },
                available = true,
            )
        }
    }.sortedWith(compareBy({ it.season ?: 1 }, { it.episode ?: 1 }))

    return MetaDetails(
        id = "xtream:series:$seriesId",
        type = "series",
        name = title,
        poster = posterImg,
        background = backdropImg ?: posterImg,
        description = info?.plot,
        genres = info?.genre?.split(",")?.map(String::trim)?.filter(String::isNotBlank) ?: emptyList(),
        director = info?.director?.split(",")?.map(String::trim)?.filter(String::isNotBlank) ?: emptyList(),
        cast = info?.cast?.split(",")?.map(String::trim)?.filter(String::isNotBlank)?.map { MetaPerson(name = it) } ?: emptyList(),
        releaseInfo = info?.releaseDate,
        imdbRating = info?.rating,
        videos = videoList,
    )
}

fun XtreamLiveStream.toMetaDetails(): MetaDetails =
    MetaDetails(
        id = "xtream:live:$streamId",
        type = "tv",
        name = name,
        poster = streamIcon,
        background = streamIcon,
        description = "Canal de TV en directo",
        videos = listOf(
            MetaVideo(
                id = "xtream:live:$streamId",
                title = name,
                thumbnail = streamIcon,
                available = true,
            ),
        ),
    )

package com.nuvio.app.features.xtream

data class ParsedM3uChannel(
    val id: Int,
    val name: String,
    val streamUrl: String,
    val logoUrl: String? = null,
    val groupTitle: String? = null,
    val epgId: String? = null,
    val type: String = "live",
)

data class ParsedM3uPlaylist(
    val name: String,
    val channels: List<ParsedM3uChannel>,
    val categories: List<XtreamCategory>,
)

object M3uParser {

    /**
     * Attempts to extract Xtream Codes credentials if the M3U URL is an Xtream get.php URL.
     * Example: http://iptv.server:8080/get.php?username=user&password=pass&type=m3u_plus
     */
    fun tryExtractXtreamCredentials(url: String): Triple<String, String, String>? {
        val trimmed = url.trim()
        if (!trimmed.contains("get.php", ignoreCase = true) && !trimmed.contains("player_api.php", ignoreCase = true)) {
            return null
        }
        val uriParts = trimmed.split("?", limit = 2)
        if (uriParts.size < 2) return null
        val baseUrl = uriParts[0].substringBeforeLast("/").trimEnd('/')
        val queryParams = uriParts[1].split("&").associate { param ->
            val parts = param.split("=", limit = 2)
            if (parts.size == 2) parts[0].lowercase() to parts[1] else parts[0].lowercase() to ""
        }
        val user = queryParams["username"] ?: queryParams["user"]
        val pass = queryParams["password"] ?: queryParams["pass"]
        if (!user.isNullOrBlank() && !pass.isNullOrBlank()) {
            return Triple(baseUrl, user, pass)
        }
        return null
    }

    /**
     * Parses raw M3U / M3U8 text into channels and categories.
     */
    fun parse(content: String, defaultCategory: String = "General"): ParsedM3uPlaylist {
        val lines = content.lines()
        val channels = mutableListOf<ParsedM3uChannel>()
        val categoriesSet = linkedSetOf<String>()

        var currentExtInf: String? = null
        var currentGroup: String? = null
        var channelIndex = 1

        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue

            if (line.startsWith("#EXTINF:", ignoreCase = true)) {
                currentExtInf = line
                currentGroup = extractAttribute(line, "group-title")
            } else if (line.startsWith("#EXTGRP:", ignoreCase = true)) {
                currentGroup = line.substringAfter(":").trim().takeIf { it.isNotEmpty() }
            } else if (!line.startsWith("#") && currentExtInf != null) {
                val streamUrl = line
                val extInf = currentExtInf

                val name = if (extInf.contains(",")) {
                    extInf.substringAfter(",").trim().ifBlank { "Canal $channelIndex" }
                } else {
                    "Canal $channelIndex"
                }

                val logo = extractAttribute(extInf, "tvg-logo")
                val group = currentGroup?.ifBlank { defaultCategory }
                    ?: extractAttribute(extInf, "group-title")?.ifBlank { defaultCategory }
                    ?: defaultCategory

                val epgId = extractAttribute(extInf, "tvg-id") ?: extractAttribute(extInf, "tvg-name")

                val isMovie = streamUrl.contains("/movie/", ignoreCase = true) ||
                        streamUrl.endsWith(".mp4", ignoreCase = true) ||
                        streamUrl.endsWith(".mkv", ignoreCase = true) ||
                        streamUrl.endsWith(".avi", ignoreCase = true)

                val isSeries = streamUrl.contains("/series/", ignoreCase = true)

                val type = when {
                    isMovie -> "movie"
                    isSeries -> "series"
                    else -> "live"
                }

                categoriesSet.add(group)
                channels.add(
                    ParsedM3uChannel(
                        id = channelIndex,
                        name = name,
                        streamUrl = streamUrl,
                        logoUrl = logo,
                        groupTitle = group,
                        epgId = epgId,
                        type = type,
                    )
                )
                channelIndex++
                currentExtInf = null
                currentGroup = null
            }
        }

        val categories = categoriesSet.mapIndexed { idx, groupName ->
            XtreamCategory(
                categoryId = (idx + 1).toString(),
                categoryName = groupName,
            )
        }

        return ParsedM3uPlaylist(
            name = "M3U Playlist",
            channels = channels,
            categories = categories,
        )
    }

    private fun extractAttribute(extInf: String, attributeName: String): String? {
        val regex = Regex("""$attributeName="([^"]*)"""", RegexOption.IGNORE_CASE)
        val match = regex.find(extInf)
        if (match != null) {
            return match.groupValues[1].trim().takeIf { it.isNotEmpty() }
        }
        val singleQuoteRegex = Regex("""$attributeName='([^']*)'""", RegexOption.IGNORE_CASE)
        val singleMatch = singleQuoteRegex.find(extInf)
        if (singleMatch != null) {
            return singleMatch.groupValues[1].trim().takeIf { it.isNotEmpty() }
        }
        val simpleRegex = Regex("""$attributeName=([^ ,]*)""", RegexOption.IGNORE_CASE)
        val simpleMatch = simpleRegex.find(extInf)
        return simpleMatch?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }
    }
}

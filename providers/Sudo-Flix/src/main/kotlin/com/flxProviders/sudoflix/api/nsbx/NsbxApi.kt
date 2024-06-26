package com.flxProviders.sudoflix.api.nsbx

import android.net.Uri
import com.flixclusive.core.util.coroutines.asyncCalls
import com.flixclusive.core.util.coroutines.mapAsync
import com.flixclusive.core.util.film.FilmType
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.core.util.network.fromJson
import com.flixclusive.core.util.network.request
import com.flixclusive.model.provider.SourceLink
import com.flixclusive.model.provider.Subtitle
import com.flixclusive.model.provider.SubtitleSource
import com.flixclusive.model.tmdb.FilmDetails
import com.flixclusive.model.tmdb.common.tv.Episode
import com.flixclusive.provider.ProviderApi
import com.flxProviders.sudoflix.api.nsbx.dto.NsbxProviders
import com.flxProviders.sudoflix.api.nsbx.dto.NsbxSource
import okhttp3.Headers.Companion.toHeaders
import okhttp3.OkHttpClient

internal class NsbxApi(
    client: OkHttpClient
) : ProviderApi(client) {
    override val baseUrl: String
        get() = "https://api.nsbx.ru"
    override val name: String
        get() = "NSBX"

    private val origin = "https://extension.works.again.with.nsbx"
    private val headers = mapOf(
        "Origin" to origin,
        "Referer" to origin,
    ).toHeaders()

    override suspend fun getSourceLinks(
        watchId: String,
        film: FilmDetails,
        episode: Episode?,
        onLinkLoaded: (SourceLink) -> Unit,
        onSubtitleLoaded: (Subtitle) -> Unit
    ) {
        val availableProviders = getAvailableProviders()

        if (availableProviders.isEmpty())
            throw IllegalStateException("No available providers for NSBX")

        for (i in availableProviders.indices) {
            try {
                val provider = availableProviders[i]
                val query = film.getQuery(
                    season = episode?.season,
                    episode = episode?.number
                )

                val searchRawResponse = client.request(
                    url = "$baseUrl/search?provider=$provider&query=${Uri.encode(query)}",
                    headers = headers,
                ).execute().body?.string()
                    ?: throw IllegalStateException("[$name]> Could not search for film")

                val searchResponse = fromJson<Map<String, String>>(searchRawResponse)
                val encryptedSourceId = searchResponse["url"]
                    ?: throw IllegalStateException("[$name]> Could not get encrypted source id")

                val source = client.request(
                    url = "$baseUrl/provider?resourceId=$encryptedSourceId&provider=$provider",
                    headers = headers,
                ).execute()
                    .use {
                        val stringResponse = it.body?.string()

                        if (!it.isSuccessful || stringResponse == null || stringResponse.contains("\"error\"")) {
                            errorLog(stringResponse ?: "Unknown NSBX Error")
                            throw IllegalStateException("[$name]> Could not get source link")
                        }

                        fromJson<NsbxSource>(stringResponse)
                    }

                asyncCalls(
                    {
                        source.stream.mapAsync {
                            it.qualities.entries.mapAsync { (serverName, qualitySource) ->
                                onLinkLoaded(
                                    SourceLink(
                                        name = serverName,
                                        url = qualitySource.url
                                    )
                                )
                            }
                        }
                    },
                    {
                        source.stream.mapAsync {
                            it.captions.mapAsync { caption ->
                                onSubtitleLoaded(
                                    Subtitle(
                                        url = caption.url,
                                        language = caption.language,
                                        type = SubtitleSource.ONLINE
                                    )
                                )
                            }
                        }
                    },
                )
                return
            } catch (e: Exception) {
                if (i != availableProviders.indices.last) {
                    errorLog(e.stackTraceToString())
                    continue
                }

                throw e
            }
        }

        throw IllegalStateException("[$name]> Could not get source link")
    }

    private fun getAvailableProviders(): List<String> {
        val response = client.request(
            url = "$baseUrl/status",
            headers = headers
        ).execute().body?.string()
            ?: throw NullPointerException("[$name]> Could not get available providers")

        if (response.contains("\"error\""))
            throw IllegalStateException("[$name]> Internal server error")

        return fromJson<NsbxProviders>(response).providers
    }

    private fun FilmDetails.getQuery(
        season: Int?,
        episode: Int?
    ): String {
        val filmType = if (season != null) "show" else FilmType.MOVIE.type

        return """
            {"title":"$title","releaseYear":${year},"tmdbId":"$tmdbId","imdbId":"$imdbId","type":"$filmType","season":"${season ?: ""}","episode":"${episode ?: ""}"}
        """.trimIndent()
    }
}
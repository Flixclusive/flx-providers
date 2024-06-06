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
import com.flixclusive.model.tmdb.Film
import com.flixclusive.provider.ProviderApi
import com.flixclusive.provider.dto.FilmInfo
import com.flixclusive.provider.dto.SearchResults
import com.flxProviders.sudoflix.api.nsbx.dto.NsbxProviders
import com.flxProviders.sudoflix.api.nsbx.dto.NsbxSource
import com.flxProviders.sudoflix.api.nsbx.dto.TmdbQueryDto
import com.flxProviders.sudoflix.api.nsbx.util.getTmdbQuery
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

    private val defaultMethodError = IllegalAccessException("This method is not necessary to be called for NsbxApi")

    override suspend fun getFilmInfo(
        filmId: String,
        filmType: FilmType
    ): FilmInfo = throw defaultMethodError

    override suspend fun getSourceLinks(
        filmId: String,
        film: Film,
        season: Int?,
        episode: Int?,
        onLinkLoaded: (SourceLink) -> Unit,
        onSubtitleLoaded: (Subtitle) -> Unit
    ) {
        val availableProviders = getAvailableProviders()

        if (availableProviders.isEmpty())
            throw IllegalStateException("No available providers for NSBX")

        for (i in availableProviders.indices) {
            try {
                val provider = availableProviders[i]
                val query = getFilmQuery(
                    filmId = filmId,
                    season = season,
                    episode = episode,
                )

                val searchRawResponse = client.request(
                    url = "$baseUrl/search?provider=$provider&query=${Uri.encode(query)}",
                    headers = headers,
                ).execute().body?.string()
                    ?: throw IllegalStateException("Could not search for film [NsbxApi]")

                val searchResponse = fromJson<Map<String, String>>(searchRawResponse)
                val encryptedSourceId = searchResponse["url"]
                    ?: throw IllegalStateException("Could not get encrypted source id [NsbxApi]")

                val source = client.request(
                    url = "$baseUrl/provider?resourceId=$encryptedSourceId&provider=$provider",
                    headers = headers,
                ).execute()
                    .use {
                        val stringResponse = it.body?.string()

                        if (!it.isSuccessful || stringResponse == null || stringResponse.contains("\"error\"")) {
                            errorLog(stringResponse ?: "Unknown NSBX Error")
                            throw IllegalStateException("Could not get source link [NsbxApi]")
                        }

                        fromJson<NsbxSource>(stringResponse)
                    }

                asyncCalls(
                    {
                        source.stream.mapAsync {
                            if (it.qualities.entries == null) {
                                throw IllegalStateException("Could not get mp4s [NsbxApi]")
                            }

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

        throw IllegalStateException("Could not get source link [NsbxApi]")
    }

    override suspend fun search(
        film: Film,
        page: Int,
    ): SearchResults = throw defaultMethodError

    private fun getAvailableProviders(): List<String> {
        val response = client.request(
            url = "$baseUrl/status",
            headers = headers
        ).execute().body?.string()
            ?: throw NullPointerException("Could not get available providers [NsbxApi]")

        return fromJson<NsbxProviders>(response).providers
    }

    private fun getFilmQuery(
        filmId: String,
        season: Int?,
        episode: Int?,
    ): String {
        val tmdbFilmType = if (season != null) FilmType.TV_SHOW.type else FilmType.MOVIE.type
        val filmType = if (season != null) "show" else FilmType.MOVIE.type

        val tmdbQuery = getTmdbQuery(
            id = filmId,
            filmType = tmdbFilmType
        )

        val response = client.request(tmdbQuery)
            .execute().body?.string()
            ?: throw NullPointerException("Could not get TMDB ID [NsbxApi]")

        val tmdbResponse = fromJson<TmdbQueryDto>(response)

        return """
            {"title":"${tmdbResponse.title}","releaseYear":${tmdbResponse.releaseYear},"tmdbId":"${tmdbResponse.tmdbId}","imdbId":"${tmdbResponse.imdbId ?: tmdbResponse.externalIds["imdb_id"]}","type":"$filmType","season":"${season ?: ""}","episode":"${episode ?: ""}"}
        """.trimIndent()
    }
}
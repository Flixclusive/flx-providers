package com.flxProviders.sudoflix.api.primewire.extractor

import com.flixclusive.core.util.exception.safeCall
import com.flixclusive.core.util.network.CryptographyUtil
import com.flixclusive.core.util.network.request
import com.flixclusive.model.provider.SourceLink
import com.flixclusive.model.provider.Subtitle
import com.flixclusive.provider.extractor.Extractor
import okhttp3.OkHttpClient

internal class Voe(
    client: OkHttpClient
) : Extractor(client) {
    override val baseUrl: String
        get() = "https://voe.sx"
    override val name: String
        get() = "Voe"

    private val linkRegex = Regex("""'hls': ?'(http.*?)',""")

    override suspend fun extract(
        url: String,
        onLinkLoaded: (SourceLink) -> Unit,
        onSubtitleLoaded: (Subtitle) -> Unit
    ) {
        val streamPage = client.request(url = url)
            .execute().body?.string()
            ?: throw Exception("[$name]> Failed to load page")

        val playerSrcMatch = linkRegex.find(streamPage)

        var streamUrl = playerSrcMatch?.groupValues?.get(1)
            ?: throw Exception("[$name]> Stream URL not found in embed code")

        safeCall {
            if (!streamUrl.startsWith("http"))
                streamUrl = CryptographyUtil.base64Decode(streamUrl)
        }

        onLinkLoaded(
            SourceLink(
                url = streamUrl,
                name = name
            )
        )
    }
}
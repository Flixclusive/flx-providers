package com.flxProviders.flixhq.extractors.vidcloud.dto

import com.flixclusive.core.util.network.fromJson
import com.flixclusive.model.provider.Subtitle
import com.flixclusive.model.provider.SubtitleSource
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import java.lang.reflect.Type

internal data class VidCloudEmbedData(
    val sources: List<DecryptedSource>,
    val tracks: List<VidCloudEmbedSubtitleData>,
    val encrypted: Boolean,
    val server: Int
) {
    data class VidCloudEmbedSubtitleData(
        @SerializedName("file") val url: String,
        @SerializedName("label") val lang: String,
        val kind: String
    )

    companion object {
        fun VidCloudEmbedSubtitleData.toSubtitle() = Subtitle(
            url = url,
            language = lang,
            type = SubtitleSource.ONLINE
        )
    }
}

internal class VidCloudEmbedDataCustomDeserializer(
    private val decryptSource: (String) -> List<DecryptedSource>
): JsonDeserializer<VidCloudEmbedData> {
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): VidCloudEmbedData {
        val obj = json.asJsonObject
        val tracks = obj.get("tracks").asJsonArray.map {
            fromJson<VidCloudEmbedData.VidCloudEmbedSubtitleData>(it)
        }
        val encrypted = obj.get("t").asInt == 1
        val server = obj.get("server").asInt

        val sources = if (encrypted) {
            decryptSource(obj.get("sources").asString)
        } else {
            obj.get("sources").asJsonArray.map {
                fromJson<DecryptedSource>(it)
            }
        }

        return VidCloudEmbedData(
            sources = sources,
            tracks = tracks,
            encrypted = encrypted,
            server = server,
        )
    }
}
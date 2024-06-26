package com.flxProviders.stremio.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.core.ui.mobile.component.ImageWithSmallPlaceholder
import com.flxProviders.stremio.api.model.Addon
import com.flxProviders.stremio.settings.util.LocalResources
import com.flxProviders.stremio.settings.util.getBitmapFromImage
import com.flxProviders.stremio.settings.util.getDrawable
import java.net.URL
import com.flixclusive.core.ui.common.R as UiCommonR
import com.flixclusive.core.util.R as UtilR

@Composable
internal fun AddonCard(
    addon: Addon,
    onUpdateManifest: () -> Unit,
    onEdit: () -> Unit,
    onRemove: () -> Unit,
) {
    val resources = LocalResources.current
    val clipboardManager = LocalClipboardManager.current

    OutlinedCard(
        onClick = {
            val stremioUrl = "${addon.baseUrl}/manifest.json"
                .replace("https://", "stremio://")
            clipboardManager.setText(
                AnnotatedString(text = stremioUrl)
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .height(60.dp)
                .fillMaxWidth()
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(weight = 1F, fill = true)
                    .padding(3.dp)
            ) {
                ImageWithSmallPlaceholder(
                    modifier = Modifier.size(60.dp),
                    placeholderModifier = Modifier.size(30.dp),
                    urlImage = addon.logo,
                    placeholderId = UiCommonR.drawable.provider_logo,
                    contentDescId = UtilR.string.provider_icon_content_desc,
                    shape = MaterialTheme.shapes.small
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val host = remember { URL(addon.baseUrl).host }

                    Text(
                        text = addon.name,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )

                    Text(
                        text = host ?: "",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal,
                            color = LocalContentColor.current.onMediumEmphasis()
                        )
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 3.dp)
            ) {
                IconButton(onClick = onUpdateManifest) {
                    val refreshIcon = remember {
                        resources.getDrawable("refresh")?.getBitmapFromImage()
                    }

                    if (refreshIcon != null) {
                        Icon(
                            bitmap = refreshIcon,
                            contentDescription = null
                        )
                    }
                }

                IconButton(onClick = onEdit) {
                    val modifyIcon = remember {
                        resources.getDrawable("modify")?.getBitmapFromImage()
                    }

                    if (modifyIcon != null) {
                        Icon(
                            bitmap = modifyIcon,
                            contentDescription = null
                        )
                    }
                }

                IconButton(onClick = onRemove) {
                    val deleteIcon = remember {
                        resources.getDrawable("delete")
                            ?.getBitmapFromImage()
                    }

                    if (deleteIcon != null) {
                        Icon(
                            bitmap = deleteIcon,
                            contentDescription = null
                        )
                    }
                }
            }
        }
    }
}
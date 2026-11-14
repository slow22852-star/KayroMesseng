package com.kirox.messenger

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private fun removeWhiteBackground(source: Bitmap): Bitmap {
    val bitmap = source.copy(Bitmap.Config.ARGB_8888, true)

    for (y in 0 until bitmap.height) {
        for (x in 0 until bitmap.width) {
            val pixel = bitmap.getPixel(x, y)

            val red = android.graphics.Color.red(pixel)
            val green = android.graphics.Color.green(pixel)
            val blue = android.graphics.Color.blue(pixel)

            val alpha = 255 - minOf(red, green, blue)

            bitmap.setPixel(
                x,
                y,
                android.graphics.Color.argb(
                    alpha,
                    red,
                    green,
                    blue
                )
            )
        }
    }

    return bitmap
}

@Composable
fun KayroSpriteIcon(
    @DrawableRes resource: Int,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    removeBackground: Boolean = true
) {
    val bitmap = remember(resource, removeBackground) {
        val original = BitmapFactory.decodeResource(
            androidx.compose.ui.platform.LocalContext.current.resources,
            resource
        )

        if (removeBackground) {
            removeWhiteBackground(original)
        } else {
            original
        }
    }

    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = null,
        modifier = modifier
            .size(size)
            .padding(1.dp),
        contentScale = ContentScale.Fit
    )
}

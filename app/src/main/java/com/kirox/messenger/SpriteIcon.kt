package com.kirox.messenger

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.util.ArrayDeque

private fun isBackgroundPixel(color: Int): Boolean {
    val r = Color.red(color)
    val g = Color.green(color)
    val b = Color.blue(color)

    return r >= 235 && g >= 235 && b >= 235
}

private fun removeWhiteBackground(source: Bitmap): Bitmap {
    val bitmap = source.copy(Bitmap.Config.ARGB_8888, true)

    val width = bitmap.width
    val height = bitmap.height

    val visited = BooleanArray(width * height)
    val queue = ArrayDeque<Int>()

    fun addPixel(x: Int, y: Int) {
        if (x < 0 || y < 0 || x >= width || y >= height) return

        val index = y * width + x

        if (visited[index]) return

        if (isBackgroundPixel(bitmap.getPixel(x, y))) {
            visited[index] = true
            queue.addLast(index)
        }
    }

    for (x in 0 until width) {
        addPixel(x, 0)
        addPixel(x, height - 1)
    }

    for (y in 0 until height) {
        addPixel(0, y)
        addPixel(width - 1, y)
    }

    while (queue.isNotEmpty()) {
        val index = queue.removeFirst()

        val x = index % width
        val y = index / width

        bitmap.setPixel(
            x,
            y,
            Color.TRANSPARENT
        )

        addPixel(x - 1, y)
        addPixel(x + 1, y)
        addPixel(x, y - 1)
        addPixel(x, y + 1)
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
    val context = LocalContext.current

    val bitmap = remember(resource, removeBackground, context) {
        val original = BitmapFactory.decodeResource(
            context.resources,
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
        modifier = modifier.size(size),
        contentScale = ContentScale.Fit
    )
}

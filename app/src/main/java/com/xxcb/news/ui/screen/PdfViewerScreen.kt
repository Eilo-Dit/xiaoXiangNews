package com.xxcb.news.ui.screen

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xxcb.news.data.cache.PdfCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val RedPrimary = Color(0xFFE70012)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(
    pdfUrl: String,
    title: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var pdfBitmaps by remember { mutableStateOf<List<Bitmap>?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Download and render PDF
    LaunchedEffect(pdfUrl) {
        isLoading = true
        errorMessage = null
        try {
            val bitmaps = withContext(Dispatchers.IO) {
                // Download PDF (with cache)
                val cacheFile = PdfCache.downloadAndCache(context, pdfUrl)
                // Render PDF pages to bitmaps
                val fd = ParcelFileDescriptor.open(cacheFile, ParcelFileDescriptor.MODE_READ_ONLY)
                val renderer = PdfRenderer(fd)
                val result = mutableListOf<Bitmap>()
                for (i in 0 until renderer.pageCount) {
                    val page = renderer.openPage(i)
                    // Render at 2x for high quality
                    val scale = 2
                    val bitmap = Bitmap.createBitmap(
                        page.width * scale,
                        page.height * scale,
                        Bitmap.Config.ARGB_8888
                    )
                    bitmap.eraseColor(android.graphics.Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()
                    result.add(bitmap)
                }
                renderer.close()
                fd.close()
                result
            }
            pdfBitmaps = bitmaps
            isLoading = false
        } catch (e: Exception) {
            errorMessage = "PDF加载失败: ${e.message}"
            isLoading = false
        }
    }

    // Clean up bitmaps when leaving
    DisposableEffect(Unit) {
        onDispose {
            pdfBitmaps?.forEach { it.recycle() }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top bar
        TopAppBar(
            title = {
                Text(
                    text = title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = Color.White
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = RedPrimary
            )
        )

        // Content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF525659))
        ) {
            when {
                isLoading -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = Color.White)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "正在加载高清版...",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }
                }
                errorMessage != null -> {
                    Text(
                        text = errorMessage ?: "加载失败",
                        color = Color.White,
                        fontSize = 16.sp,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                pdfBitmaps != null -> {
                    PdfPagesView(bitmaps = pdfBitmaps!!)
                }
            }
        }
    }
}

@Composable
fun PdfPagesView(bitmaps: List<Bitmap>) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    val boxSize = remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { boxSize.value = Offset(it.width.toFloat(), it.height.toFloat()) }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val pointerCount = event.changes.count { it.pressed }

                        if (pointerCount >= 2) {
                            val zoom = event.calculateZoom()
                            val pan = event.calculatePan()
                            val centroid = event.calculateCentroid(useCurrent = false)

                            val oldScale = scale
                            val newScale = (oldScale * zoom).coerceIn(1f, 5f)

                            if (centroid != Offset.Unspecified && newScale != oldScale) {
                                // Anchor zoom on centroid:
                                // The point under the centroid in content coords is:
                                //   contentPt = (centroid - offset) / oldScale
                                // After zoom, that point should still be at centroid:
                                //   centroid = contentPt * newScale + newOffset
                                //   newOffset = centroid - contentPt * newScale
                                val contentX = (centroid.x - offsetX) / oldScale
                                val contentY = (centroid.y - offsetY) / oldScale
                                offsetX = centroid.x - contentX * newScale + pan.x
                                offsetY = centroid.y - contentY * newScale + pan.y
                            } else {
                                offsetX += pan.x
                                offsetY += pan.y
                            }

                            scale = newScale

                            event.changes.forEach { change ->
                                if (change.positionChanged()) {
                                    change.consume()
                                }
                            }
                        } else if (pointerCount == 1 && scale > 1f) {
                            val pan = event.calculatePan()
                            offsetX += pan.x
                            offsetY += pan.y
                            event.changes.forEach { change ->
                                if (change.positionChanged()) {
                                    change.consume()
                                }
                            }
                        }
                    } while (event.changes.any { it.pressed })

                    // Reset to 1x if barely zoomed
                    if (scale < 1.1f) {
                        scale = 1f
                        offsetX = 0f
                        offsetY = 0f
                    }
                }
            }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offsetX,
                    translationY = offsetY,
                    transformOrigin = TransformOrigin(0f, 0f)
                ),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            itemsIndexed(bitmaps) { index, bitmap ->
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "第${index + 1}页",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    contentScale = ContentScale.FillWidth
                )
            }
        }
    }
}

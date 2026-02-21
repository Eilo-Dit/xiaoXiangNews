package com.xxcb.news.ui.screen

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xxcb.news.data.cache.PdfCache
import com.xxcb.news.data.model.NewspaperPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val RedPrimary = Color(0xFFE70012)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PdfViewerScreen(
    pages: List<NewspaperPage>,
    initialPageIndex: Int,
    onBack: () -> Unit
) {
    val pagerState = rememberPagerState(
        initialPage = initialPageIndex,
        pageCount = { pages.size }
    )

    val currentEdition = if (pages.isNotEmpty()) {
        pages[pagerState.currentPage].edition
    } else ""

    Column(modifier = Modifier.fillMaxSize()) {
        // Top bar
        TopAppBar(
            title = {
                Text(
                    text = currentEdition,
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

        // Pager content
        Box(
            modifier = Modifier
                .weight(1f)
                .background(Color(0xFF525659))
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                beyondBoundsPageCount = pages.size,
                key = { pages[it].pdfUrl }
            ) { pageIndex ->
                SinglePdfPage(
                    pdfUrl = pages[pageIndex].pdfUrl
                )
            }
        }

        // Bottom page indicator
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF333333))
                .padding(vertical = 10.dp)
        ) {
            Text(
                text = "第${pagerState.currentPage + 1}版 / 共${pages.size}版    ${currentEdition}",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun SinglePdfPage(pdfUrl: String) {
    val context = LocalContext.current

    // Initialize from memory cache synchronously (no loading flash)
    val cachedBitmaps = remember(pdfUrl) { PdfCache.getCachedBitmaps(pdfUrl) }
    var pdfBitmaps by remember(pdfUrl) { mutableStateOf(cachedBitmaps) }
    var isLoading by remember(pdfUrl) { mutableStateOf(cachedBitmaps == null) }
    var errorMessage by remember(pdfUrl) { mutableStateOf<String?>(null) }

    // Only download and render if not in memory cache
    LaunchedEffect(pdfUrl) {
        if (pdfBitmaps != null) return@LaunchedEffect

        isLoading = true
        errorMessage = null
        try {
            val bitmaps = withContext(Dispatchers.IO) {
                val cacheFile = PdfCache.downloadAndCache(context, pdfUrl)
                val fd = ParcelFileDescriptor.open(cacheFile, ParcelFileDescriptor.MODE_READ_ONLY)
                val renderer = PdfRenderer(fd)
                val result = mutableListOf<Bitmap>()
                // Use screen density for appropriate render scale, cap at 2x
                val density = context.resources.displayMetrics.density
                val renderScale = density.coerceIn(1.5f, 2f)
                for (i in 0 until renderer.pageCount) {
                    val page = renderer.openPage(i)
                    val bitmap = Bitmap.createBitmap(
                        (page.width * renderScale).toInt(),
                        (page.height * renderScale).toInt(),
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
            // Store in memory cache (cleared on date change)
            PdfCache.putBitmaps(pdfUrl, bitmaps)
            pdfBitmaps = bitmaps
            isLoading = false
        } catch (e: Exception) {
            errorMessage = "PDF加载失败: ${e.message}"
            isLoading = false
        }
    }

    // Don't recycle bitmaps on dispose — they're managed by PdfCache's HashMap

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoading -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
                    fontSize = 16.sp
                )
            }
            pdfBitmaps != null -> {
                ZoomablePdfView(bitmaps = pdfBitmaps!!)
            }
        }
    }
}

@Composable
fun ZoomablePdfView(bitmaps: List<Bitmap>) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
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

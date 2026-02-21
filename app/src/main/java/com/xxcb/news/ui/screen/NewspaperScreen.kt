package com.xxcb.news.ui.screen

import android.app.DatePickerDialog
import android.view.Gravity
import android.view.ViewGroup
import android.widget.DatePicker
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.xxcb.news.data.model.NewspaperPage
import com.xxcb.news.ui.viewmodel.NewspaperViewModel
import java.util.Calendar

private val RedPrimary = Color(0xFFE70012)
private val BluePrimary = Color(0xFF0354A3)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewspaperScreen(
    viewModel: NewspaperViewModel,
    onPageClick: (NewspaperPage) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        // Top bar
        TopAppBar(
            title = {
                Text(
                    text = "潇湘晨报",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            },
            actions = {
                // Date picker button
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.2f))
                        .clickable {
                            showDatePicker(context, uiState.currentDate, uiState.validDates) { date ->
                                viewModel.changeDate(date)
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (uiState.currentDate.isNotEmpty()) uiState.currentDate else "选择日期",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "▼",
                        color = Color.White,
                        fontSize = 10.sp
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
                .background(Color(0xFFF5F5F5))
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = RedPrimary
                    )
                }
                uiState.error != null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = uiState.error ?: "加载失败",
                            color = Color.Gray,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "点击重试",
                            color = BluePrimary,
                            fontSize = 14.sp,
                            modifier = Modifier.clickable {
                                viewModel.loadNewspaper(uiState.currentDate)
                            }
                        )
                    }
                }
                else -> {
                    NewspaperGrid(
                        pages = uiState.pages,
                        onPageClick = onPageClick
                    )
                }
            }
        }
    }
}

@Composable
fun NewspaperGrid(
    pages: List<NewspaperPage>,
    onPageClick: (NewspaperPage) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(pages) { page ->
            NewspaperPageCard(page = page, onClick = { onPageClick(page) })
        }
    }
}

@Composable
fun NewspaperPageCard(
    page: NewspaperPage,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column {
            // Newspaper page image
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(page.imgUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = page.edition,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp),
                contentScale = ContentScale.Fit
            )

            // Edition label
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BluePrimary)
                    .padding(vertical = 8.dp, horizontal = 12.dp)
            ) {
                Text(
                    text = page.edition,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // "View HD" hint
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                Text(
                    text = "点击查看高清版 →",
                    color = RedPrimary,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

private fun showDatePicker(
    context: android.content.Context,
    currentDate: String,
    validDates: Set<String>,
    onDateSelected: (String) -> Unit
) {
    val calendar = Calendar.getInstance()
    if (currentDate.isNotEmpty()) {
        try {
            val parts = currentDate.split("-")
            calendar.set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
        } catch (_: Exception) {}
    }

    val dialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val date = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
            onDateSelected(date)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    // Set max date to today
    dialog.datePicker.maxDate = System.currentTimeMillis()

    // Add a red hint TextView above the buttons
    val hintText = TextView(context).apply {
        text = "该日期无报纸"
        setTextColor(android.graphics.Color.RED)
        textSize = 14f
        gravity = Gravity.CENTER
        setPadding(0, 16, 0, 0)
        visibility = android.view.View.GONE
    }

    // Dynamically enable/disable OK button and show/hide hint
    if (validDates.isNotEmpty()) {
        dialog.datePicker.init(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ) { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
            val date = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
            val isValid = validDates.contains(date)
            dialog.getButton(DatePickerDialog.BUTTON_POSITIVE)?.isEnabled = isValid
            hintText.visibility = if (isValid) android.view.View.GONE else android.view.View.VISIBLE
        }

        // Check initial date validity and inject hint after dialog is shown
        dialog.setOnShowListener {
            // Find the DatePicker's parent and inject hintText below it
            val datePicker = dialog.datePicker
            val parent = datePicker.parent as? ViewGroup
            if (parent != null) {
                // Wrap: remove DatePicker from parent, put it in a LinearLayout with hint, re-add
                val index = parent.indexOfChild(datePicker)
                parent.removeView(datePicker)
                val wrapper = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = datePicker.layoutParams
                    addView(datePicker)
                    addView(hintText)
                }
                parent.addView(wrapper, index)
            }

            val initDate = String.format(
                "%04d-%02d-%02d",
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            val isValid = validDates.contains(initDate)
            dialog.getButton(DatePickerDialog.BUTTON_POSITIVE)?.isEnabled = isValid
            hintText.visibility = if (isValid) android.view.View.GONE else android.view.View.VISIBLE
        }
    }

    dialog.show()
}

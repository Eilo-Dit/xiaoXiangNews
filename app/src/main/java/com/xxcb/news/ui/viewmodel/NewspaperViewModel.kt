package com.xxcb.news.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xxcb.news.data.cache.PdfCache
import com.xxcb.news.data.model.NewspaperPage
import com.xxcb.news.data.repository.NewspaperRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class NewspaperUiState(
    val isLoading: Boolean = false,
    val pages: List<NewspaperPage> = emptyList(),
    val currentDate: String = "",
    val validDates: Set<String> = emptySet(),
    val error: String? = null
)

class NewspaperViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = NewspaperRepository()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private val _uiState = MutableStateFlow(NewspaperUiState())
    val uiState: StateFlow<NewspaperUiState> = _uiState.asStateFlow()

    init {
        val today = dateFormat.format(Calendar.getInstance().time)
        loadNewspaper(today)
        loadValidDates()
    }

    fun loadNewspaper(date: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val result = repository.getNewspaper(date)
            result.onSuccess { pages ->
                if (pages.isNotEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        pages = pages,
                        currentDate = date,
                        error = null
                    )
                    // Preload all PDF files to disk cache in background
                    preloadPdfs(pages)
                } else {
                    // 当天没有数据，尝试获取最近一期
                    val lastDateResult = repository.getLastDate(date)
                    lastDateResult.onSuccess { lastDate ->
                        if (lastDate != null && lastDate != date) {
                            loadNewspaper(lastDate)
                        } else {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = "暂无报纸数据"
                            )
                        }
                    }.onFailure {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "暂无报纸数据"
                        )
                    }
                }
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "加载失败"
                )
            }
        }
    }

    private fun loadValidDates() {
        viewModelScope.launch {
            val result = repository.getAllValidDates()
            result.onSuccess { dates ->
                _uiState.value = _uiState.value.copy(
                    validDates = dates.toSet()
                )
            }
        }
    }

    fun changeDate(date: String) {
        PdfCache.clearBitmapCache()
        loadNewspaper(date)
    }

    private fun preloadPdfs(pages: List<NewspaperPage>) {
        val context = getApplication<Application>()
        pages.forEach { page ->
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    PdfCache.downloadAndCache(context, page.pdfUrl)
                } catch (_: Exception) {}
            }
        }
    }
}

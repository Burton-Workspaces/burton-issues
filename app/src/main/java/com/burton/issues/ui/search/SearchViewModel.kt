package com.burton.issues.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.burton.issues.data.repository.IssuesRepository
import com.burton.issues.domain.IssueSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUi(
    val query: String = "",
    val hits: List<IssueSummary> = emptyList(),
    val scanning: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: IssuesRepository,
) : ViewModel() {
    private val _ui = MutableStateFlow(SearchUi())
    val ui = _ui.asStateFlow()
    private var job: Job? = null

    fun onQuery(value: String) {
        _ui.update { it.copy(query = value) }
        job?.cancel()
        if (value.isBlank()) {
            _ui.update { it.copy(hits = emptyList(), scanning = false, error = null) }
            return
        }
        job = viewModelScope.launch {
            delay(280)
            _ui.update { it.copy(scanning = true, error = null) }
            runCatching { repository.search(value) }
                .onSuccess { hits -> _ui.update { it.copy(hits = hits, scanning = false) } }
                .onFailure { error -> _ui.update { it.copy(scanning = false, error = error.message) } }
        }
    }
}

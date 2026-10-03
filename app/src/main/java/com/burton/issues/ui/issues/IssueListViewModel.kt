package com.burton.issues.ui.issues

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.burton.issues.data.repository.IssuesRepository
import com.burton.issues.domain.IssueQuery
import com.burton.issues.domain.IssueStateFilter
import com.burton.issues.domain.IssueSummary
import com.burton.issues.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class IssueListUi(
    val repo: String,
    val appName: String = "",
    val issues: List<IssueSummary> = emptyList(),
    val filter: IssueStateFilter = IssueStateFilter.OPEN,
    val scanning: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class IssueListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: IssuesRepository,
) : ViewModel() {
    val repo: String = Routes.decode(savedStateHandle["repo"])

    private val _ui = MutableStateFlow(
        IssueListUi(
            repo = repo,
            appName = repository.state.value.catalog.firstOrNull { it.repo == repo }?.name ?: repo.substringAfterLast('/'),
        ),
    )
    val ui = _ui.asStateFlow()

    init {
        refresh()
    }

    fun setFilter(filter: IssueStateFilter) {
        _ui.update { it.copy(filter = filter) }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _ui.update { it.copy(scanning = true, error = null) }
            runCatching { repository.listIssues(repo, IssueQuery(state = _ui.value.filter)) }
                .onSuccess { issues -> _ui.update { it.copy(issues = issues, scanning = false) } }
                .onFailure { error -> _ui.update { it.copy(scanning = false, error = error.message) } }
        }
    }
}

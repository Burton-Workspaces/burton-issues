package com.burton.issues.ui.composeissue

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.burton.issues.data.repository.IssuesRepository
import com.burton.issues.domain.NewIssueDraft
import com.burton.issues.domain.TrackedApp
import com.burton.issues.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ComposeUi(
    val repo: String = "",
    val apps: List<TrackedApp> = emptyList(),
    val title: String = "",
    val body: String = "",
    val sending: Boolean = false,
    val error: String? = null,
    val createdNumber: Int? = null,
)

@HiltViewModel
class ComposeIssueViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: IssuesRepository,
) : ViewModel() {
    private val initialRepo = Routes.decode(savedStateHandle.get<String>("repo"))

    private val _ui = MutableStateFlow(
        ComposeUi(
            repo = initialRepo,
            apps = repository.state.value.catalog,
        ),
    )
    val ui = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            repository.state.collect { snap ->
                val pending = snap.pendingNewIssue
                val resolved = pending?.let { request -> snap.catalog.firstOrNull { request.matches(it) } }
                val ready = pending != null &&
                    (resolved != null || snap.catalog.isNotEmpty() || !pending.hasTarget)
                if (ready) {
                    repository.consumeNewIssue()
                }
                _ui.update { current ->
                    current.copy(
                        apps = snap.catalog,
                        repo = when {
                            resolved != null -> resolved.repo
                            current.repo.isNotBlank() -> current.repo
                            initialRepo.isNotBlank() -> initialRepo
                            else -> pending?.repo.orEmpty()
                        },
                        title = when {
                            pending == null -> current.title
                            pending.title.isNotBlank() -> pending.title
                            else -> current.title
                        },
                        body = when {
                            pending == null -> current.body
                            pending.body.isNotBlank() -> pending.body
                            else -> current.body
                        },
                    )
                }
            }
        }
    }

    fun setRepo(repo: String) = _ui.update { it.copy(repo = repo, error = null) }
    fun onTitle(value: String) = _ui.update { it.copy(title = value) }
    fun onBody(value: String) = _ui.update { it.copy(body = value) }

    fun submit() {
        val repo = _ui.value.repo
        val title = _ui.value.title.trim()
        if (repo.isBlank()) {
            _ui.update { it.copy(error = "Choose an app.") }
            return
        }
        if (title.isBlank()) {
            _ui.update { it.copy(error = "Title is required.") }
            return
        }
        viewModelScope.launch {
            _ui.update { it.copy(sending = true, error = null) }
            runCatching { repository.createIssue(repo, NewIssueDraft(title = title, body = _ui.value.body)) }
                .onSuccess { created -> _ui.update { it.copy(sending = false, createdNumber = created.number) } }
                .onFailure { error -> _ui.update { it.copy(sending = false, error = error.message) } }
        }
    }
}

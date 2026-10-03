package com.burton.issues.ui.issue

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.burton.issues.data.repository.IssuesRepository
import com.burton.issues.domain.IssueComment
import com.burton.issues.domain.IssueLabel
import com.burton.issues.domain.IssueMilestone
import com.burton.issues.domain.IssuePatch
import com.burton.issues.domain.IssueSummary
import com.burton.issues.domain.IssueUser
import com.burton.issues.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class IssueUi(
    val repo: String,
    val number: Int,
    val issue: IssueSummary? = null,
    val comments: List<IssueComment> = emptyList(),
    val labels: List<IssueLabel> = emptyList(),
    val assignees: List<IssueUser> = emptyList(),
    val milestones: List<IssueMilestone> = emptyList(),
    val draft: String = "",
    val editTitle: String = "",
    val editBody: String = "",
    val editing: Boolean = false,
    val scanning: Boolean = false,
    val sending: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class IssueViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: IssuesRepository,
) : ViewModel() {
    val repo: String = Routes.decode(savedStateHandle["repo"])
    val number: Int = savedStateHandle.get<Int>("number") ?: savedStateHandle.get<String>("number")?.toIntOrNull() ?: 0

    private val _ui = MutableStateFlow(IssueUi(repo = repo, number = number))
    val ui = _ui.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _ui.update { it.copy(scanning = true, error = null) }
            runCatching {
                val detail = repository.getIssue(repo, number)
                val labels = runCatching { repository.listLabels(repo) }.getOrDefault(emptyList())
                val assignees = runCatching { repository.listAssignees(repo) }.getOrDefault(emptyList())
                val milestones = runCatching { repository.listMilestones(repo) }.getOrDefault(emptyList())
                Triple(detail, labels, assignees) to milestones
            }.onSuccess { (triple, milestones) ->
                val (detail, labels, assignees) = triple
                _ui.update {
                    it.copy(
                        issue = detail.issue,
                        comments = detail.comments,
                        labels = labels,
                        assignees = assignees,
                        milestones = milestones,
                        editTitle = detail.issue.title,
                        editBody = detail.issue.body,
                        scanning = false,
                    )
                }
            }.onFailure { error ->
                _ui.update { it.copy(scanning = false, error = error.message) }
            }
        }
    }

    fun onDraft(value: String) {
        _ui.update { it.copy(draft = value) }
    }

    fun sendComment() {
        val body = _ui.value.draft.trim()
        if (body.isBlank()) return
        viewModelScope.launch {
            _ui.update { it.copy(sending = true, error = null) }
            runCatching { repository.addComment(repo, number, body) }
                .onSuccess { comment ->
                    _ui.update { it.copy(comments = it.comments + comment, draft = "", sending = false) }
                }
                .onFailure { error -> _ui.update { it.copy(sending = false, error = error.message) } }
        }
    }

    fun toggleState() {
        val open = _ui.value.issue?.open ?: return
        patch(IssuePatch(state = if (open) "closed" else "open"))
    }

    fun setLabels(names: List<String>) = patch(IssuePatch(labels = names))

    fun setAssignees(logins: List<String>) = patch(IssuePatch(assignees = logins))

    fun setMilestone(number: Int?) {
        patch(if (number == null) IssuePatch(clearMilestone = true) else IssuePatch(milestone = number))
    }

    fun saveEdit() {
        val ui = _ui.value
        patch(IssuePatch(title = ui.editTitle.trim(), body = ui.editBody))
        _ui.update { it.copy(editing = false) }
    }

    fun onEditTitle(value: String) = _ui.update { it.copy(editTitle = value) }
    fun onEditBody(value: String) = _ui.update { it.copy(editBody = value) }
    fun setEditing(on: Boolean) = _ui.update { it.copy(editing = on) }

    fun deleteComment(id: Long) {
        viewModelScope.launch {
            runCatching { repository.deleteComment(repo, id) }
                .onSuccess { _ui.update { it.copy(comments = it.comments.filterNot { c -> c.id == id }) } }
                .onFailure { error -> _ui.update { it.copy(error = error.message) } }
        }
    }

    private fun patch(patch: IssuePatch) {
        viewModelScope.launch {
            runCatching { repository.updateIssue(repo, number, patch) }
                .onSuccess { issue -> _ui.update { it.copy(issue = issue, error = null) } }
                .onFailure { error -> _ui.update { it.copy(error = error.message) } }
        }
    }
}

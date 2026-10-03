package com.burton.issues.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.burton.issues.data.repository.IssuesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: IssuesRepository,
) : ViewModel() {
    val state = repository.state

    fun signOut() {
        viewModelScope.launch { repository.signOut() }
    }
}

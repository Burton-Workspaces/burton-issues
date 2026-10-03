package com.burton.issues

import androidx.lifecycle.ViewModel
import com.burton.issues.data.repository.IssuesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    repository: IssuesRepository,
) : ViewModel() {
    val state = repository.state

    init {
        repository.start()
    }
}

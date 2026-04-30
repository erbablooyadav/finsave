package com.finsave.feature.splitter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finsave.domain.model.SplitterGroup
import com.finsave.domain.repository.SplitterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class SplitterViewModel @Inject constructor(
    private val splitterRepository: SplitterRepository
) : ViewModel() {

    val groups = splitterRepository.getActiveGroups()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createGroup(name: String, emoji: String) {
        viewModelScope.launch {
            val group = SplitterGroup(
                name = name,
                emoji = emoji,
                createdAt = LocalDateTime.now(),
                isSettled = false,
                isArchived = false
            )
            splitterRepository.insertGroup(group)
        }
    }
}

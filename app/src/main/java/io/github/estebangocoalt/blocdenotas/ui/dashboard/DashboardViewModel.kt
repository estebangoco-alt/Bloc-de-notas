package io.github.estebangocoalt.blocdenotas.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.estebangocoalt.blocdenotas.blocApp
import io.github.estebangocoalt.blocdenotas.data.DashboardData
import io.github.estebangocoalt.blocdenotas.data.DashboardLogic
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

class DashboardViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = app.blocApp.repository

    val data: StateFlow<DashboardData> =
        combine(repository.notes, repository.boards, repository.columns) { notes, boards, columns ->
            DashboardLogic.compute(notes.map { it.note }, boards, columns, LocalDate.now().toEpochDay())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardData())
}

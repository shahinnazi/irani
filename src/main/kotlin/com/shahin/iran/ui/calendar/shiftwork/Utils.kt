package com.shahin.iran.ui.calendar.shiftwork

import android.content.Context
import androidx.core.content.edit
import com.shahin.iran.PREF_SHIFT_WORK_RECURS
import com.shahin.iran.PREF_SHIFT_WORK_SETTING
import com.shahin.iran.PREF_SHIFT_WORK_STARTING_JDN
import com.shahin.iran.entities.Jdn
import com.shahin.iran.entities.ShiftWorkRecord
import com.shahin.iran.global.updateStoredPreference
import com.shahin.iran.utils.preferences
import com.shahin.iran.utils.putJdn

fun saveShiftWorkState(context: Context, viewModel: ShiftWorkViewModel) {
    val result = viewModel.shiftWorks.value.filter { it.length != 0 }.joinToString(",") {
        "${it.type.replace("=", "").replace(",", "")}=${it.length}"
    }

    context.preferences.edit {
        if (result.isEmpty()) remove(PREF_SHIFT_WORK_STARTING_JDN)
        else putJdn(PREF_SHIFT_WORK_STARTING_JDN, viewModel.startingDate.value)
        putString(PREF_SHIFT_WORK_SETTING, result)
        putBoolean(PREF_SHIFT_WORK_RECURS, viewModel.recurs.value)
    }

    updateStoredPreference(context)
}

fun fillViewModelFromGlobalVariables(shiftWorkViewModel: ShiftWorkViewModel, selectedJdn: Jdn) {
    shiftWorkViewModel.changeShiftWorks(
        com.shahin.iran.global.shiftWorks
            .takeIf { it.isNotEmpty() } ?: listOf(ShiftWorkRecord(shiftWorkKeyToString("d"), 1))
    )
    shiftWorkViewModel.changeIsFirstSetup(false)
    shiftWorkViewModel.changeStartingDate(
        com.shahin.iran.global.shiftWorkStartingJdn ?: shiftWorkViewModel.run {
            shiftWorkViewModel.changeIsFirstSetup(true)
            selectedJdn
        }
    )
    shiftWorkViewModel.changeRecurs(com.shahin.iran.global.shiftWorkRecurs)
}

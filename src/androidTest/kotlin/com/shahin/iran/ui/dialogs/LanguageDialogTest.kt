package com.shahin.iran.ui.dialogs

import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.shahin.iran.R
import com.shahin.iran.entities.Language
import com.shahin.iran.ui.settings.interfacecalendar.LanguageDialog
import org.junit.Rule
import org.junit.Test

class LanguageDialogTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun randomLanguageClickTest() {
        var languageString = ""
        composeTestRule.setContent {
            languageString = stringResource(R.string.language)
            LanguageDialog {}
        }
        composeTestRule.onNodeWithText(languageString)

        val language = Language.entries.random()
        println("\n\n\nSelecting $language in language preference switch dialog\n\n\n")
        composeTestRule.onNodeWithText(language.nativeName)
            .assertHasClickAction()
            .performClick()
    }
}

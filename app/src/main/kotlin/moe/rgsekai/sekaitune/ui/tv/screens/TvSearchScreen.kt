/*
 * Sekai Tune (2026)
 * © Sekai Tune - github.com/rgsekai/sekai-tune
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rgsekai.sekaitune.ui.tv.screens

import android.view.KeyEvent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import moe.rgsekai.sekaitune.LocalDatabase
import moe.rgsekai.sekaitune.R
import moe.rgsekai.sekaitune.constants.PauseSearchHistoryKey
import moe.rgsekai.sekaitune.db.entities.SearchHistory
import moe.rgsekai.sekaitune.ui.screens.search.onlineSearchResultRoute
import moe.rgsekai.sekaitune.ui.utils.safeRequestFocus
import moe.rgsekai.sekaitune.utils.rememberPreference

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput

import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager

/**
 * TV Search Screen containing exclusively the search input field for TV navigation.
 * All discovery tabs, music recognition pills, and mood/genre shelves are removed.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TvSearchScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    firstFocusRequester: FocusRequester? = null,
    upFocusRequester: FocusRequester? = null,
) {
    val database = LocalDatabase.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val pauseSearchHistory by rememberPreference(PauseSearchHistoryKey, defaultValue = false)

    var queryValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(""))
    }

    val defaultSearchFieldFocusRequester = remember { FocusRequester() }
    val searchFieldFocusRequester = firstFocusRequester ?: defaultSearchFieldFocusRequester
    val clearButtonFocusRequester = remember { FocusRequester() }
    val submitButtonFocusRequester = remember { FocusRequester() }

    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val clearInteractionSource = remember { MutableInteractionSource() }
    val isClearFocused by clearInteractionSource.collectIsFocusedAsState()

    val submitInteractionSource = remember { MutableInteractionSource() }
    val isSubmitFocused by submitInteractionSource.collectIsFocusedAsState()

    fun performSearch(queryText: String) {
        val trimmed = queryText.trim()
        if (trimmed.isNotEmpty()) {
            keyboardController?.hide()
            navController.navigate(onlineSearchResultRoute(trimmed)) {
                launchSingleTop = true
            }
            if (!pauseSearchHistory) {
                database.query {
                    insert(SearchHistory(query = trimmed))
                }
            }
        }
    }

    val activePrimary = MaterialTheme.colorScheme.primary
    val isAnyFieldFocused = isFocused || isClearFocused || isSubmitFocused

    val borderColor by animateColorAsState(
        targetValue = if (isAnyFieldFocused) activePrimary else Color.White.copy(alpha = 0.12f),
        animationSpec = tween(150),
        label = "tvSearchBorderColor",
    )

    val containerColor by animateColorAsState(
        targetValue = if (isAnyFieldFocused) {
            MaterialTheme.colorScheme.surfaceContainerHighest
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        animationSpec = tween(150),
        label = "tvSearchContainerColor",
    )

    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // TV Search Input Pill
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .clip(CircleShape)
                    .background(containerColor)
                    .border(if (isAnyFieldFocused) 2.5.dp else 1.dp, borderColor, CircleShape)
                    .pointerInput(Unit) {
                        detectTapGestures {
                            searchFieldFocusRequester.safeRequestFocus()
                            keyboardController?.show()
                        }
                    }
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.search),
                        contentDescription = "Search icon",
                        tint = if (isAnyFieldFocused) activePrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp),
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (queryValue.text.isEmpty()) {
                            Text(
                                text = stringResource(R.string.search_yt_music),
                                style = TextStyle(
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                ),
                                maxLines = 1,
                            )
                        }

                        BasicTextField(
                            value = queryValue,
                            onValueChange = { queryValue = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(searchFieldFocusRequester)
                                .focusProperties {
                                    if (upFocusRequester != null) {
                                        up = upFocusRequester
                                    }
                                    down = FocusRequester.Cancel
                                    left = FocusRequester.Cancel
                                    right = if (queryValue.text.isNotEmpty()) {
                                        clearButtonFocusRequester
                                    } else {
                                        FocusRequester.Cancel
                                    }
                                }
                                .onPreviewKeyEvent { keyEvent ->
                                    if (keyEvent.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                                    val keyCode = keyEvent.nativeKeyEvent.keyCode
                                    val isEnter = keyEvent.key == Key.DirectionCenter ||
                                        keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
                                        keyCode == KeyEvent.KEYCODE_BUTTON_A
                                    val isSearchSubmit = keyEvent.key == Key.Enter ||
                                        keyEvent.key == Key.NumPadEnter ||
                                        keyCode == KeyEvent.KEYCODE_ENTER

                                    if (keyEvent.key == Key.DirectionUp || keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                                        val moved = upFocusRequester?.safeRequestFocus() ?: false
                                        if (!moved) {
                                            focusManager.moveFocus(FocusDirection.Up)
                                        }
                                        return@onPreviewKeyEvent true
                                    }

                                    if (keyEvent.key == Key.DirectionDown || keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                                        return@onPreviewKeyEvent true
                                    }

                                    if ((keyEvent.key == Key.DirectionRight || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) && queryValue.text.isNotEmpty()) {
                                        val moved = clearButtonFocusRequester.safeRequestFocus()
                                        if (moved) return@onPreviewKeyEvent true
                                    }

                                    if ((keyEvent.key == Key.DirectionLeft || keyCode == KeyEvent.KEYCODE_DPAD_LEFT) && queryValue.text.isEmpty()) {
                                        return@onPreviewKeyEvent true
                                    }

                                    if (isSearchSubmit && queryValue.text.isNotBlank()) {
                                        performSearch(queryValue.text)
                                        return@onPreviewKeyEvent true
                                    }

                                    if (isEnter) {
                                        keyboardController?.show()
                                        return@onPreviewKeyEvent true
                                    }
                                    false
                                },
                            singleLine = true,
                            textStyle = TextStyle(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                            ),
                            cursorBrush = SolidColor(activePrimary),
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Search,
                                keyboardType = KeyboardType.Text,
                            ),
                            keyboardActions = KeyboardActions(
                                onSearch = { performSearch(queryValue.text) },
                            ),
                            interactionSource = interactionSource,
                        )
                    }

                    if (queryValue.text.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                queryValue = TextFieldValue("")
                                searchFieldFocusRequester.safeRequestFocus()
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .focusRequester(clearButtonFocusRequester)
                                .focusProperties {
                                    if (upFocusRequester != null) {
                                        up = upFocusRequester
                                    }
                                    left = searchFieldFocusRequester
                                    right = submitButtonFocusRequester
                                    down = FocusRequester.Cancel
                                }
                                .onPreviewKeyEvent { keyEvent ->
                                    if (keyEvent.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                                    val code = keyEvent.nativeKeyEvent.keyCode
                                    when {
                                        keyEvent.key == Key.DirectionUp || code == KeyEvent.KEYCODE_DPAD_UP -> {
                                            val moved = upFocusRequester?.safeRequestFocus() ?: false
                                            if (!moved) {
                                                focusManager.moveFocus(FocusDirection.Up)
                                            }
                                            true
                                        }
                                        keyEvent.key == Key.DirectionLeft || code == KeyEvent.KEYCODE_DPAD_LEFT -> {
                                            searchFieldFocusRequester.safeRequestFocus()
                                        }
                                        keyEvent.key == Key.DirectionRight || code == KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                            submitButtonFocusRequester.safeRequestFocus()
                                        }
                                        keyEvent.key == Key.DirectionDown || code == KeyEvent.KEYCODE_DPAD_DOWN -> true
                                        else -> false
                                    }
                                }
                                .focusable(interactionSource = clearInteractionSource),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.close),
                                contentDescription = "Clear search query",
                                tint = if (isClearFocused) activePrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp),
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        IconButton(
                            onClick = { performSearch(queryValue.text) },
                            modifier = Modifier
                                .size(36.dp)
                                .focusRequester(submitButtonFocusRequester)
                                .focusProperties {
                                    if (upFocusRequester != null) {
                                        up = upFocusRequester
                                    }
                                    left = clearButtonFocusRequester
                                    right = FocusRequester.Cancel
                                    down = FocusRequester.Cancel
                                }
                                .onPreviewKeyEvent { keyEvent ->
                                    if (keyEvent.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                                    val code = keyEvent.nativeKeyEvent.keyCode
                                    when {
                                        keyEvent.key == Key.DirectionUp || code == KeyEvent.KEYCODE_DPAD_UP -> {
                                            val moved = upFocusRequester?.safeRequestFocus() ?: false
                                            if (!moved) {
                                                focusManager.moveFocus(FocusDirection.Up)
                                            }
                                            true
                                        }
                                        keyEvent.key == Key.DirectionLeft || code == KeyEvent.KEYCODE_DPAD_LEFT -> {
                                            clearButtonFocusRequester.safeRequestFocus()
                                        }
                                        keyEvent.key == Key.DirectionRight || code == KeyEvent.KEYCODE_DPAD_RIGHT -> true
                                        keyEvent.key == Key.DirectionDown || code == KeyEvent.KEYCODE_DPAD_DOWN -> true
                                        else -> false
                                    }
                                }
                                .focusable(interactionSource = submitInteractionSource),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.arrow_forward),
                                contentDescription = "Submit search",
                                tint = if (isSubmitFocused) activePrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.language),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }
        }
    }
}

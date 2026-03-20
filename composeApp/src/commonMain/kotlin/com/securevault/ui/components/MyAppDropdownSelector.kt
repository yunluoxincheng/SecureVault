package com.securevault.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.securevault.ui.animation.AnimationTokens
import com.securevault.ui.theme.layout
import com.securevault.ui.theme.spacing

@Composable
fun <T> MyAppDropdownSelector(
    label: String,
    selectedText: String,
    options: List<MyAppDropdownOption<T>>,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxWidth()) {
        MyAppListItem(
            headline = label,
            supportingText = selectedText,
            overlineText = supportingText,
            onClick = { expanded = true },
            trailing = {
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "收起选项" else "展开选项",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
        )

        if (expanded) {
            Dialog(
                onDismissRequest = { expanded = false },
                properties = DialogProperties(usePlatformDefaultWidth = false),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.32f)),
                    contentAlignment = androidx.compose.ui.Alignment.Center,
                ) {
                    AnimatedVisibility(
                        visible = expanded,
                        enter = fadeIn(tween(AnimationTokens.dialogDuration)) +
                            scaleIn(
                                initialScale = 0.92f,
                                animationSpec = tween(
                                    AnimationTokens.dialogDuration,
                                    easing = AnimationTokens.easeOutBack,
                                ),
                            ),
                        exit = fadeOut(tween(AnimationTokens.crossFadeDuration)) +
                            scaleOut(
                                targetScale = 0.92f,
                                animationSpec = tween(AnimationTokens.crossFadeDuration),
                            ),
                    ) {
                        MyAppCard(
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .widthIn(max = MaterialTheme.layout.authFormMaxWidth),
                            variant = MyAppCardVariant.Elevated,
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs),
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(horizontal = MaterialTheme.spacing.xs),
                                )

                                options.forEach { option ->
                                    val isSelected = option.label == selectedText
                                    MyAppSelectionRow(
                                        headline = option.label,
                                        selected = isSelected,
                                        onClick = {
                                            expanded = false
                                            onSelect(option.value)
                                        },
                                        container = if (isSelected) {
                                            MyAppListItemContainer.Filled
                                        } else {
                                            MyAppListItemContainer.None
                                        },
                                    )
                                }

                                MyAppButton(
                                    text = "取消",
                                    onClick = { expanded = false },
                                    variant = MyAppButtonVariant.Ghost,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

data class MyAppDropdownOption<T>(
    val value: T,
    val label: String,
)

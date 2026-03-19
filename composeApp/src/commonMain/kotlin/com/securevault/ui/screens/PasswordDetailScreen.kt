package com.securevault.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.securevault.data.PasswordEntry
import com.securevault.ui.animation.AnimationTokens
import com.securevault.ui.components.DetailRow
import com.securevault.ui.components.MyAppButton
import com.securevault.ui.components.MyAppButtonVariant
import com.securevault.ui.components.MyAppCard
import com.securevault.ui.components.MyAppCardVariant
import com.securevault.ui.components.MyAppDialog
import com.securevault.ui.components.MyAppDivider
import com.securevault.ui.components.MyAppIconAction
import com.securevault.ui.components.MyAppTopBar
import com.securevault.ui.theme.SecurityModeColor
import com.securevault.ui.theme.layout
import com.securevault.ui.theme.spacing
import kotlinx.coroutines.delay

@Composable
fun PasswordDetailScreen(
    entry: PasswordEntry,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onCopyUsername: () -> Unit,
    onCopyPassword: () -> Unit
) {
    var showPassword by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var passwordCopied by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    LaunchedEffect(passwordCopied) {
        if (passwordCopied) {
            delay(1500)
            passwordCopied = false
        }
    }

    val copyIconTint by animateColorAsState(
        targetValue = if (passwordCopied) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(AnimationTokens.copyFeedbackDuration),
        label = "passwordCopyTint"
    )

    MyAppDialog(
        visible = showDeleteConfirm,
        title = "确认删除",
        message = "删除「${entry.title}」后将无法恢复，是否继续？",
        confirmText = "删除",
        dismissText = "取消",
        onConfirm = {
            showDeleteConfirm = false
            onDelete()
        },
        onDismiss = { showDeleteConfirm = false },
        isDanger = true,
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = MaterialTheme.layout.pageMaxWidth)
                .align(Alignment.TopCenter)
                .padding(horizontal = MaterialTheme.layout.pageHorizontalPadding)
                .verticalScroll(scrollState)
                .padding(bottom = MaterialTheme.layout.bottomBarActionInset),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.layout.contentSpacing),
        ) {
            MyAppTopBar(
                title = "密码详情",
                onBack = onBack,
            )

            AnimatedVisibility(visible = entry.securityMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            SecurityModeColor.copy(alpha = 0.1f),
                            MaterialTheme.shapes.medium,
                        )
                        .padding(MaterialTheme.spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "安全模式：密码不可见，仅可使用",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SecurityModeColor,
                    )
                }
            }

            DetailRow(title = "标题", value = entry.title)
            DetailRow(
                title = "用户名",
                value = entry.username,
                showCopy = true,
                onCopy = onCopyUsername,
            )

            // Password row (custom - has show/hide toggle)
            MyAppCard(
                modifier = Modifier.fillMaxWidth(),
                variant = MyAppCardVariant.Filled,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = MaterialTheme.layout.cardPaddingHorizontal,
                            end = MaterialTheme.spacing.sm,
                            top = MaterialTheme.layout.cardPaddingVertical,
                            bottom = MaterialTheme.layout.cardPaddingVertical,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "密码",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        val displayValue = if (entry.securityMode || !showPassword) "••••••••••••" else entry.password
                        Text(
                            text = displayValue,
                            style = if (!showPassword || entry.securityMode)
                                MaterialTheme.typography.bodyLarge
                            else
                                MaterialTheme.typography.bodyLarge.copy(
                                    fontFamily = com.securevault.ui.theme.PasswordFontFamily
                                ),
                        )
                    }

                    if (!entry.securityMode) {
                        MyAppIconAction(
                            icon = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (showPassword) "隐藏" else "显示",
                            onClick = { showPassword = !showPassword },
                        )
                    }
                    MyAppIconAction(
                        icon = if (passwordCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                        contentDescription = if (entry.securityMode) "使用" else "复制",
                        onClick = {
                            passwordCopied = true
                            onCopyPassword()
                        },
                        tint = copyIconTint,
                    )
                }
            }

            val url = entry.url
            if (!url.isNullOrBlank()) {
                DetailRow(title = "URL", value = url)
            }
            val notes = entry.notes
            if (!notes.isNullOrBlank()) {
                DetailRow(title = "备注", value = notes, maxLines = 10)
            }

            Text(
                text = "分类：${entry.category}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(top = MaterialTheme.spacing.xs),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.96f))
                .widthIn(max = MaterialTheme.layout.pageMaxWidth)
                .align(Alignment.BottomCenter)
        ) {
            MyAppDivider(
                horizontalInset = 0.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = MaterialTheme.layout.pageHorizontalPadding,
                        vertical = MaterialTheme.spacing.md,
                    ),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
            ) {
                MyAppButton(
                    text = "删除",
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.weight(1f),
                    leadingIcon = Icons.Default.Delete,
                    variant = MyAppButtonVariant.Secondary,
                )
                MyAppButton(
                    text = "编辑",
                    onClick = onEdit,
                    modifier = Modifier.weight(1f),
                    leadingIcon = Icons.Default.Edit,
                )
            }
        }
    }
}

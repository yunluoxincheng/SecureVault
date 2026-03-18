package com.securevault.ui.screens

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
import androidx.compose.material3.IconButton
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
import com.securevault.ui.components.DetailRow
import com.securevault.ui.components.SvButton
import com.securevault.ui.components.SvConfirmDialog
import com.securevault.ui.components.SvDangerButton
import com.securevault.ui.components.SvFilledCard
import com.securevault.ui.components.SvOutlinedButton
import com.securevault.ui.components.SvTopBar
import com.securevault.ui.theme.SecurityModeColor
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
        animationSpec = tween(300),
        label = "passwordCopyTint"
    )

    SvConfirmDialog(
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
                .padding(horizontal = MaterialTheme.spacing.md)
                .verticalScroll(scrollState)
                .padding(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
        ) {
            SvTopBar(
                title = "密码详情",
                onBack = onBack,
            )

            if (entry.securityMode) {
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
            SvFilledCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = MaterialTheme.spacing.md,
                            top = MaterialTheme.spacing.sm + 2.dp,
                            bottom = MaterialTheme.spacing.sm + 2.dp,
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
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showPassword) "隐藏" else "显示",
                            )
                        }
                    }
                    IconButton(onClick = {
                        passwordCopied = true
                        onCopyPassword()
                    }) {
                        Icon(
                            imageVector = if (passwordCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                            contentDescription = if (entry.securityMode) "使用" else "复制",
                            tint = copyIconTint,
                        )
                    }
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

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = MaterialTheme.spacing.md, vertical = MaterialTheme.spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
        ) {
            SvOutlinedButton(
                text = "删除",
                onClick = { showDeleteConfirm = true },
                modifier = Modifier.weight(1f),
                leadingIcon = Icons.Default.Delete,
            )
            SvButton(
                text = "编辑",
                onClick = onEdit,
                modifier = Modifier.weight(1f),
                leadingIcon = Icons.Default.Edit,
            )
        }
    }
}

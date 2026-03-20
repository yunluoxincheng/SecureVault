package com.securevault.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import com.securevault.ui.animation.AnimationTokens
import com.securevault.ui.components.MyAppButton
import com.securevault.ui.components.MyAppButtonVariant
import com.securevault.ui.icons.LOGO_SCREEN_SCALE
import com.securevault.ui.icons.SecureVaultLogoIcon
import com.securevault.ui.theme.layout
import com.securevault.ui.theme.spacing
import kotlinx.coroutines.launch

private data class OnboardingPage(
    val title: String,
    val description: String,
)

@Composable
fun OnboardingScreen(
    onFinish: () -> Unit
) {
    val pages = listOf(
        OnboardingPage(
            title = "欢迎使用 SecureVault",
            description = "离线优先的本地密码管理器，你的数据只属于你",
        ),
        OnboardingPage(
            title = "军事级加密保护",
            description = "密码使用 XChaCha20-Poly1305 加密\n主密码不会上传至任何服务器",
        ),
        OnboardingPage(
            title = "安全便捷兼得",
            description = "支持安全剪贴板自动清除与生物识别\n让安全不再是负担",
        ),
    )
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = MaterialTheme.layout.pageHorizontalPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = MaterialTheme.layout.pageMaxWidth)
                .weight(1f),
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) { page ->
                OnboardingPageContent(page = pages[page])
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = MaterialTheme.spacing.md),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(pages.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    val indicatorWidth by animateDpAsState(
                        targetValue = if (isSelected) {
                            MaterialTheme.layout.inlineIndicatorSelectedWidth
                        } else {
                            MaterialTheme.layout.inlineIndicatorSize
                        },
                        animationSpec = tween(AnimationTokens.cardAppearDuration),
                        label = "indicatorWidth"
                    )
                    val color by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        animationSpec = tween(AnimationTokens.cardAppearDuration),
                        label = "indicatorColor"
                    )
                    Box(
                        modifier = Modifier
                            .padding(horizontal = MaterialTheme.spacing.xs)
                            .height(MaterialTheme.layout.inlineIndicatorSize)
                            .width(indicatorWidth)
                            .clip(CircleShape)
                            .background(color)
                    )
                }
            }
        }

        if (pagerState.currentPage < pages.lastIndex) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = MaterialTheme.layout.pageMaxWidth)
                    .padding(bottom = MaterialTheme.spacing.md),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MyAppButton(
                    text = "跳过",
                    onClick = onFinish,
                    variant = MyAppButtonVariant.Text,
                )
                MyAppButton(
                    text = "下一步",
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    },
                    modifier = Modifier.width(MaterialTheme.layout.onboardingActionWidth),
                )
            }
        } else {
            MyAppButton(
                text = "开始使用",
                onClick = onFinish,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = MaterialTheme.layout.pageMaxWidth)
                    .padding(bottom = MaterialTheme.spacing.md),
            )
        }
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(MaterialTheme.spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        SecureVaultLogoIcon(
            contentDescription = null,
            modifier = Modifier.size(MaterialTheme.layout.heroIconSize * LOGO_SCREEN_SCALE),
        )
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.xl))
        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.md))
        Text(
            text = page.description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

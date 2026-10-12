package uz.sentinel.presentation.onboarding

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onFinishOnboarding: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()

    Scaffold(
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pager Indicators
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(3) { index ->
                        Surface(
                            shape = CircleShape,
                            color = if (pagerState.currentPage == index)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.size(if (pagerState.currentPage == index) 10.dp else 8.dp)
                        ) {}
                    }
                }

                Button(
                    onClick = {
                        if (pagerState.currentPage < 2) {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        } else {
                            onFinishOnboarding()
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (pagerState.currentPage == 2) "Boshlash" else "Keyingisi")
                }
            }
        }
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) { page ->
            when (page) {
                0 -> OnboardingPage(
                    title = "Haqiqiy Xavfsizlik",
                    description = "Zararli APK ilovalardan himoya, qulflash va fishing saytlarini real vaqtda to'xtatish tizimi.",
                    icon = Icons.Default.Security,
                    iconColor = MaterialTheme.colorScheme.primary
                )
                1 -> OnboardingPage(
                    title = "Nol Reklama & Maxfiylik",
                    description = "Ilovada hech qanday reklama yo'q, shaxsiy fotosuratlar yoki xabarlaringiz hech qachon serverlarga yuborilmaydi.",
                    icon = Icons.Default.PrivacyTip,
                    iconColor = Color(0xFF2E7D32)
                )
                2 -> OnboardingPage(
                    title = "Resurs Tejamkor",
                    description = "Batareyani ortiqcha sarflamaydi, tizimni qotirmaydi va faqat zarur paytda xavfsizlik choralarini ko'radi.",
                    icon = Icons.Default.CheckCircle,
                    iconColor = Color(0xFF00897B)
                )
            }
        }
    }
}

@Composable
fun OnboardingPage(
    title: String,
    description: String,
    icon: ImageVector,
    iconColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = iconColor.copy(alpha = 0.15f),
            modifier = Modifier.size(110.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxSize()
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
        Text(text = title, fontWeight = FontWeight.Bold, fontSize = 24.sp, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

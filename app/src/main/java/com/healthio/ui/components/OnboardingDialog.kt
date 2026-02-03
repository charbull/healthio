package com.healthio.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch

data class OnboardingPage(
    val title: String,
    val description: String,
    val icon: String
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingDialog(
    onDismiss: () -> Unit
) {
    val pages = listOf(
        OnboardingPage(
            "Welcome to Healthio",
            "Your minimalist, privacy-focused health companion. Let's get you set up in a few easy steps.",
            "🥗"
        ),
        OnboardingPage(
            "Step 1: AI Analysis",
            "Healthio uses Gemini AI to analyze your meals. Go to Settings and paste your free Google AI API key to start 'Snap & Go' logging.",
            "📸"
        ),
        OnboardingPage(
            "Step 2: Body Sync",
            "Healthio pulls data from Health Connect. Ensure Health Connect is installed, and your other apps (Garmin, Withings, etc.) are set to sync with it. Just grant Healthio permission when prompted!",
            "🏃‍♂️"
        ),
        OnboardingPage(
            "Step 3: Privacy First",
            "Connect your Google Drive in Settings. We only access files we create, keeping your personal documents 100% private.",
            "🔒"
        ),
        OnboardingPage(
            "Step 4: Goals",
            "Set your weight unit (kg/lbs) and nutrition goals in Settings. Your protein goal can even be calculated automatically!",
            "⚙️"
        ),
        OnboardingPage(
            "Step 5: Track History",
            "Tap the Calendar icon in the top right of the home screen to see your fasting consistency, workout intensity, and weight trends.",
            "📅"
        ),
        OnboardingPage(
            "Ready to go!",
            "Use the Flux Timer for fasting, the Camera for food, and the Stats page to track your progress. Welcome aboard!",
            "🚀"
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight(),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.height(320.dp)
                ) { index ->
                    val page = pages[index]
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(text = page.icon, fontSize = 64.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = page.title,
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = page.description,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Page Indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(pages.size) { iteration ->
                        val color = if (pagerState.currentPage == iteration) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.outlineVariant
                        Surface(
                            modifier = Modifier.padding(4.dp).size(8.dp),
                            shape = androidx.compose.foundation.shape.CircleShape,
                            color = color
                        ) {}
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (pagerState.currentPage > 0) {
                        TextButton(onClick = {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                        }) {
                            Text("Back")
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    if (pagerState.currentPage < pages.size - 1) {
                        Button(onClick = {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        }) {
                            Text("Next")
                        }
                    } else {
                        Button(onClick = onDismiss) {
                            Text("Get Started")
                        }
                    }
                }
            }
        }
    }
}

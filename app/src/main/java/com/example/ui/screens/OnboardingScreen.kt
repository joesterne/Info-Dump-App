package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.WavingHand
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    var currentStep by remember { mutableIntStateOf(0) }

    val steps = listOf(
        OnboardingStepData(
            title = "Welcome to Infodump",
            description = "A gentle, zero-pressure space to talk endlessly about your hyperfixations—or learn about someone else's.",
            icon = Icons.Filled.WavingHand,
            iconContentDesc = "Waving hand icon"
        ),
        OnboardingStepData(
            title = "The Matching System",
            description = "We match based on energy. Choose if you want to 'Infodump' (share your knowledge) or 'Listen' (learn something new) to find the perfect conversation dynamic.",
            icon = Icons.Filled.Favorite,
            iconContentDesc = "Heart icon representing matching"
        ),
        OnboardingStepData(
            title = "Interest Tags",
            description = "Add specific tags (like 'Trains' or 'Mycology') to your profile. We use these tags to connect you with people who share your exact passions.",
            icon = Icons.Filled.Tag,
            iconContentDesc = "Hashtag icon representing interest tags"
        )
    )

    Scaffold { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(500)) togetherWith fadeOut(animationSpec = tween(500))
                    },
                    label = "onboarding_step_animation"
                ) { stepIndex ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val step = steps[stepIndex]
                        
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(120.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = step.icon,
                                    contentDescription = step.iconContentDesc,
                                    modifier = Modifier.size(56.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        Text(
                            text = step.title,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.semantics { contentDescription = "Step ${stepIndex + 1} of ${steps.size}: ${step.title}" }
                        )

                        Text(
                            text = step.description,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(64.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    steps.forEachIndexed { index, _ ->
                        val isSelected = currentStep == index
                        Surface(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(if (isSelected) 12.dp else 8.dp),
                            shape = CircleShape,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        ) {}
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        if (currentStep < steps.size - 1) {
                            currentStep++
                        } else {
                            onComplete()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = MaterialTheme.shapes.large
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (currentStep < steps.size - 1) "Next" else "Get Started",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = if (currentStep < steps.size - 1) Icons.AutoMirrored.Filled.ArrowForward else Icons.Filled.Check,
                            contentDescription = null
                        )
                    }
                }
            }
        }
    }
}

data class OnboardingStepData(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val iconContentDesc: String
)

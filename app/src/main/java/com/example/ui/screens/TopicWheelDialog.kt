package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutQuart
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicWheelDialog(
    topics: List<String>,
    onDismiss: () -> Unit,
    onTopicSelected: (String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val rotation = remember { Animatable(0f) }
    var isSpinning by remember { mutableStateOf(false) }
    var selectedTopic by remember { mutableStateOf<String?>(null) }
    
    val textMeasurer = rememberTextMeasurer()
    val colors = listOf(
        Color(0xFFE57373), Color(0xFFF06292), Color(0xFFBA68C8), Color(0xFF9575CD),
        Color(0xFF7986CB), Color(0xFF64B5F6), Color(0xFF4FC3F7), Color(0xFF4DD0E1),
        Color(0xFF4DB6AC), Color(0xFF81C784), Color(0xFFAED581), Color(0xFFFF8A65)
    )

    AlertDialog(
        onDismissRequest = { if (!isSpinning) onDismiss() },
        title = { 
            Text(
                text = "Random Topic Wheel", 
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            ) 
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Not sure what to listen to? Give the wheel a spin!", style = MaterialTheme.typography.bodyMedium)
                
                Box(
                    modifier = Modifier
                        .size(260.dp)
                        .padding(top = 16.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 12.dp)
                    ) {
                        val sliceAngle = 360f / topics.size
                        val radius = size.minDimension / 2
                        val center = Offset(size.width / 2, size.height / 2)

                        rotate(rotation.value) {
                            for (i in topics.indices) {
                                val startAngle = i * sliceAngle
                                drawArc(
                                    color = colors[i % colors.size],
                                    startAngle = startAngle,
                                    sweepAngle = sliceAngle,
                                    useCenter = true,
                                    topLeft = Offset(center.x - radius, center.y - radius),
                                    size = Size(radius * 2, radius * 2)
                                )
                                
                                // Draw Text
                                rotate(startAngle + sliceAngle / 2) {
                                    val textLayoutResult = textMeasurer.measure(
                                        text = topics[i],
                                        style = TextStyle(
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    // position text along the radius
                                    val textOffset = Offset(
                                        x = center.x + radius * 0.2f, // distance from center
                                        y = center.y - textLayoutResult.size.height / 2f
                                    )
                                    drawText(
                                        textLayoutResult = textLayoutResult,
                                        topLeft = textOffset
                                    )
                                }
                            }
                        }
                    }
                    
                    // Pointer at the top (0 degrees or 270 depending on how we calculate)
                    // The arc draws from 3 o'clock (0 degrees). We rotate the canvas.
                    // Actually, top is 270 degrees.
                    Canvas(modifier = Modifier.size(24.dp).offset(y = (-4).dp)) {
                        val path = Path().apply {
                            moveTo(size.width / 2f, size.height)
                            lineTo(0f, 0f)
                            lineTo(size.width, 0f)
                            close()
                        }
                        drawPath(path, color = Color.DarkGray)
                    }
                }
                
                if (selectedTopic != null && !isSpinning) {
                    Text(
                        text = "Landed on: ${selectedTopic!!}", 
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedTopic != null) {
                        onTopicSelected(selectedTopic!!)
                        onDismiss()
                    } else if (!isSpinning) {
                        isSpinning = true
                        coroutineScope.launch {
                            val extraSpins = (3..6).random()
                            val randomSliceIndex = topics.indices.random()
                            val sliceAngle = 360f / topics.size
                            
                            // Top is at 270 degrees. 
                            // When unrotated, slice i goes from i*sliceAngle to (i+1)*sliceAngle.
                            // The center of slice i is i*sliceAngle + sliceAngle/2.
                            // To make the center of slice i point to 270 degrees:
                            // rotation + sliceCenter = 270 (mod 360)
                            // rotation = 270 - sliceCenter + 360k
                            
                            val sliceCenter = randomSliceIndex * sliceAngle + sliceAngle / 2f
                            var targetRotation = 270f - sliceCenter
                            while (targetRotation < 0f) targetRotation += 360f
                            
                            val totalRotation = targetRotation + (360f * extraSpins)
                            
                            rotation.animateTo(
                                targetValue = rotation.value + totalRotation,
                                animationSpec = tween(durationMillis = 3000, easing = EaseOutQuart)
                            )
                            
                            selectedTopic = topics[randomSliceIndex]
                            isSpinning = false
                        }
                    }
                },
                enabled = !isSpinning
            ) {
                Text(if (selectedTopic != null) "Search Topic" else "Spin!")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSpinning) {
                Text("Cancel")
            }
        }
    )
}

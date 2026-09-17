package com.janoski.retroframe

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color as AndroidColor
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraFront
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.komoui.KomoTheme
import com.komoui.components.Button as KomoButton
import kotlin.math.roundToInt

private val Ink = Color(0xFFF2F0E8)
private val Muted = Color(0xFFA6A49C)
private val Panel = Color(0xE90F0F0E)
private val Border = Color(0x3324211D)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent {
            KomoTheme {
                RetroFrameApp()
            }
        }
    }
}

@Composable
private fun RetroFrameApp() {
    val context = LocalContext.current
    val cameraController = remember { CameraController(context) }
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            setBackgroundColor(AndroidColor.BLACK)
        }
    }

    var cameraGranted by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { cameraGranted = it }

    LaunchedEffect(Unit) {
        if (!cameraGranted) permissionLauncher.launch(Manifest.permission.CAMERA)
        else cameraController.bind(previewView)
    }

    var selectedPreset by remember { mutableStateOf(FilmPreset.PORTRA_400) }
    var showControls by remember { mutableStateOf(false) }
    var isCapturing by remember { mutableStateOf(false) }
    var thumbnailUri by remember { mutableStateOf<android.net.Uri?>(null) }

    val adjustments = remember {
        mutableStateMapOf<FilmPreset, FilmAdjustments>().apply {
            FilmPreset.values().forEach { this[it] = it.base }
        }
    }
    val current = adjustments[selectedPreset] ?: selectedPreset.base

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (cameraGranted) {
            AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
        } else {
            PermissionPanel(onRequest = { permissionLauncher.launch(Manifest.permission.CAMERA) })
        }

        // Subtle live-look overlay. The exported photo receives the exact full-resolution recipe.
        FilmPreviewWash(adjustments = current)

        TopBar(
            preset = selectedPreset,
            onFlip = {
                cameraController.flip(previewView)
            },
            onToggleControls = { showControls = !showControls },
            modifier = Modifier.align(Alignment.TopCenter),
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Panel)
                .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 10.dp),
        ) {
            AnimatedVisibility(visible = showControls) {
                TuningPanel(
                    adjustments = current,
                    onChange = { updated -> adjustments[selectedPreset] = updated },
                    onReset = { adjustments[selectedPreset] = selectedPreset.base },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Text(
                text = "${selectedPreset.label.uppercase()}  ·  ${selectedPreset.stockDescription.uppercase()}",
                color = Muted,
                fontSize = 9.sp,
                letterSpacing = 1.15.sp,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 3.dp),
            )

            FilmPicker(
                presets = FilmPreset.values().toList(),
                selected = selectedPreset,
                onSelected = { selectedPreset = it },
            )

            Spacer(Modifier.height(8.dp))

            BottomControls(
                thumbnailUri = thumbnailUri,
                onGallery = {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        val target = thumbnailUri ?: android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                        setDataAndType(target, "image/*")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    runCatching { context.startActivity(intent) }
                        .onFailure { Toast.makeText(context, "Photos are in Pictures/RetroFrame", Toast.LENGTH_SHORT).show() }
                },
                onCapture = {
                    if (!isCapturing) {
                        isCapturing = true
                        cameraController.capture(previewView, current,
                            onSaved = { uri ->
                                thumbnailUri = uri
                                isCapturing = false
                                Toast.makeText(context, "Saved to RetroFrame", Toast.LENGTH_SHORT).show()
                            },
                            onError = {
                                isCapturing = false
                                Toast.makeText(context, it.message ?: "Capture failed", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                },
                isCapturing = isCapturing,
            )
        }
    }
}

@Composable
private fun FilmPreviewWash(adjustments: FilmAdjustments) {
    val warm = (adjustments.temperature * 0.06f).coerceIn(-0.08f, 0.08f)
    val fade = (adjustments.fade * 0.14f).coerceIn(0f, 0.14f)
    Box(
        Modifier.fillMaxSize().background(
            if (adjustments.saturation == 0f) Color(0x00000000)
            else Color(red = (0.5f + warm).coerceIn(0f, 1f), green = (0.48f + fade).coerceIn(0f, 1f), blue = (0.42f - warm).coerceIn(0f, 1f), alpha = 0.035f)
        )
    )
}

@Composable
private fun TopBar(
    preset: FilmPreset,
    onFlip: () -> Unit,
    onToggleControls: () -> Unit,
    modifier: Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(
            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 10.dp,
            start = 18.dp,
            end = 12.dp,
        ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text("RETROFRAME", color = Ink, fontSize = 13.sp, letterSpacing = 2.6.sp, fontWeight = FontWeight.Bold)
            Text(preset.shortLabel, color = Muted, fontSize = 11.sp, letterSpacing = 1.4.sp)
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onToggleControls) {
                Icon(Icons.Outlined.Tune, contentDescription = "Tune film", tint = Ink)
            }
            IconButton(onClick = onFlip) {
                Icon(Icons.Outlined.CameraFront, contentDescription = "Flip camera", tint = Ink)
            }
        }
    }
}

@Composable
private fun FilmPicker(
    presets: List<FilmPreset>,
    selected: FilmPreset,
    onSelected: (FilmPreset) -> Unit,
) {
    val scroll = rememberScrollState()
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(scroll).padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        presets.forEach { preset ->
            FilmChip(preset, selected == preset, onClick = { onSelected(preset) })
        }
    }
}

@Composable
private fun FilmChip(preset: FilmPreset, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier.width(82.dp).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.height(44.dp).fillMaxWidth().clip(RoundedCornerShape(12.dp)),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            preset.swatches.forEachIndexed { index, argb ->
                Box(modifier = Modifier.weight(1f).fillMaxSize().background(Color(argb)))
            }
        }
        Spacer(Modifier.height(5.dp))
        Text(
            preset.shortLabel,
            color = if (selected) Ink else Muted,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            letterSpacing = 0.8.sp,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TuningPanel(
    adjustments: FilmAdjustments,
    onChange: (FilmAdjustments) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier,
) {
    Column(modifier.padding(horizontal = 18.dp, vertical = 12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("TUNE", color = Ink, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.8.sp)
                Text("Adjust this film only", color = Muted, fontSize = 11.sp)
            }
            KomoButton(onClick = onReset) { Text("RESET", fontSize = 10.sp, letterSpacing = 1.sp) }
        }
        Spacer(Modifier.height(8.dp))
        TuningSlider("EXPOSURE", adjustments.exposure, -1f..1f, { onChange(adjustments.copy(exposure = it)) }, decimals = 2)
        TuningSlider("CONTRAST", adjustments.contrast, 0.65f..1.35f, { onChange(adjustments.copy(contrast = it)) }, decimals = 2)
        TuningSlider("SATURATION", adjustments.saturation, 0f..1.45f, { onChange(adjustments.copy(saturation = it)) }, decimals = 2)
        TuningSlider("WARMTH", adjustments.temperature, -1f..1f, { onChange(adjustments.copy(temperature = it)) }, decimals = 2)
        TuningSlider("FADE", adjustments.fade, 0f..0.65f, { onChange(adjustments.copy(fade = it)) }, decimals = 2)
        TuningSlider("GRAIN", adjustments.grain, 0f..0.95f, { onChange(adjustments.copy(grain = it)) }, decimals = 2)
        TuningSlider("VIGNETTE", adjustments.vignette, 0f..0.80f, { onChange(adjustments.copy(vignette = it)) }, decimals = 2)
        TuningSlider("INTENSITY", adjustments.intensity, 0f..1f, { onChange(adjustments.copy(intensity = it)) }, decimals = 2)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TuningSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit, decimals: Int) {
    val shown = String.format(java.util.Locale.US, "%.${decimals}f", value)
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Muted, fontSize = 9.sp, letterSpacing = 1.2.sp, modifier = Modifier.width(76.dp))
        Slider(
            value = value.coerceIn(range),
            onValueChange = onChange,
            valueRange = range,
            modifier = Modifier.weight(1f).height(28.dp),
            colors = SliderDefaults.colors(
                thumbColor = Ink,
                activeTrackColor = Ink,
                inactiveTrackColor = Color(0xFF3A3935),
            ),
        )
        Text(shown, color = Ink, fontSize = 9.sp, modifier = Modifier.width(42.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End)
    }
}

@Composable
private fun BottomControls(
    thumbnailUri: android.net.Uri?,
    onGallery: () -> Unit,
    onCapture: () -> Unit,
    isCapturing: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        RoundAction(icon = Icons.Outlined.Collections, onClick = onGallery)
        Box(
            modifier = Modifier.size(74.dp).shadow(10.dp, CircleShape).clip(CircleShape).background(Ink).clickable(onClick = onCapture),
            contentAlignment = Alignment.Center,
        ) {
            Box(modifier = Modifier.size(62.dp).clip(CircleShape).background(Color.Transparent).then(if (isCapturing) Modifier else Modifier.border(BorderStroke(2.dp, Color.Black.copy(alpha = 0.14f)), CircleShape)))
        }
        RoundAction(icon = Icons.Outlined.Refresh, onClick = { })
    }
}

@Composable
private fun RoundAction(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(48.dp).clip(CircleShape).background(Color(0x24000000)),
    ) {
        Icon(icon, contentDescription = null, tint = Ink)
    }
}

@Composable
private fun PermissionPanel(onRequest: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Camera access required", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text("RetroFrame needs the camera to show the film viewfinder.", color = Muted, fontSize = 13.sp)
        Spacer(Modifier.height(18.dp))
        KomoButton(onClick = onRequest) { Text("ALLOW CAMERA") }
    }
}

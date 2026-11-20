package com.example.feature.home

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import com.example.R
import com.example.core.designsystem.LocalMyatoColors
import com.example.core.designsystem.MyatoBrandBlack
import com.example.core.designsystem.MyatoBrandBlackSecondary
import com.example.core.designsystem.MyatoBrandOrange
import com.example.core.designsystem.MyatoBrandOrangeVibrant
import com.example.core.designsystem.MyatoCompactBrandBadge
import com.example.core.designsystem.MyatoOfficialBrandLockup
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 1. Splash Screen: Deep black (#050505) background, subtle orange aura,
 * official MYATO MUSIC logo with animated equalizer bars, crisp entrance, fast handoff.
 */
@Composable
fun SplashScreen(
    onFinished: () -> Unit,
) {
    val scale = remember { Animatable(0.88f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        val scaleJob = launch {
            scale.animateTo(
                1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            )
        }
        val alphaJob = launch {
            alpha.animateTo(1f, animationSpec = tween(durationMillis = 280))
        }
        scaleJob.join()
        alphaJob.join()
        delay(550L)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        MyatoBrandOrange.copy(alpha = 0.14f),
                        MyatoBrandOrangeVibrant.copy(alpha = 0.04f),
                        MyatoBrandBlackSecondary.copy(alpha = 0.45f),
                        MyatoBrandBlack,
                    ),
                ),
            )
            .background(MyatoBrandBlack.copy(alpha = 0.35f))
            .safeDrawingPadding()
            .testTag("splash_screen"),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .scale(scale.value)
                .alpha(alpha.value)
                .padding(32.dp),
            contentAlignment = Alignment.Center,
        ) {
            MyatoOfficialBrandLockup(
                symbolSize = 128.dp,
                animateBars = true,
                showGlow = true,
                useContrastCardOnLightTheme = false,
            )
        }
    }
}

/**
 * 2. Onboarding Screen (Apresentação inicial)
 */
@Composable
fun OnboardingScreen(
    onContinue: () -> Unit,
) {
    val colors = LocalMyatoColors.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(colors.background, colors.altBackground, colors.background)
                )
            )
            .safeDrawingPadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(12.dp))
                Image(
                    painter = painterResource(R.drawable.img_onboarding_wave),
                    contentDescription = "Ilustração sonora MYATO MUSIC",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(24.dp)),
                )
                Spacer(Modifier.height(22.dp))
                MyatoCompactBrandBadge(
                    symbolSize = 40.dp,
                    animateBars = true,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.app_slogan),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(20.dp))

                FeatureHighlightCard(
                    icon = Icons.Rounded.Headphones,
                    title = "Motor Media3 + Equalizador Real",
                    subtitle = "Reprodução gapless, múltiplas filas independentes e perfis acústicos por fone ou Bluetooth.",
                )
                Spacer(Modifier.height(10.dp))
                FeatureHighlightCard(
                    icon = Icons.Rounded.Lock,
                    title = "100% Offline & Privado",
                    subtitle = "Zero streaming, zero publicidade, zero envio de arquivos. Suas músicas permanecem no aparelho.",
                )
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("onboarding_continue_button"),
                shape = RoundedCornerShape(27.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    contentColor = colors.onPrimary,
                ),
            ) {
                Text(
                    text = "Começar Experiência",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun FeatureHighlightCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
) {
    val colors = LocalMyatoColors.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = colors.card),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(colors.primary.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = colors.primary)
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                Spacer(Modifier.height(2.dp))
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
            }
        }
    }
}

/**
 * 3. Permissions Screen (Permissões de áudio, notificações e pastas SAF)
 */
@Composable
fun PermissionsScreen(
    onPermissionsGranted: () -> Unit,
    onImportSafFolder: (android.net.Uri) -> Unit,
) {
    val colors = LocalMyatoColors.current
    val context = LocalContext.current

    val audioPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    var audioGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, audioPermission) == PermissionChecker.PERMISSION_GRANTED
        )
    }
    var notifGranted by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PermissionChecker.PERMISSION_GRANTED
        )
    }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        audioGranted = results[audioPermission] == true ||
            ContextCompat.checkSelfPermission(context, audioPermission) == PermissionChecker.PERMISSION_GRANTED
        notifGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            results[Manifest.permission.POST_NOTIFICATIONS] == true
        } else true
        if (audioGranted) onPermissionsGranted()
    }

    val safFolderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
            }
            onImportSafFolder(uri)
            onPermissionsGranted()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .safeDrawingPadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    text = "Permissões de Biblioteca",
                    style = MaterialTheme.typography.headlineLarge,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Para localizar suas músicas locais e exibir controles de reprodução em segundo plano, o MYATO MUSIC solicita apenas as permissões essenciais.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.textSecondary,
                )
                Spacer(Modifier.height(24.dp))

                PermissionStatusCard(
                    icon = Icons.Rounded.VerifiedUser,
                    title = "Acesso às músicas locais",
                    description = "Lê arquivos MP3, FLAC, WAV, OGG, M4A, OPUS e AAC no dispositivo.",
                    granted = audioGranted,
                )
                Spacer(Modifier.height(12.dp))
                PermissionStatusCard(
                    icon = Icons.Rounded.NotificationsActive,
                    title = "Notificações de reprodução",
                    description = "Exibe capa, controles e ações rápidas na barra de notificações.",
                    granted = notifGranted,
                )
            }

            Column {
                Button(
                    onClick = {
                        val req = buildList {
                            add(audioPermission)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                add(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }.toTypedArray()
                        permLauncher.launch(req)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("grant_permissions_button"),
                    shape = RoundedCornerShape(27.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.onPrimary),
                ) {
                    Text(
                        text = if (audioGranted) "Escanear e Entrar" else "Conceder Permissões",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { safFolderLauncher.launch(null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("saf_folder_button"),
                    shape = RoundedCornerShape(25.dp),
                ) {
                    Icon(Icons.Rounded.FolderOpen, contentDescription = null, tint = colors.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("Selecionar Pasta via SAF", color = colors.textPrimary)
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = onPermissionsGranted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("skip_permissions_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = colors.textSecondary,
                    ),
                ) {
                    Text("Continuar agora")
                }
            }
        }
    }
}

@Composable
private fun PermissionStatusCard(
    icon: ImageVector,
    title: String,
    description: String,
    granted: Boolean,
) {
    val colors = LocalMyatoColors.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = colors.card),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = if (granted) Color(0xFF2ECC71) else colors.primary)
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                Text(description, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (granted) "Ativo" else "Pendente",
                style = MaterialTheme.typography.labelMedium,
                color = if (granted) Color(0xFF2ECC71) else colors.primary,
            )
        }
    }
}

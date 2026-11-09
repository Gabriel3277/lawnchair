package app.lawnchair.ui.welcome

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.ViewGroup
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlinx.coroutines.delay

private const val PREFS = "lawnchair_welcome"
private const val KEY_SEEN = "seen"

/** Mostra a tela de boas-vindas só na primeira abertura. */
fun showWelcomeIfFirstRun(activity: Activity) {
    val sp = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    if (sp.getBoolean(KEY_SEEN, false)) return
    val view = ComposeView(activity)
    view.setContent {
        WelcomeScreen(
            onContinue = {
                sp.edit().putBoolean(KEY_SEEN, true).apply()
                (view.parent as? ViewGroup)?.removeView(view)
            },
        )
    }
    activity.addContentView(
        view,
        ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
    )
}

private class Pal(val greet: String, val bg: Long, val cookie: Long, val wave: Long, val light: Long, val ink: Long)

private val palettes = listOf(
    Pal("Olá", 0xFF12172B, 0xFF5B7BDB, 0xFF3C4F8F, 0xFFE4E9FF, 0xFF33468F),
    Pal("你好呀", 0xFF0F1B26, 0xFF3B8FB5, 0xFF2F6D8C, 0xFFBFEAFF, 0xFF1F5670),
    Pal("Hello", 0xFF10201A, 0xFF5DB88A, 0xFF3A7D5F, 0xFFCFF5E0, 0xFF25603F),
    Pal("Bonjour", 0xFF26180F, 0xFFE0925A, 0xFF96603A, 0xFFFFE2CC, 0xFF7A4A25),
    Pal("こんにちは", 0xFF2A121C, 0xFFE07A9A, 0xFF964A62, 0xFFFFD9E5, 0xFF7A3550),
    Pal("Привет", 0xFF1F1428, 0xFFB67BDB, 0xFF7A4F96, 0xFFF1DAFF, 0xFF5E3A7A),
    Pal("Hola", 0xFF1F2410, 0xFFB5C95A, 0xFF76853A, 0xFFF0F8CC, 0xFF566320),
    Pal("Hallo", 0xFF12172B, 0xFF5B7BDB, 0xFF3C4F8F, 0xFFE4E9FF, 0xFF33468F),
)

@Composable
fun WelcomeScreen(onContinue: () -> Unit) {
    val context = LocalContext.current
    var idx by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(2600)
            idx = (idx + 1) % palettes.size
        }
    }
    val p = palettes[idx]
    val bg by animateColorAsState(Color(p.bg), tween(1200), label = "bg")
    val cookie by animateColorAsState(Color(p.cookie), tween(1200), label = "cookie")
    val wave by animateColorAsState(Color(p.wave), tween(1200), label = "wave")
    val light by animateColorAsState(Color(p.light), tween(1200), label = "light")
    val ink by animateColorAsState(Color(p.ink), tween(1200), label = "ink")

    // 1 unidade = 1 segundo
    val t = rememberInfiniteTransition(label = "t").animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(tween(1_000_000, easing = LinearEasing), RepeatMode.Restart),
        label = "time",
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(bg)
            .pointerInput(Unit) { detectTapGestures { } },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val time = t.value
            val w = size.width
            val h = size.height

            // onda
            val wp = Path()
            wp.moveTo(0f, h)
            val steps = 40
            for (i in 0..steps) {
                val nx = i / steps.toFloat()
                val y = h * 0.54f +
                    h * 0.09f * sin(nx * PI.toFloat() * 1.1f + time * 0.7f) +
                    h * 0.028f * sin(nx * 4.4f - time * 1.1f) +
                    nx * h * 0.015f
                wp.lineTo(nx * w, y)
            }
            wp.lineTo(w, h)
            wp.close()
            drawPath(wp, wave)

            // forma "cookie"
            val cx = w * 0.69f
            val cy = h * 0.27f
            val r0 = max(w * 0.32f, 1f)
            val amp = 0.075f + 0.025f * sin(time * 0.9f)
            val rot = time * 0.35f
            val n = 140
            val cp = Path()
            for (k in 0 until n) {
                val a = k / n.toFloat() * 2f * PI.toFloat()
                val r = r0 * (1f + amp * cos(9f * a + rot * 3f) + 0.02f * sin(5f * a - time * 1.3f))
                val x = cx + r * cos(a + rot)
                val y = cy + r * sin(a + rot)
                if (k == 0) cp.moveTo(x, y) else cp.lineTo(x, y)
            }
            cp.close()
            drawPath(cp, cookie)
        }

        // topo
        Row(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
        ) {
            Chip { Ico(Color.White, 20.dp) { drawHandset(Color.White) } }
            Chip { Box(Modifier.size(10.dp).clip(CircleShape).background(Color.White)) }
        }

        // saudação
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 150.dp)
                .navigationBarsPadding(),
        ) {
            AnimatedContent(
                targetState = idx,
                transitionSpec = {
                    (fadeIn(tween(350)) + slideInVertically(tween(350)) { it / 3 }) togetherWith
                        (fadeOut(tween(250)) + slideOutVertically(tween(250)) { -it / 3 })
                },
                label = "greet",
            ) { i ->
                Text(
                    text = palettes[i].greet,
                    color = Color.White,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-1).sp,
                )
            }
        }

        // botões
        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
        ) {
            Pill(
                modifier = Modifier.weight(0.36f),
                bg = Color.Black.copy(alpha = 0.4f),
                onClick = {
                    runCatching {
                        context.startActivity(
                            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    }
                },
            ) { Ico(Color.White, 30.dp) { drawAccessibility(Color.White) } }
            Pill(
                modifier = Modifier.weight(0.64f),
                bg = light,
                onClick = onContinue,
            ) { Ico(ink, 30.dp) { drawArrow(ink) } }
        }
    }
}

@Composable
private fun Chip(content: @Composable () -> Unit) {
    Box(
        Modifier.size(44.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.28f)),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun Pill(modifier: Modifier, bg: Color, onClick: () -> Unit, content: @Composable () -> Unit) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val sc by animateFloatAsState(
        if (pressed) 0.94f else 1f,
        spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium),
        label = "sc",
    )
    val corner by animateDpAsState(if (pressed) 24.dp else 36.dp, label = "corner")
    Box(
        modifier
            .height(72.dp)
            .graphicsLayer { scaleX = sc; scaleY = sc }
            .clip(RoundedCornerShape(corner))
            .background(bg)
            .clickable(interactionSource = src, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun Ico(color: Color, size: Dp, draw: DrawScope.() -> Unit) {
    Canvas(Modifier.size(size)) {
        scale(this.size.width / 24f, pivot = Offset.Zero) { draw() }
    }
}

private fun DrawScope.drawArrow(c: Color) {
    val s = Stroke(width = 2.2f, cap = StrokeCap.Round)
    drawLine(c, Offset(4f, 12f), Offset(20f, 12f), 2.2f, StrokeCap.Round)
    drawLine(c, Offset(14f, 6f), Offset(20f, 12f), 2.2f, StrokeCap.Round)
    drawLine(c, Offset(20f, 12f), Offset(14f, 18f), 2.2f, StrokeCap.Round)
}

private fun DrawScope.drawAccessibility(c: Color) {
    drawCircle(c, 1.7f, Offset(12f, 4.5f))
    drawLine(c, Offset(4.5f, 8.5f), Offset(19.5f, 8.5f), 2.3f, StrokeCap.Round)
    drawLine(c, Offset(12f, 8.5f), Offset(12f, 13.5f), 2.3f, StrokeCap.Round)
    drawLine(c, Offset(12f, 13.5f), Offset(8.5f, 19.5f), 2.3f, StrokeCap.Round)
    drawLine(c, Offset(12f, 13.5f), Offset(15.5f, 19.5f), 2.3f, StrokeCap.Round)
}

private val handset: Path by lazy {
    PathParser().parsePathString(
        "M6.6 10.8a15 15 0 006.6 6.6l2.2-2.2a1 1 0 011-.25 11 11 0 003.5.55 1 1 0 011 1V20a1 1 0 01-1 1A17 17 0 013 4a1 1 0 011-1h3.5a1 1 0 011 1c0 1.2.2 2.4.55 3.5a1 1 0 01-.25 1z",
    ).toPath()
}

private fun DrawScope.drawHandset(c: Color) {
    drawPath(handset, c)
}

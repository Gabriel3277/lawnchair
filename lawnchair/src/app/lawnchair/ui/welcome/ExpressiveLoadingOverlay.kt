package app.lawnchair.ui.welcome

import android.graphics.Color as AndroidColor
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.view.ViewGroup
import android.view.WindowManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlinx.coroutines.delay

private class MorphShape(val lobes: Int, val depth: Float)

private val morphShapes = listOf(
    MorphShape(7, 0.09f),
    MorphShape(8, 0.16f),
    MorphShape(4, 0.13f),
    MorphShape(5, 0.19f),
    MorphShape(2, 0.20f),
)

private fun easeInOut(t: Float): Float {
    val x = t.coerceIn(0f, 1f)
    return if (x < 0.5f) 2f * x * x else 1f - (-2f * x + 2f) * (-2f * x + 2f) / 2f
}

/**
 * Indicador de carregamento que gira trocando de forma (estilo Material 3 Expressive).
 */
@Composable
fun ExpressiveLoadingIndicator(
    modifier: Modifier = Modifier,
    containerColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primaryContainer,
    shapeColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
) {
    var timeSec by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val start = withFrameNanos { it }
        while (true) {
            withFrameNanos { now ->
                timeSec = (now - start) / 1_000_000_000f
            }
        }
    }

    Canvas(modifier = modifier.size(120.dp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        drawCircle(color = containerColor, radius = size.minDimension / 2f, center = center)

        val step = timeSec / 0.9f
        val index = floor(step).toInt()
        val from = morphShapes[index % morphShapes.size]
        val to = morphShapes[(index + 1) % morphShapes.size]
        val fraction = step - floor(step)
        val mix = if (fraction < 0.35f) 0f else easeInOut((fraction - 0.35f) / 0.65f)
        val rotation = timeSec * 1.4f

        val baseRadius = size.minDimension * 0.34f / 1.1f
        val points = 120
        val path = Path()
        for (i in 0 until points) {
            val theta = (2.0 * PI * i / points).toFloat()
            val ra = 1f + from.depth * cos(from.lobes * theta)
            val rb = 1f + to.depth * cos(to.lobes * theta)
            val r = baseRadius * ((1f - mix) * ra + mix * rb)
            val x = center.x + r * cos(theta + rotation)
            val y = center.y + r * sin(theta + rotation)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        drawPath(path = path, color = shapeColor)
    }
}

/**
 * Tela cheia com blur no fundo e o indicador de carregamento no centro.
 * Some sozinha depois de [durationMs] e chama [onFinished].
 */
@Composable
fun ExpressiveLoadingOverlay(
    durationMs: Long = 20_000L,
    onFinished: () -> Unit,
) {
    LaunchedEffect(Unit) {
        delay(durationMs)
        onFinished()
    }

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
        ),
    ) {
        val view = LocalView.current
        SideEffect {
            val window = (view.parent as? DialogWindowProvider)?.window ?: return@SideEffect
            window.setBackgroundDrawable(ColorDrawable(AndroidColor.TRANSPARENT))
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            window.setDimAmount(0.3f)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                val params = window.attributes
                params.blurBehindRadius = 100
                window.attributes = params
            }
        }
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            ExpressiveLoadingIndicator()
        }
    }
}

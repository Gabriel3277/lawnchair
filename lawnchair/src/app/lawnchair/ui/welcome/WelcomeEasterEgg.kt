package app.lawnchair.ui.welcome

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView

/** Acha a Activity a partir de qualquer Context (útil dentro de telas Compose). */
fun Context.findActivityOrNull(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

/**
 * Mostra a tela de boas-vindas e depois o carregamento (20s) agora,
 * mesmo que ela já tenha sido vista. Não mexe na preferência "seen".
 */
fun showWelcomeNow(activity: Activity) {
    val view = ComposeView(activity)
    view.setContent {
        var loading by remember { mutableStateOf(false) }
        if (loading) {
            ExpressiveLoadingOverlay(
                durationMs = 20_000L,
                onFinished = {
                    (view.parent as? ViewGroup)?.removeView(view)
                },
            )
        } else {
            WelcomeScreen(onContinue = { loading = true })
        }
    }
    activity.addContentView(
        view,
        ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
    )
}

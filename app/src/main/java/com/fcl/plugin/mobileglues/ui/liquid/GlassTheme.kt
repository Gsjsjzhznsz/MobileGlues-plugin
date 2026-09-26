package com.fcl.plugin.mobileglues.ui.liquid

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fcl.plugin.mobileglues.settings.ThemeMode
import com.fcl.plugin.mobileglues.ui.AppController
import com.fcl.plugin.mobileglues.ui.LocalMotionSpeed
import com.fcl.plugin.mobileglues.ui.LocalMotionStagger

/**
 * Android 12 以下无系统取色板，回退品牌蓝作为 Monet 种子色（避免紫罗兰默认色）。
 */
val FALLBACK_KEY_COLOR = Color(0xFF3482FF)

/**
 * 主题与液态玻璃的总包装（BandQQ BandQQTheme 的移植形态，皮肤无关）：
 *
 * - 从 [AppController.pluginConfig] 读主题模式与玻璃三开关，落 CompositionLocal，
 *   底栏/顶栏在 draw 阶段直接读，不经过任何皮肤组件；
 * - 深色判定一处做（模式强制 > 系统跟随），两套皮肤吃同一个结果，
 *   状态栏/导航栏图标明暗随之联动；
 * - 皮肤的着色系统（MiuixTheme / MaterialTheme）仍由各皮肤自己搭，
 *   这里只传判定结果，不抢着色权。
 */
@Composable
fun MgGlassTheme(
    controller: AppController,
    content: @Composable (themeMode: ThemeMode, keyColor: Int, darkTheme: Boolean) -> Unit,
) {
    val themeMode by controller.pluginConfig.themeMode.collectAsStateWithLifecycle()
    val keyColor by controller.pluginConfig.keyColor.collectAsStateWithLifecycle()
    val enableBlur by controller.pluginConfig.enableBlur.collectAsStateWithLifecycle()
    val floatingBar by controller.pluginConfig.floatingBottomBar.collectAsStateWithLifecycle()
    val glassBar by controller.pluginConfig.floatingBottomBarGlass.collectAsStateWithLifecycle()
    val pageScale by controller.pluginConfig.pageScale.collectAsStateWithLifecycle()
    val motionSpeed by controller.pluginConfig.motionSpeed.collectAsStateWithLifecycle()
    val motionStagger by controller.pluginConfig.motionStagger.collectAsStateWithLifecycle()

    val darkTheme = themeMode.isDark || (themeMode.isSystem && isSystemInDarkTheme())

    // 界面缩放：按比例缩放全局密度与字宽（BandQQ BandQQTheme / KernelSU PageScale
    // 同款 80% ~ 110%）。挂在两套皮肤共同的外壳上，一个实现、处处生效。
    val base = LocalDensity.current
    val scaledDensity = remember(base, pageScale) {
        Density(base.density * pageScale, base.fontScale * pageScale)
    }

    // 状态栏/导航栏图标明暗跟随主题模式，而不是只跟随系统。
    val activity = LocalContext.current as? Activity
    LaunchedEffect(darkTheme) {
        val window = activity?.window ?: return@LaunchedEffect
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = !darkTheme
            isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(
        LocalGlassDarkTheme provides darkTheme,
        LocalEnableBlur provides enableBlur,
        LocalEnableFloatingBottomBar provides floatingBar,
        LocalEnableFloatingBottomBarGlass provides (glassBar && Build.VERSION.SDK_INT >= 33),
        LocalDensity provides scaledDensity,
        LocalMotionSpeed provides motionSpeed,
        LocalMotionStagger provides motionStagger,
    ) {
        content(themeMode, keyColor, darkTheme)
    }
}

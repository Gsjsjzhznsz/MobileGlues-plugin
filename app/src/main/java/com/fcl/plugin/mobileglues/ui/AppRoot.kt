package com.fcl.plugin.mobileglues.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fcl.plugin.mobileglues.settings.UiStyle
import com.fcl.plugin.mobileglues.ui.liquid.MgGlassTheme
import com.fcl.plugin.mobileglues.ui.material.MaterialApp
import com.fcl.plugin.mobileglues.ui.miuix.MiuixApp

/**
 * 根：按「界面风格」选择皮肤。两套皮肤是完整独立的两套界面（不是换肤），
 * 但它们吃进同一个 [AppController]，所以操作逻辑严格一致。
 */
@Composable
fun MobileGluesApp(controller: AppController) {
    val style by controller.pluginConfig.uiStyle.collectAsStateWithLifecycle()

    // 跑分期间不许自动熄屏。一次跑分要到一分多钟，而全程没有任何触摸——正是系统认定
    // 「用户走开了」的样子。屏幕一灭，测的就不再是游戏里那块 GPU 的状态：合成停了、
    // 频率策略换了，结果既不可比也不可信，而用户回来只看到一份莫名其妙的排名。
    //
    // 放在这一层而不是各皮肤的对话框里：跑分状态属于 controller，两套皮肤共用同一份，
    // 这件事就不该有两个实现（也就不会有一套忘了做）。
    val benchState by controller.benchState.collectAsStateWithLifecycle()
    val view = LocalView.current
    val benchRunning = benchState is AppController.BenchState.Running
    DisposableEffect(view, benchRunning) {
        // View 自己的开关，不碰 Activity 的 window flag，也就不需要为「谁是 Activity」
        // 做上下文回溯；视图不可见时系统自动失效，兜住了跑分中途切走的情形。
        view.keepScreenOn = benchRunning
        onDispose { view.keepScreenOn = false }
    }

    // 主题与液态玻璃的 CompositionLocal 在这一层提供：两套皮肤吃同一份主题模式
    // 与玻璃开关，底栏/顶栏的玻璃效果不经过皮肤组件就能取到状态。
    MgGlassTheme(controller) { themeMode, keyColor, darkTheme ->
        when (style) {
            UiStyle.Material -> MaterialApp(controller, themeMode, keyColor, darkTheme)
            UiStyle.Miuix -> MiuixApp(controller, themeMode, keyColor)
        }
    }
}

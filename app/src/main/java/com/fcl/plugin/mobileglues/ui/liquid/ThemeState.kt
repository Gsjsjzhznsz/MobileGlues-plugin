package com.fcl.plugin.mobileglues.ui.liquid

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * 液态玻璃主题的 CompositionLocal 集合（BandQQ/KernelSU 同款结构）。
 *
 * 这些值由 AppRoot 的主题包装层提供，供底栏/顶栏在 draw 阶段读取；
 * 全部有安全默认值（关闭），所以任何皮肤漏接 Provider 时也只是回退实色，不会崩。
 */

/** 当前是否深色主题（液态玻璃组件在高光/阴影上按明暗取不同系数） */
val LocalGlassDarkTheme = staticCompositionLocalOf { false }

@Composable
fun isGlassDarkTheme(): Boolean = LocalGlassDarkTheme.current

/** 顶栏和底栏的模糊效果（Android 13+ 生效；关闭或设备不支持时回退实色） */
val LocalEnableBlur = staticCompositionLocalOf { false }

/** 使用 Apple 风格的悬浮底栏（替代标准 NavigationBar） */
val LocalEnableFloatingBottomBar = staticCompositionLocalOf { false }

/** 悬浮底栏的液态玻璃效果（悬浮底栏的二级选项，Android 13+ 生效） */
val LocalEnableFloatingBottomBarGlass = staticCompositionLocalOf { false }

package com.fcl.plugin.mobileglues.ui

import androidx.compose.runtime.Composable
import com.fcl.plugin.mobileglues.ui.liquid.LocalGlassDarkTheme

/**
 * 深色判定的唯一出口：MgGlassTheme 已经按主题模式算好（模式强制 > 系统跟随）
 * 并通过 [LocalGlassDarkTheme] 提供到组合树里，底栏/顶栏的 draw 阶段直接读，
 * 不再各自判一遍。AppRoot 把整个界面都包在 MgGlassTheme 里，所以这里读到的
 * 永远是"生效值"。
 */
@Composable
fun isInDarkTheme(): Boolean = LocalGlassDarkTheme.current

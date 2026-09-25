package com.fcl.plugin.mobileglues.ui.miuix

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fcl.plugin.mobileglues.R
import com.fcl.plugin.mobileglues.settings.ThemeMode
import com.fcl.plugin.mobileglues.ui.AppController
import com.fcl.plugin.mobileglues.ui.AppSubPage
import com.fcl.plugin.mobileglues.ui.AppTab
import com.fcl.plugin.mobileglues.ui.component.FloatingBottomBar
import com.fcl.plugin.mobileglues.ui.component.FloatingBottomBarItem
import com.fcl.plugin.mobileglues.ui.liquid.FALLBACK_KEY_COLOR
import com.fcl.plugin.mobileglues.ui.liquid.LocalEnableBlur
import com.fcl.plugin.mobileglues.ui.liquid.LocalEnableFloatingBottomBar
import com.fcl.plugin.mobileglues.ui.liquid.LocalEnableFloatingBottomBarGlass
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateBottomPadding
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.basic.NavigationRail
import top.yukonga.miuix.kmp.basic.NavigationRailItem
import com.fcl.plugin.mobileglues.ui.Responsive
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SnackbarHost
import top.yukonga.miuix.kmp.basic.SnackbarDuration
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import top.yukonga.miuix.kmp.basic.SnackbarResult
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import com.fcl.plugin.mobileglues.ui.BlurredBar
import com.fcl.plugin.mobileglues.ui.rememberBlurBackdrop
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color

/**
 * Miuix 皮肤的外壳。
 *
 * 结构与 MD3 皮肤一一对应，用的却是完全不同的一套组件：这里是两套界面，不是换个配色。
 * 唯一共享的是 [AppController]——所有操作逻辑因此只有一份。
 */
@Composable
fun MiuixApp(controller: AppController, themeMode: ThemeMode, keyColor: Int) {
    val dark = themeMode.isDark || (themeMode.isSystem && isSystemInDarkTheme())

    // BandQQ 同款：主题模式（六档）映射到 miuix ThemeController 的 ColorSchemeMode，
    // 动态取色三档走系统 Monet 色板；旧 colors= 参数的重载永远停在浅色，不能再用。
    val schemeMode = when (themeMode) {
        ThemeMode.System -> ColorSchemeMode.System
        ThemeMode.Light -> ColorSchemeMode.Light
        ThemeMode.Dark -> ColorSchemeMode.Dark
        ThemeMode.MonetSystem -> ColorSchemeMode.MonetSystem
        ThemeMode.MonetLight -> ColorSchemeMode.MonetLight
        ThemeMode.MonetDark -> ColorSchemeMode.MonetDark
    }
    val seedColor: Color? = when {
        keyColor != 0 -> Color(keyColor)
        themeMode.isMonet && android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S -> FALLBACK_KEY_COLOR
        else -> null
    }
    val themeController = remember(schemeMode, dark, seedColor) {
        ThemeController(colorSchemeMode = schemeMode, keyColor = seedColor, isDark = dark)
    }
    MiuixTheme(controller = themeController) {
        val tab by controller.tab.collectAsStateWithLifecycle()
        val subPage by controller.subPage.collectAsStateWithLifecycle()
        val snackbarHostState = remember { SnackbarHostState() }

        LaunchedEffect(controller) {
            controller.snackbar.collect { snackbarHostState.showSnackbar(it.toString()) }
        }

        // 换了 GLES 驱动，手上那份排序是在旧驱动上量的。用 snackbar 而不是对话框：
        // 这只是句提醒，用户正忙着调设置，不该被拦下来。
        val outdatedMessage = stringResource(R.string.md_bench_outdated)
        val outdatedAction = stringResource(R.string.md_bench_outdated_action)
        LaunchedEffect(controller) {
            controller.benchOutdated.collect {
                val result = snackbarHostState.showSnackbar(
                    message = outdatedMessage,
                    actionLabel = outdatedAction,
                    duration = SnackbarDuration.Long,
                )
                if (result == SnackbarResult.ActionPerformed) {
                    controller.runMultidrawBench(AppController.BenchTarget.AllEntries)
                }
            }
        }

        BackHandler(enabled = subPage != null) { controller.navigateBack() }

        // 垂直方向紧张（通常是手机横屏）时导航让到侧边，理由与 Material 皮肤相同：
        // 底栏吃掉的是横屏下最稀缺的高度。判断的是高度而不是朝向，见 Responsive。
        val heightCompact = Responsive.isHeightCompact()
        val enableBlur = LocalEnableBlur.current
        val floatingBar = LocalEnableFloatingBottomBar.current
        val glassBar = LocalEnableFloatingBottomBarGlass.current
        // BandQQ/KernelSU 同款双采集层：
        // - blurBackdrop 供标准底栏的 textureBlur（enableBlur 关闭或设备不支持时为 null，回退实色）；
        // - glassBackdrop 供悬浮底栏液态玻璃，先垫 surface 底色再画内容，防止采样透明像素发黑。
        val blurBackdrop = rememberBlurBackdrop(enableBlur)
        val glassBackdrop = rememberLayerBackdrop {
            drawRect(MiuixTheme.colorScheme.surface)
            drawContent()
        }
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            // Miuix 的语义与 MD3 相反：页面是 surface（深色下是纯黑），卡片才是 surfaceContainer。
            // 把页面画成 background 会和卡片同色，分组就看不见了。
            containerColor = MiuixTheme.colorScheme.surface,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                AnimatedVisibility(
                    visible = subPage == null && !heightCompact,
                    enter = slideInVertically { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut(),
                ) {
                    MiuixBottomBar(
                        current = tab,
                        onSelect = controller::navigateTab,
                        backdrop = glassBackdrop,
                        blurBackdrop = blurBackdrop,
                        floatingBar = floatingBar,
                        glassBar = glassBar,
                    )
                }
            },
        ) { innerPadding ->
            // 对话框宿主要在 Scaffold 之内：Miuix 的弹窗渲染进 Scaffold 提供的 popup host。
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        when {
                            floatingBar && glassBar -> Modifier.layerBackdrop(glassBackdrop)
                            !floatingBar && blurBackdrop != null -> Modifier.layerBackdrop(blurBackdrop)
                            else -> Modifier
                        }
                    )
            ) {
                Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                    AnimatedVisibility(
                        visible = subPage == null && heightCompact,
                        enter = slideInHorizontally { -it } + fadeIn(),
                        exit = slideOutHorizontally { -it } + fadeOut(),
                    ) {
                        MiuixNavigationRail(current = tab, onSelect = controller::navigateTab)
                    }
                // 每页的滚动位置各自存一份：从子页面退回来时，列表还停在原处。
                val pageState = rememberSaveableStateHolder()
                AnimatedContent(
                    targetState = subPage ?: tab,
                    transitionSpec = { miuixPageTransition(initialState, targetState) },
                    modifier = Modifier.fillMaxSize(),
                    label = "page",
                ) { destination ->
                    pageState.SaveableStateProvider(destination) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            when (destination) {
                                AppTab.Home -> MiuixHomePage(controller)
                                AppTab.Settings -> MiuixSettingsPage(controller)
                                AppTab.Info -> MiuixInfoPage(controller)
                                AppSubPage.GlInfo -> MiuixGlInfoPage(controller)
                                AppSubPage.Privacy -> MiuixPrivacyPage(controller)
                                AppSubPage.ThirdParty -> MiuixThirdPartyPage(controller)
                                AppSubPage.Theme -> MiuixThemePage(controller)
                            }
                        }
                    }
                }
                }
                MiuixDialogHost(controller)
                // 跑分可以从主页的提示、设置页的按钮、切驱动后的 snackbar 三处发起，
                // 对话框因此挂在这一层，而不是某一页里。
                MiuixMultidrawBenchDialogs(controller)
            }
        }
    }
}

@Composable
private fun MiuixNavigationBar(
    current: AppTab,
    onSelect: (AppTab) -> Unit,
    containerColor: Color = MiuixTheme.colorScheme.surface,
) {
    NavigationBar(color = containerColor) {
        NavigationBarItem(
            selected = current == AppTab.Home,
            onClick = { onSelect(AppTab.Home) },
            icon = rememberVectorIcon(R.drawable.ic_home),
            label = stringResource(R.string.nav_home),
        )
        NavigationBarItem(
            selected = current == AppTab.Settings,
            onClick = { onSelect(AppTab.Settings) },
            icon = rememberVectorIcon(R.drawable.ic_settings),
            label = stringResource(R.string.nav_settings),
        )
        NavigationBarItem(
            selected = current == AppTab.Info,
            onClick = { onSelect(AppTab.Info) },
            icon = rememberVectorIcon(R.drawable.ic_info),
            label = stringResource(R.string.nav_info),
        )
    }
}

/**
 * 底栏双形态（BandQQ/KernelSU 同款语义）：
 * - 非悬浮：标准 NavigationBar；
 * - 悬浮：Apple 风格 FloatingBottomBar（液态玻璃 + 阻尼拖拽指示 pill + 交互高光）。
 *
 * 悬浮栏底距 = 导航栏 inset + 8dp（无手势导航设备回退 28dp），否则底栏贴到屏幕底边。
 */
@Composable
private fun MiuixBottomBar(
    current: AppTab,
    onSelect: (AppTab) -> Unit,
    backdrop: Backdrop,
    blurBackdrop: LayerBackdrop?,
    floatingBar: Boolean,
    glassBar: Boolean,
) {
    if (!floatingBar) {
        // BandQQ 同款：模糊开启时标准底栏包进 textureBlur，本体透明让模糊层透出来。
        if (blurBackdrop != null) {
            BlurredBar(backdrop = blurBackdrop) {
                MiuixNavigationBar(
                    current = current,
                    onSelect = onSelect,
                    containerColor = Color.Transparent,
                )
            }
        } else {
            MiuixNavigationBar(current = current, onSelect = onSelect)
        }
        return
    }
    val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        .let { inset -> if (inset != 0.dp) 8.dp + inset else 28.dp }
    FloatingBottomBar(
        modifier = Modifier
            .pointerInput(Unit) { detectTapGestures { } }
            .padding(start = 28.dp, end = 28.dp, bottom = bottomPadding),
        selectedIndex = current.ordinal,
        onSelected = { index -> onSelect(AppTab.entries[index]) },
        backdrop = backdrop,
        tabsCount = AppTab.entries.size,
        isBlurEnabled = glassBar,
    ) { activateTab ->
        AppTab.entries.forEach { tab ->
            FloatingBottomBarItem(
                selected = current == tab,
                onClick = { activateTab(tab.ordinal) },
                // weight 子项在 IntrinsicSize.Min 的 intrinsic 测量中宽度为 0，
                // 必须 minWidth 兜底，否则整个底栏塌缩成一个颗粒（KSU 同款写法）
                modifier = Modifier.defaultMinSize(minWidth = 76.dp),
            ) {
                Icon(
                    imageVector = rememberVectorIcon(tab.iconRes()),
                    contentDescription = stringResource(tab.labelRes()),
                )
                Text(
                    text = stringResource(tab.labelRes()),
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Visible,
                )
            }
        }
    }
}

private fun AppTab.iconRes(): Int = when (this) {
    AppTab.Home -> R.drawable.ic_home
    AppTab.Settings -> R.drawable.ic_settings
    AppTab.Info -> R.drawable.ic_info
}

private fun AppTab.labelRes(): Int = when (this) {
    AppTab.Home -> R.string.nav_home
    AppTab.Settings -> R.string.nav_settings
    AppTab.Info -> R.string.nav_info
}

/** 底栏的侧边形态，条目与顺序同一份语义——两种形态永远不会各说各话。 */
@Composable
private fun MiuixNavigationRail(current: AppTab, onSelect: (AppTab) -> Unit) {
    NavigationRail {
        // 同 Material 侧的理由：底栏条目水平居中，侧边形态就该垂直居中。
        Spacer(Modifier.weight(1f))
        NavigationRailItem(
            selected = current == AppTab.Home,
            onClick = { onSelect(AppTab.Home) },
            icon = rememberVectorIcon(R.drawable.ic_home),
            label = stringResource(R.string.nav_home),
        )
        NavigationRailItem(
            selected = current == AppTab.Settings,
            onClick = { onSelect(AppTab.Settings) },
            icon = rememberVectorIcon(R.drawable.ic_settings),
            label = stringResource(R.string.nav_settings),
        )
        NavigationRailItem(
            selected = current == AppTab.Info,
            onClick = { onSelect(AppTab.Info) },
            icon = rememberVectorIcon(R.drawable.ic_info),
            label = stringResource(R.string.nav_info),
        )
        Spacer(Modifier.weight(1f))
    }
}

/** 与 MD3 皮肤同样的过场语义：同级切页小位移，进出子页面从右侧推入。 */
private fun miuixPageTransition(from: Any, to: Any): ContentTransform {
    val fadeInSpec = tween<Float>(durationMillis = 260, easing = FastOutSlowInEasing)
    val fadeOutSpec = tween<Float>(durationMillis = 180, easing = FastOutSlowInEasing)
    val slide = tween<IntOffset>(durationMillis = 320, easing = FastOutSlowInEasing)

    val (enterFraction, exitFraction) = when {
        to is AppSubPage -> 3 to -10
        from is AppSubPage -> -10 to 3
        from is AppTab && to is AppTab && to.ordinal > from.ordinal -> 6 to -6
        else -> -6 to 6
    }

    return ContentTransform(
        targetContentEnter = slideInHorizontally(slide) { it / enterFraction } + fadeIn(fadeInSpec),
        initialContentExit = slideOutHorizontally(slide) { it / exitFraction } + fadeOut(fadeOutSpec),
        sizeTransform = SizeTransform(clip = false),
    )
}

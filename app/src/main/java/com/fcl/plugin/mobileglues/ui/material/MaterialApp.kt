package com.fcl.plugin.mobileglues.ui.material

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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import com.fcl.plugin.mobileglues.ui.Responsive
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import com.fcl.plugin.mobileglues.R
import com.fcl.plugin.mobileglues.settings.ThemeMode
import com.fcl.plugin.mobileglues.ui.AppController
import com.fcl.plugin.mobileglues.ui.AppSubPage
import com.fcl.plugin.mobileglues.ui.AppTab
import com.fcl.plugin.mobileglues.ui.component.FloatingBottomBar
import com.fcl.plugin.mobileglues.ui.component.FloatingBottomBarItem
import com.fcl.plugin.mobileglues.ui.liquid.LocalEnableBlur
import com.fcl.plugin.mobileglues.ui.liquid.LocalEnableFloatingBottomBar
import com.fcl.plugin.mobileglues.ui.liquid.LocalEnableFloatingBottomBarGlass

/**
 * MD3 皮肤的外壳：底部导航三页 + 子页面，全部对话框挂在这一层。
 *
 * 页面本身不持有任何状态——它们从 [AppController] 读，把操作回调回去，
 * 所以和 Miuix 皮肤看到的是同一份真相。
 */
@Composable
fun MaterialApp(
    controller: AppController,
    themeMode: ThemeMode,
    keyColor: Int,
    darkTheme: Boolean,
) {
    // 动态取色三档在 MD3 侧 = S+ 的系统动态色板；非动态档用基线色板。
    // keyColor 种子在 MD3 侧不落地（compose MD3 不支持直接播种），主题色在 Miuix 皮肤生效。
    MgMaterialTheme(darkTheme = darkTheme, dynamic = themeMode.isMonet) {
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

        // 子页面吃掉返回键；没有子页面时交还系统（退出应用）。
        BackHandler(enabled = subPage != null) { controller.navigateBack() }

        // 垂直方向紧张（通常就是手机横屏）时，导航从底部让到侧边：底栏在横屏吃掉的是
        // 本来就稀缺的高度，而左侧的宽度反而有富余。判断的是高度而不是朝向，理由见
        // Responsive。
        val heightCompact = Responsive.isHeightCompact()
        val enableBlur = LocalEnableBlur.current
        val floatingBar = LocalEnableFloatingBottomBar.current
        val glassBar = LocalEnableFloatingBottomBarGlass.current
        // 悬浮底栏液态玻璃的采集层：先垫 surface 底色再画内容，否则采样到透明像素会发黑。
        // colorScheme 是 @Composable 读取，必须先取值再进 draw lambda。
        val backdropColor = MaterialTheme.colorScheme.background
        val glassBackdrop = rememberLayerBackdrop {
            drawRect(backdropColor)
            drawContent()
        }
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = {
                    AnimatedVisibility(
                        visible = subPage == null && !heightCompact,
                        enter = slideInVertically { it } + fadeIn(),
                        exit = slideOutVertically { it } + fadeOut(),
                    ) {
                        MaterialBottomBar(
                            current = tab,
                            onSelect = controller::navigateTab,
                            backdrop = glassBackdrop,
                            floatingBar = floatingBar,
                            glassBar = glassBar,
                        )
                    }
                },
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .then(if (floatingBar && glassBar) Modifier.layerBackdrop(glassBackdrop) else Modifier),
                ) {
                    AnimatedVisibility(
                        visible = subPage == null && heightCompact,
                        enter = slideInHorizontally { -it } + fadeIn(),
                        exit = slideOutHorizontally { -it } + fadeOut(),
                    ) {
                        MaterialNavigationRail(current = tab, onSelect = controller::navigateTab)
                    }
                    // 每页的滚动位置各自存一份：从子页面退回来时，列表还停在原处。
                    val pageState = rememberSaveableStateHolder()
                    AnimatedContent(
                        targetState = subPage ?: tab,
                        transitionSpec = { pageTransition(initialState, targetState) },
                        modifier = Modifier.fillMaxSize(),
                        label = "page",
                    ) { destination ->
                        pageState.SaveableStateProvider(destination) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                when (destination) {
                                    AppTab.Home -> MaterialHomePage(controller)
                                    AppTab.Settings -> MaterialSettingsPage(controller)
                                    AppTab.Info -> MaterialInfoPage(controller)
                                    AppSubPage.GlInfo -> MaterialGlInfoPage(controller)
                                    AppSubPage.Privacy -> MaterialPrivacyPage(controller)
                                    AppSubPage.ThirdParty -> MaterialThirdPartyPage(controller)
                                    AppSubPage.Theme -> MaterialThemePage(controller)
                                }
                            }
                        }
                    }
                }
            }
        }

        MaterialDialogHost(controller)
        // 跑分可以从主页的提示、设置页的按钮、切驱动后的 snackbar 三处发起，
        // 对话框因此挂在这一层，而不是某一页里。
        MultidrawBenchDialogs(controller)
    }
}

@Composable
private fun MaterialNavigationBar(current: AppTab, onSelect: (AppTab) -> Unit) {
    NavigationBar(modifier = Modifier.fillMaxWidth()) {
        NavigationDestinations.forEach { (destination, icon, label) ->
            NavigationBarItem(
                selected = current == destination,
                onClick = { onSelect(destination) },
                icon = {
                    Icon(painter = painterResource(icon), contentDescription = stringResource(label))
                },
                label = { Text(stringResource(label)) },
            )
        }
    }
}

/**
 * 底栏双形态（与 Miuix 皮肤同语义）：标准 NavigationBar / Apple 风格悬浮液态玻璃胶囊。
 * 悬浮栏底距 = 导航栏 inset + 8dp（无手势导航设备回退 28dp）。
 */
@Composable
private fun MaterialBottomBar(
    current: AppTab,
    onSelect: (AppTab) -> Unit,
    backdrop: Backdrop,
    floatingBar: Boolean,
    glassBar: Boolean,
) {
    if (!floatingBar) {
        MaterialNavigationBar(current = current, onSelect = onSelect)
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
        NavigationDestinations.forEach { (destination, icon, label) ->
            FloatingBottomBarItem(
                selected = current == destination,
                onClick = { activateTab(destination.ordinal) },
                // weight 子项在 IntrinsicSize.Min 的 intrinsic 测量中宽度为 0，
                // 必须 minWidth 兜底，否则整个底栏塌缩成一个颗粒（KSU 同款写法）
                modifier = Modifier.defaultMinSize(minWidth = 76.dp),
            ) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = stringResource(label),
                )
                Text(
                    text = stringResource(label),
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

/** 底栏的侧边形态，条目与顺序同一份声明——两种形态永远不会各说各话。 */
@Composable
private fun MaterialNavigationRail(current: AppTab, onSelect: (AppTab) -> Unit) {
    NavigationRail {
        // Rail 默认从顶部排；底栏的条目是水平居中的，侧边形态没理由不垂直居中。
        Spacer(Modifier.weight(1f))
        NavigationDestinations.forEach { (destination, icon, label) ->
            NavigationRailItem(
                selected = current == destination,
                onClick = { onSelect(destination) },
                icon = {
                    Icon(painter = painterResource(icon), contentDescription = stringResource(label))
                },
                label = { Text(stringResource(label)) },
            )
        }
        Spacer(Modifier.weight(1f))
    }
}

private data class NavigationDestination(val tab: AppTab, val icon: Int, val label: Int)

private val NavigationDestinations = listOf(
    NavigationDestination(AppTab.Home, R.drawable.ic_home, R.string.nav_home),
    NavigationDestination(AppTab.Settings, R.drawable.ic_settings, R.string.nav_settings),
    NavigationDestination(AppTab.Info, R.drawable.ic_info, R.string.nav_info),
)

/**
 * 过场动画：同级切页是「淡入淡出 + 顺着方向的小位移」，进出子页面是「从右侧推入」。
 * 方向取自底部导航的顺序，所以位移方向和用户的手指方向一致。
 */
private fun pageTransition(from: Any, to: Any): ContentTransform {
    val fadeInSpec = tween<Float>(durationMillis = 260, easing = FastOutSlowInEasing)
    val fadeOutSpec = tween<Float>(durationMillis = 180, easing = FastOutSlowInEasing)
    val slide = tween<IntOffset>(durationMillis = 320, easing = FastOutSlowInEasing)

    // 位移是屏宽的几分之一：进出子页面推得多一点，同级切页只要一点点。
    val (enterFraction, exitFraction) = when {
        to is AppSubPage -> 3 to -10
        from is AppSubPage -> -10 to 3
        from is AppTab && to is AppTab && to.ordinal > from.ordinal -> 6 to -6
        else -> -6 to 6
    }

    return ContentTransform(
        targetContentEnter = slideInHorizontally(slide) { it / enterFraction } + fadeIn(fadeInSpec),
        initialContentExit = slideOutHorizontally(slide) { it / exitFraction } + fadeOut(fadeOutSpec),
        // 两页高度不同时不要裁剪，否则退场那一页会被切掉一块。
        sizeTransform = SizeTransform(clip = false),
    )
}

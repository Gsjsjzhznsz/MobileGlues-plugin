package com.fcl.plugin.mobileglues.ui.miuix

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fcl.plugin.mobileglues.R
import com.fcl.plugin.mobileglues.settings.ThemeMode
import com.fcl.plugin.mobileglues.ui.AppController
import com.fcl.plugin.mobileglues.ui.PrivacySections
import com.fcl.plugin.mobileglues.ui.ThirdPartyGroups
import com.fcl.plugin.mobileglues.ui.component.ScaleDialog
import com.fcl.plugin.mobileglues.ui.liquid.LocalGlassDarkTheme
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.SliderDefaults
import top.yukonga.miuix.kmp.basic.TabRow
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.CloudFill
import top.yukonga.miuix.kmp.icon.extended.GridView
import top.yukonga.miuix.kmp.icon.extended.HorizontalSplit
import top.yukonga.miuix.kmp.icon.extended.Play
import top.yukonga.miuix.kmp.icon.extended.Scan
import top.yukonga.miuix.kmp.icon.extended.Sidebar
import top.yukonga.miuix.kmp.icon.extended.Theme
import top.yukonga.miuix.kmp.icon.extended.Timer
import top.yukonga.miuix.kmp.icon.extended.Tune
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import kotlin.math.roundToInt

/** GL 信息页（Miuix）。 */
@Composable
fun MiuixGlInfoPage(controller: AppController) {
    val context = LocalContext.current
    val info by controller.glInfo.collectAsStateWithLifecycle()
    val loading by controller.glInfoLoading.collectAsStateWithLifecycle()
    val needsAngle by controller.glInfoNeedsAngle.collectAsStateWithLifecycle()
    val angleState by controller.glInfoAngle.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { controller.loadGlInfo() }

    MiuixSubPage(
        title = stringResource(R.string.dialog_mg_gl_info_title),
        onBack = { controller.navigateBack() },
        actions = {
            AnimatedVisibility(
                visible = !info.isNullOrBlank(),
                enter = fadeIn() + scaleIn(initialScale = 0.8f),
                exit = fadeOut() + scaleOut(targetScale = 0.8f),
            ) {
                IconButton(onClick = { copyGlInfo(context, controller, info.orEmpty()) }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_copy),
                        contentDescription = stringResource(R.string.copy),
                        tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        },
    ) {
        Crossfade(targetState = loading, label = "gl-info") { busy ->
            if (busy) {
                MiuixLoading(
                    text = stringResource(R.string.gl_info_loading),
                    modifier = Modifier.padding(top = 48.dp),
                )
            } else {
                Column {
                    // ANGLE 随启动器走，本 App 里没有；不借的话这一页讲的是系统驱动，
                    // 不是游戏里那个。借不借由用户点——不能因为他只想看一眼就自作主张
                    // 把别人的原生代码载进来。
                    if (needsAngle) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = MiuixScreenPadding, vertical = 4.dp),
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Text(
                                    text = stringResource(R.string.md_glinfo_needs_angle),
                                    style = MiuixTheme.textStyles.body2,
                                    color = MiuixTheme.colorScheme.primary,
                                )
                                TextButton(
                                    text = stringResource(R.string.md_glinfo_borrow),
                                    onClick = { controller.reloadGlInfoWithAngle() },
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                )
                            }
                        }
                    } else if (angleState == AppController.GlInfoAngle.Borrowed) {
                        Text(
                            text = stringResource(R.string.md_glinfo_borrowed),
                            style = MiuixTheme.textStyles.footnote2,
                            color = MiuixTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(horizontal = MiuixScreenPadding, vertical = 4.dp),
                        )
                    } else if (angleState == AppController.GlInfoAngle.BorrowIneffective) {
                        Text(
                            text = stringResource(R.string.md_glinfo_borrow_ineffective),
                            style = MiuixTheme.textStyles.footnote2,
                            color = MiuixTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(horizontal = MiuixScreenPadding, vertical = 4.dp),
                        )
                    }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = MiuixScreenPadding),
                    ) {
                        MiuixSelectableBody(
                            text = info.orEmpty(),
                            modifier = Modifier.padding(18.dp),
                        )
                    }
                }
            }
        }
        MiuixBottomSpacer()
    }
}

private fun copyGlInfo(context: Context, controller: AppController, text: String) {
    val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText(GL_INFO_CLIP_LABEL, text))
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        controller.snackbar(context.getString(R.string.copied))
    }
}

/** 隐私政策页（Miuix）。 */
@Composable
fun MiuixPrivacyPage(controller: AppController) {
    MiuixSubPage(
        title = stringResource(R.string.info_privacy),
        onBack = { controller.navigateBack() },
    ) {
        Text(
            text = stringResource(R.string.privacy_intro),
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onBackgroundVariant,
            modifier = Modifier.padding(horizontal = MiuixScreenPadding + 16.dp, vertical = 8.dp),
        )
        // 标题在卡片外、正文在卡片内——和设置页的分组是同一套语法。
        PrivacySections.forEach { (title, body) ->
            MiuixGroup(
                title = stringResource(title),
                titleColor = MiuixTheme.colorScheme.onSurface,
            ) {
                Text(
                    text = stringResource(body),
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
        }
        MiuixBottomSpacer()
    }
}

/**
 * 第三方开源项目（Miuix）。
 *
 * 分「渲染器」和「插件」两组：用户看到 SPIRV-Cross 的时候，应该同时知道它是被游戏里
 * 那个 .so 用的，而不是被这个设置界面用的。每一项都能点开自己的主页去看许可证原文——
 * 在这里抄一份许可证全文，既没人读，也保证不了和上游一致。
 */
@Composable
fun MiuixThirdPartyPage(controller: AppController) {
    MiuixSubPage(
        title = stringResource(R.string.third_party_title),
        onBack = { controller.navigateBack() },
    ) {
        Text(
            text = stringResource(R.string.third_party_intro),
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onBackgroundVariant,
            modifier = Modifier.padding(horizontal = MiuixScreenPadding + 16.dp, vertical = 8.dp),
        )
        ThirdPartyGroups.forEach { group ->
            MiuixGroup(title = stringResource(group.title)) {
                group.components.forEach { component ->
                    MiuixArrowRow(
                        title = component.name,
                        summary = "${component.author} · ${component.license}",
                        onClick = { controller.openThirdPartyComponent(component) },
                    )
                }
            }
        }
        MiuixBottomSpacer()
    }
}

/** 子页面骨架：返回 + 标题 + 操作，下面是可滚动内容。 */
@Composable
private fun MiuixSubPage(
    title: String,
    onBack: () -> Unit,
    actions: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back),
                    contentDescription = stringResource(R.string.nav_back),
                    tint = MiuixTheme.colorScheme.onBackground,
                    modifier = Modifier.size(22.dp),
                )
            }
            Text(
                text = title,
                style = MiuixTheme.textStyles.title4,
                color = MiuixTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f).padding(start = 8.dp),
            )
            actions()
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                // 甩到顶或底时给一下振动——HyperOS 的滚动到此为止就是这个手感。
                .scrollEndHaptic()
                .verticalScroll(rememberScrollState()),
        ) {
            content()
        }
    }
}

private const val GL_INFO_CLIP_LABEL = "MobileGlues GL info"

/**
 * 主题设置页（BandQQ ThemeScreen 照抄，Miuix 版）：
 * 主题实时预览卡片 → TabRow 三档模式 → Monet + 关键色 → 界面与效果（模糊/悬浮底栏/
 * 液态玻璃二级）→ 手势与显示（预测性返回/界面缩放）→ 动画（速度/级联延迟）。
 * 状态全部落在 [AppController.pluginConfig]，AppRoot 的 MgGlassTheme 消费。
 */
@Composable
fun MiuixThemePage(controller: AppController) {
    val themeMode by controller.pluginConfig.themeMode.collectAsStateWithLifecycle()
    val keyColor by controller.pluginConfig.keyColor.collectAsStateWithLifecycle()
    val enableBlur by controller.pluginConfig.enableBlur.collectAsStateWithLifecycle()
    val floatingBar by controller.pluginConfig.floatingBottomBar.collectAsStateWithLifecycle()
    val glassBar by controller.pluginConfig.floatingBottomBarGlass.collectAsStateWithLifecycle()
    val pageScale by controller.pluginConfig.pageScale.collectAsStateWithLifecycle()
    val predictiveBack by controller.pluginConfig.predictiveBack.collectAsStateWithLifecycle()
    val motionSpeed by controller.pluginConfig.motionSpeed.collectAsStateWithLifecycle()
    val motionStagger by controller.pluginConfig.motionStagger.collectAsStateWithLifecycle()

    val isDark = LocalGlassDarkTheme.current
    val supportBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    // 滑条的本地值：拖动期间即时反馈，松手/确认才落盘（BandQQ 同款）。
    val showScaleDialog = rememberSaveable { mutableStateOf(false) }
    var sliderValue by remember(pageScale) { mutableFloatStateOf(pageScale) }
    var speedValue by remember(motionSpeed) { mutableFloatStateOf(motionSpeed) }
    var staggerValue by remember(motionStagger) { mutableFloatStateOf(motionStagger.toFloat()) }

    val modeLabels = listOf(
        stringResource(R.string.theme_mode_system),
        stringResource(R.string.theme_mode_light),
        stringResource(R.string.theme_mode_dark),
    )
    val colorNames = listOf(stringResource(R.string.theme_keycolor_default)) +
        KEY_COLOR_OPTIONS.map { stringResource(it.second) }
    val colorValues = listOf(0) + KEY_COLOR_OPTIONS.map { it.first.toInt() }

    MiuixSubPage(
        title = stringResource(R.string.theme_title),
        onBack = { controller.navigateBack() },
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(12.dp))

            // ===== 主题实时预览卡片 =====
            ThemePreviewCard(
                isDark = isDark,
                monet = themeMode.isMonet,
                floatingBar = floatingBar,
                glassBar = glassBar,
            )
            Spacer(Modifier.height(24.dp))

            // ===== 主题模式 TabRow（跟随系统 / 浅色 / 深色，Monet 由开关叠加）=====
            TabRow(
                tabs = modeLabels,
                selectedTabIndex = themeMode.value % 3,
                onTabSelected = { index ->
                    controller.pluginConfig.setThemeMode(
                        ThemeMode.fromValue(index + if (themeMode.isMonet) 3 else 0)
                    )
                },
            )

            // ===== Monet 颜色卡片 =====
            Card(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .fillMaxWidth(),
            ) {
                SwitchPreference(
                    title = stringResource(R.string.theme_monet),
                    summary = stringResource(R.string.theme_monet_summary),
                    startAction = {
                        Icon(
                            MiuixIcons.Theme,
                            modifier = Modifier.padding(end = 6.dp),
                            contentDescription = stringResource(R.string.theme_monet),
                            tint = MiuixTheme.colorScheme.onBackground,
                        )
                    },
                    checked = themeMode.isMonet,
                    onCheckedChange = { on ->
                        controller.pluginConfig.setThemeMode(
                            ThemeMode.fromValue(themeMode.value % 3 + if (on) 3 else 0)
                        )
                    },
                )
                AnimatedVisibility(visible = themeMode.isMonet) {
                    Column {
                        OverlayDropdownPreference(
                            title = stringResource(R.string.theme_key_color),
                            startAction = {
                                Icon(
                                    MiuixIcons.Tune,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = stringResource(R.string.theme_key_color),
                                    tint = MiuixTheme.colorScheme.onBackground,
                                )
                            },
                            items = colorNames,
                            selectedIndex = colorValues.indexOf(keyColor).takeIf { it >= 0 } ?: 0,
                            onSelectedIndexChange = { index ->
                                controller.pluginConfig.setKeyColor(colorValues[index])
                            },
                        )
                    }
                }
            }

            // ===== 界面与效果：模糊 / 悬浮底栏 / 液态玻璃（二级）=====
            SmallTitle(text = stringResource(R.string.theme_section_effects))
            Card(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .fillMaxWidth(),
            ) {
                if (supportBlur) {
                    SwitchPreference(
                        title = stringResource(R.string.glass_blur),
                        summary = stringResource(R.string.glass_blur_summary),
                        startAction = {
                            Icon(
                                MiuixIcons.CloudFill,
                                modifier = Modifier.padding(end = 6.dp),
                                contentDescription = stringResource(R.string.glass_blur),
                                tint = MiuixTheme.colorScheme.onBackground,
                            )
                        },
                        checked = enableBlur,
                        onCheckedChange = { on -> controller.pluginConfig.setEnableBlur(on) },
                    )
                }
                SwitchPreference(
                    title = stringResource(R.string.glass_floating_bar),
                    summary = stringResource(R.string.glass_floating_bar_summary),
                    startAction = {
                        Icon(
                            MiuixIcons.HorizontalSplit,
                            modifier = Modifier.padding(end = 6.dp),
                            contentDescription = stringResource(R.string.glass_floating_bar),
                            tint = MiuixTheme.colorScheme.onBackground,
                        )
                    },
                    checked = floatingBar,
                    onCheckedChange = { on -> controller.pluginConfig.setFloatingBottomBar(on) },
                )
                // 悬浮底栏开启后的二级选项：液态玻璃（Android 13+）
                AnimatedVisibility(visible = floatingBar && supportBlur) {
                    SwitchPreference(
                        title = stringResource(R.string.glass_floating_glass),
                        summary = stringResource(R.string.glass_floating_glass_summary),
                        startAction = {
                            Icon(
                                MiuixIcons.Scan,
                                modifier = Modifier.padding(end = 6.dp),
                                contentDescription = stringResource(R.string.glass_floating_glass),
                                tint = MiuixTheme.colorScheme.onBackground,
                            )
                        },
                        checked = glassBar,
                        onCheckedChange = { on -> controller.pluginConfig.setFloatingBottomBarGlass(on) },
                    )
                }
            }

            // ===== 手势与显示：预测性返回 / 界面缩放 =====
            SmallTitle(text = stringResource(R.string.theme_section_gestures))
            Card(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .fillMaxWidth(),
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    SwitchPreference(
                        title = stringResource(R.string.theme_predictive_back),
                        summary = stringResource(R.string.theme_predictive_back_summary),
                        startAction = {
                            Icon(
                                MiuixIcons.Sidebar,
                                modifier = Modifier.padding(end = 6.dp),
                                contentDescription = stringResource(R.string.theme_predictive_back),
                                tint = MiuixTheme.colorScheme.onBackground,
                            )
                        },
                        checked = predictiveBack,
                        onCheckedChange = { on -> controller.pluginConfig.setPredictiveBack(on) },
                    )
                }

                ArrowPreference(
                    title = stringResource(R.string.theme_page_scale),
                    summary = stringResource(R.string.theme_page_scale_summary),
                    startAction = {
                        Icon(
                            MiuixIcons.GridView,
                            modifier = Modifier.padding(end = 6.dp),
                            contentDescription = stringResource(R.string.theme_page_scale),
                            tint = MiuixTheme.colorScheme.onBackground,
                        )
                    },
                    endActions = {
                        Text(
                            text = "${(sliderValue * 100).toInt()}%",
                            color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                        )
                    },
                    onClick = { showScaleDialog.value = !showScaleDialog.value },
                    holdDownState = showScaleDialog.value,
                    bottomAction = {
                        Slider(
                            value = sliderValue,
                            onValueChange = { sliderValue = it },
                            onValueChangeFinished = {
                                controller.pluginConfig.setPageScale(sliderValue)
                            },
                            valueRange = 0.8f..1.1f,
                            showKeyPoints = true,
                            keyPoints = listOf(0.8f, 0.9f, 1f, 1.1f),
                            magnetThreshold = 0.01f,
                            hapticEffect = SliderDefaults.SliderHapticEffect.Step,
                        )
                    },
                )
                ScaleDialog(
                    show = showScaleDialog.value,
                    onDismissRequest = { showScaleDialog.value = false },
                    volumeState = { pageScale },
                    onVolumeChange = { scale -> controller.pluginConfig.setPageScale(scale) },
                )
            }

            // ===== 动画：速度 / 级联延迟 =====
            SmallTitle(text = stringResource(R.string.theme_section_animation))
            Card(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .fillMaxWidth(),
            ) {
                ArrowPreference(
                    title = stringResource(R.string.theme_motion_speed),
                    summary = stringResource(R.string.theme_motion_speed_summary),
                    startAction = {
                        Icon(
                            MiuixIcons.Play,
                            modifier = Modifier.padding(end = 6.dp),
                            contentDescription = stringResource(R.string.theme_motion_speed),
                            tint = MiuixTheme.colorScheme.onBackground,
                        )
                    },
                    endActions = {
                        Text(
                            text = "${(speedValue * 10).roundToInt() / 10.0}x",
                            color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                        )
                    },
                    bottomAction = {
                        Slider(
                            value = speedValue,
                            onValueChange = { speedValue = it },
                            onValueChangeFinished = {
                                controller.pluginConfig.setMotionSpeed(
                                    (speedValue * 100).roundToInt() / 100f
                                )
                            },
                            valueRange = 0.5f..2f,
                            showKeyPoints = true,
                            keyPoints = listOf(0.5f, 1f, 1.5f, 2f),
                            magnetThreshold = 0.01f,
                            hapticEffect = SliderDefaults.SliderHapticEffect.Step,
                        )
                    },
                )
                ArrowPreference(
                    title = stringResource(R.string.theme_motion_stagger),
                    summary = stringResource(R.string.theme_motion_stagger_summary),
                    startAction = {
                        Icon(
                            MiuixIcons.Timer,
                            modifier = Modifier.padding(end = 6.dp),
                            contentDescription = stringResource(R.string.theme_motion_stagger),
                            tint = MiuixTheme.colorScheme.onBackground,
                        )
                    },
                    endActions = {
                        Text(
                            text = "${staggerValue.roundToInt()}ms",
                            color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                        )
                    },
                    bottomAction = {
                        Slider(
                            value = staggerValue,
                            onValueChange = { staggerValue = it },
                            onValueChangeFinished = {
                                controller.pluginConfig.setMotionStagger(staggerValue.roundToInt())
                            },
                            valueRange = 0f..200f,
                            showKeyPoints = true,
                            keyPoints = listOf(0f, 50f, 100f, 150f, 200f),
                            magnetThreshold = 1f,
                            hapticEffect = SliderDefaults.SliderHapticEffect.Step,
                        )
                    },
                )
            }

            MiuixBottomSpacer()
        }
    }
}

/** Monet 关键色候选（0 = 默认品牌蓝/系统色板），对齐 KSU keyColorOptions / BandQQ ThemeScreen 的用法 */
private val KEY_COLOR_OPTIONS = listOf(
    0xFFF44336L to R.string.keycolor_red, 0xFFE91E63L to R.string.keycolor_pink,
    0xFF9C27B0L to R.string.keycolor_purple, 0xFF673AB7L to R.string.keycolor_deep_purple,
    0xFF3F51B5L to R.string.keycolor_indigo, 0xFF2196F3L to R.string.keycolor_blue,
    0xFF00BCD4L to R.string.keycolor_cyan, 0xFF009688L to R.string.keycolor_teal,
    0xFF4CAF50L to R.string.keycolor_green, 0xFFFFEB3BL to R.string.keycolor_yellow,
    0xFFFFC107L to R.string.keycolor_amber, 0xFFFF9800L to R.string.keycolor_orange,
    0xFF795548L to R.string.keycolor_brown, 0xFF607D8BL to R.string.keycolor_blue_grey,
    0xFFFF8FABL to R.string.keycolor_cherry_pink,
)

/**
 * 主题实时预览卡片（BandQQ ThemePreviewCard 照抄）：
 * 按当前模式/Monet/悬浮底栏/玻璃状态实时渲染一个迷你手机界面示意。
 */
@Composable
private fun ThemePreviewCard(
    isDark: Boolean,
    monet: Boolean,
    floatingBar: Boolean,
    glassBar: Boolean,
) {
    val cs = MiuixTheme.colorScheme
    val bgColor = cs.background
    val cardColor = cs.surfaceVariant
    val accentColor = cs.primary
    val navBarColor = cs.surface
    val textColor = cs.onBackground

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.42f)
                .height(150.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(bgColor)
                .border(1.dp, cs.outline, RoundedCornerShape(20.dp)),
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "MobileGlues",
                    fontSize = 11.sp,
                    color = textColor,
                    modifier = Modifier.padding(start = 4.dp),
                )
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(accentColor.copy(alpha = 0.18f)),
                )
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(30.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(cardColor),
                )
            }

            if (floatingBar) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp)
                        .height(22.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(if (glassBar) navBarColor.copy(alpha = 0.5f) else navBarColor)
                        .border(0.5.dp, textColor.copy(alpha = 0.12f), RoundedCornerShape(11.dp))
                        .padding(horizontal = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    repeat(3) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (it == 0) accentColor else textColor.copy(alpha = 0.6f)),
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(0.5.dp)
                            .background(textColor.copy(alpha = 0.1f)),
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(22.dp)
                            .background(navBarColor)
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        repeat(3) {
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(if (it == 0) accentColor else textColor.copy(alpha = 0.6f)),
                            )
                        }
                    }
                }
            }
        }
    }
}

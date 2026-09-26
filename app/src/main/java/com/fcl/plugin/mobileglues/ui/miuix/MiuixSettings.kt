@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.fcl.plugin.mobileglues.ui.miuix

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fcl.plugin.mobileglues.R
import com.fcl.plugin.mobileglues.settings.AngleConfig
import com.fcl.plugin.mobileglues.settings.DepthClearFixMode
import com.fcl.plugin.mobileglues.settings.Fsr1Preset
import com.fcl.plugin.mobileglues.settings.GlVersion
import com.fcl.plugin.mobileglues.settings.GlslCacheScale
import com.fcl.plugin.mobileglues.settings.MGConfig
import com.fcl.plugin.mobileglues.settings.NoErrorConfig
import com.fcl.plugin.mobileglues.settings.RendererBackend
import com.fcl.plugin.mobileglues.settings.SpinnerOption
import com.fcl.plugin.mobileglues.settings.UiStyle
import com.fcl.plugin.mobileglues.ui.AppController
import com.fcl.plugin.mobileglues.ui.AppSubPage
import com.fcl.plugin.mobileglues.ui.SettingsLoadState
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.TabRowWithContour
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

/**
 * 设置页（Miuix）。
 *
 * 分组、权限门、每一处警告与倒计时都与 MD3 皮肤一致——它们都在 [AppController] 里。
 * 不同的只是表达方式：这里的选项用 Miuix 的就地下拉，而不是弹出一个对话框。
 */
@Composable
fun MiuixSettingsPage(controller: AppController) {
    val auth by controller.auth.state.collectAsStateWithLifecycle()
    val loadState by controller.loadState.collectAsStateWithLifecycle()
    val config by controller.configStore.config.collectAsStateWithLifecycle()
    val uiStyle by controller.pluginConfig.uiStyle.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { controller.ensureDeviceInfo() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            // 甩到顶或底时给一下振动——HyperOS 的滚动到此为止就是这个手感。
            .scrollEndHaptic()
            .verticalScroll(rememberScrollState()),
    ) {
        MiuixPageTitle(stringResource(R.string.nav_settings))

        MiuixGroup(title = stringResource(R.string.settings_group_plugin)) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text(
                    text = stringResource(R.string.ui_style),
                    style = MiuixTheme.textStyles.body1,
                    color = MiuixTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(10.dp))
                TabRowWithContour(
                    tabs = listOf(
                        stringResource(R.string.ui_style_material),
                        stringResource(R.string.ui_style_miuix),
                    ),
                    selectedTabIndex = uiStyle.ordinal,
                    onTabSelected = { controller.pluginConfig.setUiStyle(UiStyle.entries[it]) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            // 主题设置紧挨着界面风格：两者都是外观类选项，用户找的就是这一带。
            MiuixArrowRow(
                title = stringResource(R.string.theme_title),
                onClick = { controller.openSubPage(AppSubPage.Theme) },
            )
        }

        Crossfade(
            targetState = auth.granted to (loadState == SettingsLoadState.Ready && config != null),
            label = "settings-gate",
        ) { (granted, ready) ->
            Column(modifier = Modifier.fillMaxWidth()) {
                when {
                    !granted -> PermissionGate(onGrant = controller::requestAccess)
                    ready -> ConfigSections(controller, config ?: MGConfig.Default)
                    else -> MiuixLoading(modifier = Modifier.padding(top = 48.dp))
                }
            }
        }

        MiuixBottomSpacer()
    }
}

@Composable
private fun PermissionGate(onGrant: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MiuixScreenPadding, vertical = 20.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Text(
                text = stringResource(R.string.settings_gate_title),
                style = MiuixTheme.textStyles.title4,
                color = MiuixTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.settings_gate_msg),
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.padding(top = 8.dp),
            )
            Spacer(Modifier.height(20.dp))
            Button(onClick = onGrant, colors = ButtonDefaults.buttonColorsPrimary()) {
                Text(
                    text = stringResource(R.string.settings_gate_grant),
                    style = MiuixTheme.textStyles.button,
                    color = MiuixTheme.colorScheme.onPrimary,
                )
            }
        }
    }
}

@Composable
private fun ConfigSections(controller: AppController, config: MGConfig) {
    val context = LocalContext.current
    val deviceInfo by controller.deviceInfo.collectAsStateWithLifecycle()
    val cacheBytes by controller.configStore.glslCacheBytes.collectAsStateWithLifecycle()
    var multidrawExpanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        MiuixGroup(title = stringResource(R.string.settings_group_render)) {
            // mg-3backends：Air 6.0 同款切换器 —— 单一渲染后端行，三选一，
            // 默认 Vulkan 直连；与 Material 主题的行位完全一致。
            OptionRow(
                title = stringResource(R.string.option_renderer_backend),
                options = RendererBackend.entries,
                selected = config.backend,
                labelOf = { it.label(context) },
                onSelect = controller::selectBackend,
            )
            OptionRow(
                title = stringResource(R.string.option_angle),
                options = AngleConfig.entries,
                selected = config.angle,
                onSelect = controller::selectAngle,
            )
            OptionRow(
                title = stringResource(R.string.option_no_error),
                options = NoErrorConfig.entries,
                selected = config.noError,
                onSelect = controller::selectNoError,
            )
            OptionRow(
                title = stringResource(R.string.option_angle_clear_workaround),
                options = DepthClearFixMode.entries,
                selected = config.depthClearFix,
                onSelect = controller::selectDepthClearFix,
            )
            MiuixSwitchRow(
                title = stringResource(R.string.option_enable_fsr1),
                checked = config.fsr1Enabled,
                onCheckedChange = controller::setFsr1,
            )
            // FSR1 的两个子设置只在它开着的时候出现，行位与 Material 主题一致。
            AnimatedVisibility(
                visible = config.fsr1Enabled,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column {
                    OptionRow(
                        title = stringResource(R.string.option_fsr1_super_resolution),
                        options = Fsr1Preset.Presets,
                        selected = config.fsr1,
                        onSelect = controller::selectFsr1Preset,
                    )
                    MiuixSliderRow(
                        title = stringResource(R.string.option_fsr1_sharpness),
                        valueLabel = stringResource(R.string.option_fsr1_sharpness_value, config.fsr1Sharpness),
                        position = config.fsr1Sharpness,
                        steps = 100,
                        onPositionChange = controller::setFsr1Sharpness,
                        onDragFinished = {},
                    )
                    // 与 Material 主题同款提示：FSR1 只落在 GLES 家族后端，
                    // Vulkan 直连是路线图上的未完成项，别让开关静默无效。
                    if (config.backend == RendererBackend.DirectVulkan) {
                        Text(
                            text = stringResource(R.string.fsr1_backend_unsupported_hint),
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = MiuixScreenPadding, vertical = 8.dp),
                        )
                    }
                }
            }
        }

        MiuixGroup(title = stringResource(R.string.settings_group_cache)) {
            GlslCacheSlider(controller, config, deviceInfo?.totalRamBytes)
            // 没有缓存文件时不摆一个删不了东西的按钮：它按需浮现，删完收回。
            AnimatedVisibility(
                visible = cacheBytes != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                MiuixArrowRow(
                    title = stringResource(
                        R.string.option_glsl_cache_delete,
                        controller.formatCacheSize(cacheBytes ?: 0L),
                    ),
                    titleColor = MiuixTheme.colorScheme.error,
                    onClick = controller::deleteGlslCache,
                )
            }
        }

        MiuixGroup(title = stringResource(R.string.settings_group_ext)) {
            MiuixSwitchRow(
                title = stringResource(R.string.option_ext_cs),
                checked = config.extComputeShader,
                onCheckedChange = controller::setExtComputeShader,
            )
            MiuixSwitchRow(
                // 磁盘上记的是「启用」，界面上问的是「禁用」，取反只发生在这一行。
                title = stringResource(R.string.option_ext_timer_query),
                checked = !config.extTimerQuery,
                onCheckedChange = controller::setExtTimerQueryDisabled,
            )
            MiuixSwitchRow(
                title = stringResource(R.string.option_ext_direct_state_access),
                checked = config.extDirectStateAccess,
                onCheckedChange = controller::setExtDirectStateAccess,
            )
        }

        MiuixGroup(title = stringResource(R.string.settings_group_advanced)) {
            OptionRow(
                title = stringResource(R.string.option_custom_gl_version),
                options = GlVersion.entries,
                selected = config.glVersion,
                onSelect = controller::selectGlVersion,
            )

            MiuixExpandableSection(
                title = stringResource(R.string.option_multidraw),
                summary = miuixMultidrawSummary(config.multidraw),
                expanded = multidrawExpanded,
                onToggle = { multidrawExpanded = !multidrawExpanded },
            ) {
                MiuixMultidrawOrderContent(controller, config)
            }
        }
    }

}

/**
 * 缓存上限滑块。
 *
 * 拖动期间用本地档位，松手才交还给配置：档位 → MiB → 档位 的换算有取整，
 * 直接跟着配置画的话手指底下的滑块会自己抖。
 */
@Composable
private fun GlslCacheSlider(controller: AppController, config: MGConfig, totalRamBytes: Long?) {
    val mebibytes = config.glslCache.mebibytesOrZero
    val base = totalRamBytes?.let { GlslCacheScale.baseCeiling(it) }
        ?: GlslCacheScale.MIN_UPPER_BOUND_MIB.toInt()
    val ceiling = maxOf(base, mebibytes)
    var dragPosition by remember { mutableStateOf<Int?>(null) }

    MiuixSliderRow(
        title = stringResource(R.string.option_glsl_cache),
        valueLabel = if (mebibytes > 0) {
            stringResource(R.string.option_glsl_cache_value, mebibytes)
        } else {
            stringResource(R.string.option_glsl_cache_off)
        },
        position = dragPosition ?: GlslCacheScale.positionFor(mebibytes, ceiling),
        steps = GlslCacheScale.STEPS,
        onPositionChange = { position ->
            dragPosition = position
            controller.setGlslCacheSliderPosition(position, ceiling)
        },
        onDragFinished = { dragPosition = null },
    )
}

/** 枚举 → 下拉行。选项顺序就是枚举的声明顺序，不会出现「选项与取值对不上」。 */
@Composable
private fun <T : SpinnerOption> OptionRow(
    title: String,
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
) {
    val context = LocalContext.current
    MiuixDropdownRow(
        title = title,
        options = options.map { it.label(context).toString() },
        selectedIndex = options.indexOf(selected),
        onSelect = { onSelect(options[it]) },
    )
}

/** 带 label 映射的重载：RendererBackend 的 wire 是字符串，进不了 SpinnerOption 约束。 */
@Composable
private fun <T> OptionRow(
    title: String,
    options: List<T>,
    selected: T,
    labelOf: (T) -> CharSequence,
    onSelect: (T) -> Unit,
) {
    MiuixDropdownRow(
        title = title,
        options = options.map { labelOf(it).toString() },
        selectedIndex = options.indexOf(selected),
        onSelect = { onSelect(options[it]) },
    )
}

@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.fcl.plugin.mobileglues.ui.material

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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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

/**
 * 设置页。
 *
 * 权限门只拦渲染器配置那一段：「Plugin 配置」（界面风格）是本 App 自己的偏好，
 * 和 MG 目录无关，所以放在门之上——未授权时这一页也不是一片空白。
 */
@Composable
fun MaterialSettingsPage(controller: AppController) {
    val auth by controller.auth.state.collectAsStateWithLifecycle()
    val loadState by controller.loadState.collectAsStateWithLifecycle()
    val config by controller.configStore.config.collectAsStateWithLifecycle()
    val uiStyle by controller.pluginConfig.uiStyle.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { controller.ensureDeviceInfo() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        PageTitle(stringResource(R.string.nav_settings))

        PreferenceGroup(title = stringResource(R.string.settings_group_plugin)) {
            SegmentedChoiceRow(
                title = stringResource(R.string.ui_style),
                options = listOf(
                    stringResource(R.string.ui_style_material),
                    stringResource(R.string.ui_style_miuix),
                ),
                selectedIndex = uiStyle.ordinal,
                onSelect = { controller.pluginConfig.setUiStyle(UiStyle.entries[it]) },
            )
            // 主题设置紧挨着界面风格：两者都是外观类选项，用户找的就是这一带。
            TextPreferenceRow(
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
                    else -> CenteredLoading(modifier = Modifier.padding(top = 48.dp))
                }
            }
        }

        BottomSpacer()
    }
}

/** 未授权时的权限门。 */
@Composable
private fun PermissionGate(onGrant: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenPadding, vertical = 20.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
            Text(
                text = stringResource(R.string.settings_gate_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.settings_gate_msg),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
            Button(onClick = onGrant, modifier = Modifier.padding(top = 20.dp)) {
                Text(stringResource(R.string.settings_gate_grant))
            }
        }
    }
}

/** 权限门之内：渲染 / 着色器缓存 / 扩展 / 高级。 */
@Composable
private fun ConfigSections(controller: AppController, config: MGConfig) {
    val context = LocalContext.current
    val deviceInfo by controller.deviceInfo.collectAsStateWithLifecycle()
    val cacheBytes by controller.configStore.glslCacheBytes.collectAsStateWithLifecycle()

    var choice by remember { mutableStateOf<ChoiceTarget?>(null) }
    var multidrawExpanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        PreferenceGroup(title = stringResource(R.string.settings_group_render)) {
            // mg-3backends：Air 6.0 同款切换器 —— MobileGlues 分区里的单一
            // 渲染后端 pick 行，三选一，默认 Vulkan 直连；严禁拆成三条独立条目。
            TextPreferenceRow(
                title = stringResource(R.string.option_renderer_backend),
                summary = config.backend.label(context).toString(),
                onClick = { choice = ChoiceTarget.Backend },
            )
            TextPreferenceRow(
                title = stringResource(R.string.option_angle),
                summary = config.angle.label(context).toString(),
                onClick = { choice = ChoiceTarget.Angle },
            )
            TextPreferenceRow(
                title = stringResource(R.string.option_no_error),
                summary = config.noError.label(context).toString(),
                onClick = { choice = ChoiceTarget.NoError },
            )
            TextPreferenceRow(
                title = stringResource(R.string.option_angle_clear_workaround),
                summary = config.depthClearFix.label(context).toString(),
                onClick = { choice = ChoiceTarget.DepthClear },
            )
            SwitchPreferenceRow(
                title = stringResource(R.string.option_enable_fsr1),
                checked = config.fsr1Enabled,
                onCheckedChange = controller::setFsr1,
            )
            // FSR1 的两个子设置只在它开着的时候出现：关着摆出来只能徒增困惑。
            AnimatedVisibility(
                visible = config.fsr1Enabled,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column {
                    TextPreferenceRow(
                        title = stringResource(R.string.option_fsr1_super_resolution),
                        summary = config.fsr1.label(context).toString(),
                        onClick = { choice = ChoiceTarget.Fsr1Preset },
                    )
                    SliderPreferenceRow(
                        title = stringResource(R.string.option_fsr1_sharpness),
                        valueLabel = stringResource(R.string.option_fsr1_sharpness_value, config.fsr1Sharpness),
                        position = config.fsr1Sharpness,
                        steps = 100,
                        onPositionChange = controller::setFsr1Sharpness,
                        onDragFinished = {},
                    )
                    // FSR1 目前只落在 GLES 家族后端（DirectGLES / MobileGlues）；
                    // Vulkan 直连是路线图上的未完成项。开着开关却毫无动静是最伤
                    // 体验的组合，这里在设置页把它挑明。
                    if (config.backend == RendererBackend.DirectVulkan) {
                        Text(
                            text = stringResource(R.string.fsr1_backend_unsupported_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = ScreenPadding, vertical = 8.dp),
                        )
                    }
                }
            }
        }

        PreferenceGroup(title = stringResource(R.string.settings_group_cache)) {
            GlslCacheSlider(controller, config, deviceInfo?.totalRamBytes)
            // 没有缓存文件时不摆一个删不了东西的按钮：它按需浮现，删完收回。
            AnimatedVisibility(
                visible = cacheBytes != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                TextPreferenceRow(
                    title = stringResource(
                        R.string.option_glsl_cache_delete,
                        controller.formatCacheSize(cacheBytes ?: 0L),
                    ),
                    titleColor = MaterialTheme.colorScheme.error,
                    onClick = controller::deleteGlslCache,
                )
            }
        }

        PreferenceGroup(title = stringResource(R.string.settings_group_ext)) {
            SwitchPreferenceRow(
                title = stringResource(R.string.option_ext_cs),
                checked = config.extComputeShader,
                onCheckedChange = controller::setExtComputeShader,
            )
            SwitchPreferenceRow(
                // 磁盘上记的是「启用」，界面上问的是「禁用」，取反只发生在这一行。
                title = stringResource(R.string.option_ext_timer_query),
                checked = !config.extTimerQuery,
                onCheckedChange = controller::setExtTimerQueryDisabled,
            )
            SwitchPreferenceRow(
                title = stringResource(R.string.option_ext_direct_state_access),
                checked = config.extDirectStateAccess,
                onCheckedChange = controller::setExtDirectStateAccess,
            )
        }

        PreferenceGroup(title = stringResource(R.string.settings_group_advanced)) {
            TextPreferenceRow(
                title = stringResource(R.string.option_custom_gl_version),
                summary = config.glVersion.label(context).toString(),
                onClick = { choice = ChoiceTarget.GlVersion },
            )

            ExpandableSection(
                title = stringResource(R.string.option_multidraw),
                summary = multidrawSummary(config.multidraw),
                expanded = multidrawExpanded,
                onToggle = { multidrawExpanded = !multidrawExpanded },
            ) {
                MultidrawOrderContent(controller, config)
            }
        }
    }


    // ---- 选项对话框 ----

    when (choice) {
        ChoiceTarget.Backend -> OptionDialog(
            title = stringResource(R.string.option_renderer_backend),
            options = RendererBackend.entries,
            selected = config.backend,
            labelOf = { it.label(context) },
            onSelect = controller::selectBackend,
            onDismiss = { choice = null },
        )

        ChoiceTarget.Angle -> OptionDialog(
            title = stringResource(R.string.option_angle),
            options = AngleConfig.entries,
            selected = config.angle,
            onSelect = controller::selectAngle,
            onDismiss = { choice = null },
        )

        ChoiceTarget.NoError -> OptionDialog(
            title = stringResource(R.string.option_no_error),
            options = NoErrorConfig.entries,
            selected = config.noError,
            onSelect = controller::selectNoError,
            onDismiss = { choice = null },
        )

        ChoiceTarget.DepthClear -> OptionDialog(
            title = stringResource(R.string.option_angle_clear_workaround),
            options = DepthClearFixMode.entries,
            selected = config.depthClearFix,
            onSelect = controller::selectDepthClearFix,
            onDismiss = { choice = null },
        )

        ChoiceTarget.GlVersion -> OptionDialog(
            title = stringResource(R.string.option_custom_gl_version),
            options = GlVersion.entries,
            selected = config.glVersion,
            onSelect = controller::selectGlVersion,
            onDismiss = { choice = null },
        )

        ChoiceTarget.Fsr1Preset -> OptionDialog(
            title = stringResource(R.string.option_fsr1_super_resolution),
            options = Fsr1Preset.Presets,
            selected = config.fsr1,
            onSelect = controller::selectFsr1Preset,
            onDismiss = { choice = null },
        )

        null -> Unit
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
    // 内存还没查回来时先按最小量程画，查到之后量程只会变大，滑块位置不会倒退。
    val base = totalRamBytes?.let { GlslCacheScale.baseCeiling(it) }
        ?: GlslCacheScale.MIN_UPPER_BOUND_MIB.toInt()
    val ceiling = maxOf(base, mebibytes)
    var dragPosition by remember { mutableStateOf<Int?>(null) }

    SliderPreferenceRow(
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

/** Spinner 的替代：枚举 → 单选对话框，选项顺序就是枚举的声明顺序。 */
@Composable
private fun <T : SpinnerOption> OptionDialog(
    title: String,
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    SingleChoiceDialog(
        title = title,
        options = options.map { it.label(context).toString() },
        selectedIndex = options.indexOf(selected),
        onSelect = { onSelect(options[it]) },
        onDismiss = onDismiss,
    )
}

/** 带 label 映射的重载：RendererBackend 的 wire 是字符串（dispatcher 按名字路由），
 *  进不了 SpinnerOption 的 wire:Int 约束，但对话框形态与此完全同构。 */
@Composable
private fun <T> OptionDialog(
    title: String,
    options: List<T>,
    selected: T,
    labelOf: (T) -> CharSequence,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    SingleChoiceDialog(
        title = title,
        options = options.map { labelOf(it).toString() },
        selectedIndex = options.indexOf(selected),
        onSelect = { onSelect(options[it]) },
        onDismiss = onDismiss,
    )
}

private enum class ChoiceTarget { Backend, Angle, NoError, DepthClear, GlVersion, Fsr1Preset }

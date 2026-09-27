# Worklog

> 学 Air 项目的会话协作模式：**仓库是唯一事实源**。新会话开场只读本节速览 + 末尾最新 Task；更早历史查 git log 与本地 /home/z/my-project/worklog.md（沙箱工件，可能被重置，勿依赖）。

## ⚡ READ ME FIRST —— 会话速览

> 最后更新：Task 1（2026-09-27，FSR 闪屏第四轮：移植 air（Amethyst fork）Task 82 视口锁存形状过滤到双端 + surface 身份/呈现上下文两条决定性遥测；确认 air MobileGL 方案 = mgl_fsr.mm 桥接层呈现前 EASU+RCAS，插件侧对应物 = vendored 核内置 FSR1 + dispatcher config→env 桥接，链路已验证完整）。
> 新会话规则：新任务记录**追加到本文件最末尾**（`## Task N` 模板）；收尾时同步更新「当前状态」表；本文件超 ~400 行时把最旧 Task 段挪进 worklog-archive.md。

### 一句话
MobileGlues-plugin（分支 mg-3backends）= 安卓插件壳 app + 两个渲染子模块：**MobileGlues**（独立渲染 native，libmobileglues.so 内核，FSR1 = Arm EASU+RCAS 两趟）与 **MobileGL**（dispatcher libmobileglues.so 转发 + DirectVulkan / DirectGLES / vendored MobileGlues 三后端，FSR1 只在 GLES 家族）。

### 当前状态（收尾时更新）
| 项 | 值 |
|---|---|
| FSR 配置链（已验证） | 插件写 /sdcard/MG/config.json（fsr1Setting 0-4 / fsr1Sharpness 0-100 / backendType）→ dispatcher mg_config_scan（MG_DIR_PATH 优先）→ MobileGlues 核 settings.cpp 直读；MobileGL 核走 setenv("MOBILEGL_FSR1"/"MOBILEGL_FSR1_SHARPNESS") → DirectGLES FSR1 IsEnabled() 惰性 getenv |
| **DirectVulkan 无 FSR（结构性）** | FSR1 只实现于 GLES 家族；用户若停在默认后端 DirectVulkan，FSR 必然无效。UI 的 FSR 区块已有红字提示 + 一键"切换后端到 GLES"按钮 |
| 已知闪屏修复史 | ①viewport 触发的 target 翻转（改为 swap 时 surface query 唯一尺寸权威）②surface query 翻转（pendingStreak≥2 去抖）③编译失败自禁用（不再 0x0 视口循环）④本轮：units 锁存形状过滤（air Task 82） |
| 待用户装机验证 | 本轮 Task 1 锚点（见文末判定表）|
| 推送纪律 | **子模块先、宿主后**；token 配 remote；CI 把关构建（本地无 Android SDK）|
| 沙箱生存 | /home/z/my-project 会被重置：repo 重 clone + `submodule update --init`；本 worklog 在仓库内所以永存 |

### 关键路径与命令
- 仓库根（本地）：/home/z/my-project/plugin3b/repo；上游：Gsjsjzhznsz/MobileGlues-plugin（host）+ MobileGlues / MobileGL 子模块（同一账号）
- 推送：`git remote set-url origin https://<token>@github.com/Gsjsjzhznsz/<repo>.git`（三仓库各自配）
- 用户日志渠道：直接推仓库根 latest_game.log / latest.log（"Add files via upload"）
- CI 轮询：`gh api /repos/Gsjsjzhznsz/MobileGlues-plugin/actions/runs?per_page=N`（子模块 CI 同理）
- 产物验证：`strings lib*.so | grep -E "FSR1|mg-dispatch"` 校验新日志串已编入

### air 项目（Gsjsjzhznsz/Air-Minecraft-iOS-Launcher）FSR 经验索引 —— **勿忘看这里**
- 仓库根有 worklog.md + worklog-archive.md，FSR 相关：Task 78-85 / 99-106 / 119-130 / 143 / 154 / 165（`grep -n "Task ID" worklog-archive.md`）
- **Task 82**：FSR"缩左下角"根因 = MC 26.x 动态图集 pass 以 2048x2048（方块图集全尺寸）调 glViewport，污染 grow-only 视口锁存 → 修复 = 锁存候选须"窗口形状"（宽高比偏离 ≤3%，一次性拒绝日志）★ 本轮已移植
- **Task 83**：ApplyFSR 三趟全屏 → 单趟直画 + ≤4px 舍入钳制；FSR 独立化（osm_bridge.mm EASU，zink 等渲染器通用）
- **Task 119/130**：MobileGL 路径 FSR = **桥接层呈现前 EASU+RCAS**（mgl_fsr.mm：EASU 画进默认帧缓冲 = MobileGL 内部 swapchain image → eglSwapBuffers 直呈，零 CPU 回读；RCAS 双 pass；着色器版本适配 + 编译失败自愈回全分辨率）
- **Task 143**：GL_FRAGMENT_SHADER 枚举错写 0x8B92（实为 0x8B30）→ EASU 链静默初始化失败 → "fsr 没有生效" —— 教训：GL 枚举/常量错误表现为静默无效
- **Task 165**：dlsym 补丁致 gl* 解析绕过 MobileGlues 前端 → FSR 钩子全部失联 → 根修 + renderTexture 探针 + RCAS 运行期熔断
- air 的架构差异（重要）：air 是启动器，**直接把 MC 窗口缩到渲染分辨率**（Task 83 联动），桥接层只做呈现前放大；插件不是启动器，缩窗需 FCL 配合（不可达）→ 插件采用 FBO 重定向 + 视口重写（vendored 核内置），这是两端机制不同但目标等价的原因

### 高频方法论
- 双端症状对照：MobileGlues"闪屏且无效"优先查 units 锁存/surface 翻转/双上下文；MobileGL"无效"优先查后端是否 DirectVulkan（无 FSR）+ env 是否到达
- 日志判定锚点（全部 LOG_W_FORCE 强制输出，release 可见）：FSR1 ready / surface latched / redirect active / targets recreated #N / surface identity #N / present context #N / window-units latch rejected (air Task 82)
- 单测环境：系统 JDK21；本地无 Android SDK，构建由 CI 把关

---

## Task 1（2026-09-27）—— FSR 闪屏第四轮：air Task 82 移植 + 决定性遥测 + air MobileGL 方案勘察定论

### 用户输入
"还是闪屏。还有不是让你看我的air都modilegl方案吗，你是不是经常忘记呀，像air写个worklog吧。"
—— 三件事：① MobileGlues 闪屏继续修；② 认真看 air 的 MobileGL FSR 方案（此前多轮仅口头参考未落实）；③ 在仓库里像 air 一样写 worklog（本文件）。

### air 方案勘察（本轮补课，克隆 Gsjsjzhznsz/Air-Minecraft-iOS-Launcher 通读）
1. **air 的 MobileGL FSR = mgl_fsr.mm（Task 119/130）**：桥接层在 gl_swap_buffers 里、eglSwapBuffers 之前，把 MC 画在默认帧缓冲左下半分辨率区域的帧用 EASU+RCAS 放大铺满 → 直呈。门控惰性零锁；GL 符号经 eglGetProcAddress 解析；编译失败一次性回调恢复全分辨率（自愈）。**该机制的前提是"启动器能把 MC 窗口缩到渲染分辨率"**——插件不可达（需 FCL 配合），故插件等价物 = vendored 核 FBO 重定向 + 视口重写（已有），本轮不再重做下发机制。
2. **air Task 82 与插件同构缺陷**：air 的视口锁存 grow-only，被 MC 26.x 图集 pass（2048x2048 方形 viewport）污染后永久腐蚀放大几何（"缩左下角"/分裂/闪屏家族）。air 的修复 = 候选须窗口形状（宽高比偏离 ≤3%）+ 一次性拒绝日志。**插件双端的 units 锁存（glViewport 与 glBlitFramebuffer 两处捕获点）均无此过滤 = 本轮闪屏最高置信根因**。
3. air Task 143 的教训（枚举错值 → 静默无效）与 Task 165（符号解析绕过前端 → 钩子失联）收录速览节备查。

### 本轮落盘（代码）
- **MobileGlues（vendored 核，4 文件）**：
  - FSR1.h/FSR1.cpp：新增 `FSR1_WindowUnitsCandidate(w,h)`（宽高比规则：匹配 surface ≤3% 或匹配已锁存 units ≤3%，一次性拒绝日志）；glViewport 捕获点接入过滤
  - framebuffer.cpp：glBlitFramebuffer 捕获点接入同一过滤
  - egl.cpp presentSurface：新增"呈现上下文"遥测（`[MG] FSR1 present context #N: %p`，首 12 次 + 每 256 次）——直接证明/证伪双上下文交替呈现闪屏假说
  - FSR1.cpp CheckResolutionChange：新增"surface 身份"遥测（`[MG] FSR1 surface identity #N: surface %p now WxH`，首 24 次 + 每 128 次）——直接暴露 surface query 答案交替（去抖只是掩盖）
- **MobileGL（DirectGLES/FSR1.cpp，1 文件）**：同款 `WindowUnitsCandidate` + MapViewport/MapBlitRects 两处捕获点过滤（MGLOG_W 拒绝日志）
- **worklog.md（本文件）**：入库，长期维护

### 用户日志判读（b026f05 = 2026-09-27 18:22 上传，fcl.log + latest.log，运行构建 4e41ac5）
- **后端实锤 = MobileGlues 内核**（"Initializing MobileGlues" + GLES 3.2），FSR preset 4 / 锐化 90 全链到位：`MOBILEGL_FSR1 override applied: 4` → `fsr1Setting = 4` → `FSR1 ready (init #3)` → `redirect active` → `targets recreated #1: surface 2284x1080` → `surface latched: 2284x1080 -> render 1142x540`。**配置链与激活链全部正常，FSR 确实在跑**
- **init #3**：单进程内已有 3 个上下文初始化过 FSR（前两次的行不在日志里，文件起头即第 3 次）——多上下文假说升温，本轮 `present context #N` 遥测专为此设
- **surface 1280x720 → 2284x1080**：FCL 走 TextureView + SDL surface 桥（fcl.log 18:20 "surface ready" / "binding SDL surface"），初始 surface 是 1280x720（FCL 默认），首帧后 query 翻到 2284x1080 真实尺寸——`surface identity #N` 遥测下一轮直接暴露该翻转的频率与指针
- **关键盲区**：latest.log 止步于着色器转换（Shader #596），**闪屏发生时的游戏期日志完全不在文件内**（旧构建的 gameplay 阶段遥测行只有 blit rewrite×2 + latched×1，无 churn 无翻转）——现有证据既不能证明也不能证伪闪屏机制，必须拿新构建在闪屏发生后的日志
- fcl.log 零 FSR 行（正常，FSR 全在 game 侧 latest.log/latest_game.log）

### 验证
- 独立 TU 复刻过滤逻辑 9/9 用例通过：窗口捕获 / 2048² 图集拒绝+日志去重 / 1024² 阴影拒绝 / 窗口形增大接受 / 旋转重捕获 / 早期 dummy surface 回退 / 零值守卫
- 桌面 g++ 全文件语法检查受 vendored Arm 头与 libstdc++ 预存冲突阻断（CI NDK/libc++ 无此问题），以独立 TU + CI 为准
- 推送：MobileGlues → MobileGL → 宿主（子模块先行）；CI 绿后交付 APK

### 下份日志判定表（拿到 latest_game.log 按序判）
1. `FSR1 ready: preset %d ... (init #N, ctx %p)` —— init #2 ⇒ 存在第二上下文；结合 `present context #N` 行：若 #≥3 且交替 ⇒ 双上下文交替呈现实锤（下一步：按 ctx 隔离呈现）
2. `window-units latch rejected (air Task 82): WxH` —— 出现 ⇒ 中间 pass 视口确实在打锁存（本轮过滤已拦住它）；WxH 值指示是哪个 pass（2048x2048=图集）
3. `surface identity #N` 高频交替（尤其 1280x720 ↔ 2284x1080）⇒ surface query 翻转仍在（去抖掩盖了 target 端），需查谁在另建 1280x720 surface
4. `targets recreated #N` 高频 ⇒ 尺寸轴仍在翻（对照 view units 与 blit scale 定位）
5. 无任何 FSR 行 + 后端=DirectVulkan ⇒ 结构性缺失（UI 一键切 GLES 按钮）
6. 全部干净但仍闪 ⇒ 症状性质重新定性（回退疑点：用户装的是旧 APK？核对 `FSR1 surface latched` 行是否存在）

### 遗留 / 下一步
- 内置 FSR1 → Arm ASR 替换（计划不变）；DirectVulkan 原生 FSR（SPIR-V 双管线挂 Present）为下一个功能里程碑
- 悬浮底栏居中；BandQQ 主题 + 液态玻璃完整移植；预测手势补齐

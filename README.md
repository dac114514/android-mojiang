## v1.13 稳定后台版说明

当前版本已升级为 **稳定优先的本地可用闭环版**：

- 支持 TXT 导入、自动分章、本地持久化、范围导出。
- 加料/改写改为 WorkManager 后台队列，每轮小批量处理，降低大章节/多章节导致卡顿或闪退的概率。
- 新增长篇一致性记忆摘要：自动记录近期剧情锚点、角色/称呼候选、前文窗口，处理每章时带入上下文约束。
- 支持失败任务重试、完成任务清理、后台状态查看。
- 当前仍是“本地稳定预览引擎”：没有真正联网调用大模型，模型 API 接入是下一阶段。接入后会用真实模型返回替换本地预览文本。

稳定性策略：

1. 单次导入限制约 300 万字，避免低内存 Android 设备直接崩溃。
2. 后台任务每批最多处理 3 章，避免长时间占用主线程或内存。
3. 所有导入、导出、队列处理均使用异常兜底，失败写入状态，不主动抛到 UI。
4. 本地数据保存为 app 私有目录 JSON，重启后可恢复项目、提示词、队列和记忆摘要。

---

# 墨匠 Rewrite

墨匠 Rewrite 是一个面向 **长篇小说改写 / 扩写 / 加料** 场景的 Android 首版工程，采用 **Jetpack Compose + Material 3** 构建，目标是提供一个现代化、便于 APK 化、适合后续持续增强的移动端工作台。

## 第一版定位

当前版本聚焦于 **现代化 UI 骨架 + 关键工作流入口 + GitHub Actions 自动构建**，优先保证结构清晰、后续可扩展。

### 已覆盖的首版能力
- 现代化首页 Dashboard
- 项目工作台页面
- 加料 / 改写页面
- 模型提供商配置页面
- 提示词模板页面
- 导出页面
- 设置页面
- GitHub Actions 自动构建 Debug APK

### 规划中的后续增强
- DataStore 持久化模型配置与提示词
- Room 存储小说、章节、导出记录
- TXT 导入与自动分章真实实现
- OpenAI 兼容 API 的真实调用
- 长篇上下文记忆与一致性控制
- TXT / EPUB 多格式导出

---

## 项目结构

```text
MojiangRewrite/
├── .github/workflows/android.yml          # GitHub Actions 自动构建 APK
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/java/myapplication/
│       │   ├── MainActivity.kt
│       │   ├── app/
│       │   │   ├── AppDestination.kt
│       │   │   └── MojiangApp.kt
│       │   ├── ui/components/
│       │   ├── ui/model/
│       │   ├── ui/screens/
│       │   └── ui/theme/
│       └── res/
├── gradle/libs.versions.toml
├── build.gradle.kts
├── settings.gradle.kts
└── setup_android_env.sh
```

---

## 技术栈

- **Kotlin**
- **Jetpack Compose**
- **Material 3**
- **Navigation Compose**
- **Gradle Version Catalog**
- 预留：DataStore / Room / 网络层

---

## 本地构建

### 环境要求
- JDK 17+
- Android SDK
- Gradle Wrapper（项目已包含）

### 常用命令

```bash
./gradlew clean
./gradlew assembleDebug
./gradlew assembleRelease
```

Debug APK 默认输出：

```text
app/build/outputs/apk/debug/app-debug.apk
```

---

## GitHub Actions 自动构建

项目已包含：

```text
.github/workflows/android.yml
```

触发方式：
- push 到 `main` / `master`
- pull request 到 `main` / `master`
- 手动触发 `workflow_dispatch`

### 产物获取
构建完成后，在 GitHub Actions 的 Artifacts 中下载：

```text
mojiang-rewrite-debug-apk
```

---

## 你的使用流程

你当前的预期流程可以直接这样走：

1. 我整理好项目代码
2. 项目打包成 zip
3. 你在手机 / Termux 中解压
4. 使用 git 推送到 GitHub
5. GitHub Actions 自动构建 APK
6. 从 Actions 下载 APK 进行安装测试

---

## Termux 推送示例

```bash
pkg install git -y
cd /sdcard/Download
unzip MojiangRewrite-v1.zip -d MojiangRewrite
cd MojiangRewrite
git init
git add .
git commit -m "init mojiang rewrite v1"
git branch -M main
git remote add origin <你的仓库地址>
git push -u origin main
```

---

## 当前限制说明

首版目前属于 **高质量 UI + 架构骨架版**，不是完整功能终版：

- 页面中的按钮多数为占位交互
- 模型连接测试尚未接真实网络请求
- 导入 / 分章 / 导出仍以 UI 与流程设计为主
- 数据层尚未接入持久化数据库

这不是缺陷，而是为了先把 **移动端体验、工程结构、GitHub 构建链路** 打牢，再逐步补真实能力。

---

## 后续推荐优先级

建议第二阶段按这个顺序继续：

1. DataStore：保存模型配置与提示词模板
2. TXT 导入与章节解析
3. 导出 TXT 真正落地
4. OpenAI-compatible API 调用
5. 章节级改写与任务状态管理
6. 长篇一致性控制与记忆系统

---

## 命名说明

- 应用名：**墨匠 Rewrite**
- 工程名：**MojiangRewrite**

如果你后续想更偏中文产品名，也可以改成：
- 墨匠
- 墨匠 Pro
- 墨匠编辑器
- 墨匠小说工坊

---

## 提示

如果你准备继续做第二版，我建议优先补：
- DataStore
- Room
- TXT 文件读写
- API Provider 抽象层
- Prompt Template 管理

这样会从“UI 骨架版”升级为真正可用的“小说加料工作台”。

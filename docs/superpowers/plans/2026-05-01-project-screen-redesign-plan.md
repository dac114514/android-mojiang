# Project Screen Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix status bar overlap by replacing custom Box topBar with Material3 TopAppBar, and redesign the Project screen with import TXT, project switching, chapter search/filter, and reader navigation.

**Architecture:** Two-file change. `MojiangApp.kt` gets the Material3 TopAppBar with conditional actions (project switcher + import button shown only on Project tab). `ProjectScreen.kt` is rewritten to show either an import-empty state or a full project view with summary card, sticky search/filter bar, and filterable chapter list. The import file picker launcher lives in `MainScreen` and is passed down as a callback to avoid duplicating ActivityResult contracts.

**Tech Stack:** Jetpack Compose, Material3, ActivityResultContracts, LocalNovelStore (existing singleton)

---

### Task 1: Replace Box topBar with Material3 TopAppBar

**Files:**
- Modify: `app/src/main/java/com/java/myapplication/app/MojiangApp.kt`

- [ ] **Step 1: Add missing imports in MojiangApp.kt**

Add the following imports to the existing block:
```kotlin
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.ui.platform.LocalContext
```

Note: `TopAppBarDefaults` is already imported (was unused previously). `Icons.Rounded.Add` and `Icons.Rounded.ArrowDropDown` need the material.icons.rounded expanded imports — these are already covered by existing `import androidx.compose.material.icons.rounded.*` imports.

- [ ] **Step 2: Replace the Box topBar with TopAppBar**

Find the existing `topBar = { Box(...) }` block in `MainScreen` and replace it:

```kotlin
topBar = {
    TopAppBar(
        title = { Text("墨匠 Rewrite") },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent
        ),
        windowInsets = TopAppBarDefaults.windowInsets,
        actions = {
            if (selectedTab == AppDestination.Project) {
                ProjectTopAppBarActions(
                    novels = LocalNovelStore.novels,
                    activeNovelId = LocalNovelStore.activeNovelId.value,
                    onSelectNovel = { LocalNovelStore.selectNovel(it) },
                    onImportTxt = { importLauncher.launch(arrayOf("text/plain")) }
                )
            }
        }
    )
},
```

- [ ] **Step 3: Add the import launcher and ProjectTopAppBarActions composable**

Inside `MainScreen`, before the `Box` composable, add:

```kotlin
val context = LocalContext.current
val importLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
) { uri ->
    uri?.let { LocalNovelStore.importTxt(context, it) }
}
```

Add `onImportTxt: () -> Unit` parameter to `MainScreen`'s signature:
```kotlin
private fun MainScreen(
    onNavigateToReader: (Long) -> Unit,
    // add:
    onImportTxt: () -> Unit = {}  // default empty for backward compat
)
```

Then at the bottom of the file (after `MainScreen`), add the helper composable:

```kotlin
@Composable
private fun RowScope.ProjectTopAppBarActions(
    novels: List<NovelProject>,
    activeNovelId: Long?,
    onSelectNovel: (Long) -> Unit,
    onImportTxt: () -> Unit
) {
    val activeNovel = novels.firstOrNull { it.id == activeNovelId }

    if (novels.isNotEmpty()) {
        var expanded by remember { mutableStateOf(false) }
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.height(32.dp)
            ) {
                Text(
                    activeNovel?.title ?: "选择项目",
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1
                )
                Icon(
                    Icons.Rounded.ArrowDropDown,
                    contentDescription = "切换项目",
                    modifier = Modifier.size(18.dp)
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                novels.forEach { novel ->
                    DropdownMenuItem(
                        text = { Text(novel.title) },
                        onClick = {
                            onSelectNovel(novel.id)
                            expanded = false
                        }
                    )
                }
            }
        }
    }

    IconButton(onClick = onImportTxt) {
        Icon(Icons.Rounded.Add, contentDescription = "导入 TXT")
    }
}
```

Wait — `DropdownMenu` needs `import androidx.compose.material3.DropdownMenu`. Let me use `ExposedDropdownMenuBox` pattern instead, but that's a bit heavy for topBar. Actually, a simple `DropdownMenu` with an `OutlinedButton` as anchor is cleaner and more compact for the topBar.

Actually, looking at this more carefully, `DropdownMenu` from material3 is:
```kotlin
import androidx.compose.material3.DropdownMenu
```
Yes, that exists.

But there's an issue: `RowScope` modifier. The `actions` parameter of `TopAppBar` is `@Composable RowScope.() -> Unit`, so the composable should be `@Composable fun RowScope.ProjectTopAppBarActions(...)`.

Let me keep this as a regular composable that wraps its content in a Row if needed. Actually, since it's used inside `actions = { }` which already provides `RowScope`, the function should be `RowScope.ProjectTopAppBarActions`.

- [ ] **Step 4: Add import to DropdownMenu**

Add `import androidx.compose.material3.DropdownMenu` to the imports.

- [ ] **Step 5: Pass onImportTxt to ProjectScreen**

Update the `ProjectScreen` call in `MainScreen`:
```kotlin
AppDestination.Project -> ProjectScreen(
    onNavigateToReader = onNavigateToReader,
    onImportTxt = { importLauncher.launch(arrayOf("text/plain")) }
)
```

Also need to add `statusBarsPadding()` or handle the TopAppBar windowInsets. Since we're using `TopAppBarDefaults.windowInsets`, the TopAppBar will automatically pad the status bar. The `Scaffold`'s `contentWindowInsets = WindowInsets.safeDrawing` handles the content area. No additional padding needed.

Wait, but there's a subtle issue. The `Scaffold` already has `contentWindowInsets = WindowInsets.safeDrawing`, and the TopAppBar inside it also uses `windowInsets`. Are there any conflicts? No — `Scaffold`'s contentWindowInsets pads the inner content, while the TopAppBar's windowInsets pads the topBar itself. They work independently.

- [ ] **Step 6: Remove unused Box import check**

The `Box` import is still needed (used elsewhere in MainScreen), keep it.

---

### Task 2: Rewrite ProjectScreen

**Files:**
- Rewrite: `app/src/main/java/com/java/myapplication/ui/screens/ProjectScreen.kt`

- [ ] **Step 1: Update ProjectScreen function signature**

```kotlin
@Composable
fun ProjectScreen(
    onNavigateToReader: (Long) -> Unit = {},
    onImportTxt: () -> Unit = {}
) {
    // ...
}
```

- [ ] **Step 2: Write the empty state**

When `activeNovel == null`, show a centered import prompt:

```kotlin
val novel = LocalNovelStore.activeNovel

if (novel == null) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Rounded.AutoStories,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Text(
                "尚未导入小说",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "导入 TXT 文件，自动分章",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            Button(onClick = onImportTxt) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                Text("导入 TXT 文件")
            }
        }
    }
    return
}
// From here, novel is smart-cast to non-null
```

Needs imports:
```kotlin
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Button
```

- [ ] **Step 3: Write the project summary card**

When `novel` is not null, show the summary card as the first item:

```kotlin
// Inside the when block where novel != null:
LazyColumn(
    modifier = Modifier.padding(horizontal = 20.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
) {
    // Summary card
    item {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    novel.fileName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "共 ${novel.chapters.size} 章 · ${novel.chapters.sumOf { it.wordCount }} 字",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "已加料 ${novel.chapters.count { it.rewrittenContent.isNotBlank() }} 章 · 待处理 ${novel.chapters.count { it.rewrittenContent.isBlank() }} 章",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                var showDeleteDialog by remember { mutableStateOf(false) }
                if (showDeleteDialog) {
                    AlertDialog(
                        onDismissRequest = { showDeleteDialog = false },
                        title = { Text("删除项目") },
                        text = { Text("确定删除「${novel.title}」吗？章节内容和改文数据将一并删除，此操作不可撤销。") },
                        confirmButton = {
                            Button(onClick = {
                                LocalNovelStore.deleteNovel(novel.id)
                                showDeleteDialog = false
                            }) { Text("删除") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDeleteDialog = false }) { Text("取消") }
                        }
                    )
                }
            }
        }
    }
    // ...
}
```

- [ ] **Step 4: Implement search + filter state**

Add state variables after the `novel` val:

```kotlin
var searchQuery by remember { mutableStateOf("") }
var statusFilter by remember { mutableStateOf("全部") }
```

Add the filtered chapters derivation:

```kotlin
val filteredChapters = remember(novel, searchQuery, statusFilter) {
    novel?.chapters?.filter { chapter ->
        val matchesSearch = searchQuery.isBlank() ||
            chapter.title.contains(searchQuery, ignoreCase = true)
        val matchesStatus = when (statusFilter) {
            "待加料" -> chapter.status == "待加料"
            "已加料" -> chapter.status == "已加料"
            else -> true
        }
        matchesSearch && matchesStatus
    }?.sortedBy { it.index } ?: emptyList()
}
```

- [ ] **Step 5: Add search bar and filter chips**

```kotlin
// After summary card item, before chapter list
item {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("搜索章节名…") },
            singleLine = true,
            leadingIcon = {
                Icon(Icons.Rounded.Search, contentDescription = null)
            }
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("全部", "待加料", "已加料").forEach { label ->
                FilterChip(
                    selected = statusFilter == label,
                    onClick = { statusFilter = label },
                    label = { Text(label) }
                )
            }
        }
    }
}
```

Needs additional imports:
```kotlin
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
```

- [ ] **Step 6: Write the chapter list**

After the search/filter item, add:

```kotlin
items(filteredChapters) { chapter ->
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        onClick = { onNavigateToReader(chapter.id) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "第 ${chapter.index} 章 · ${chapter.title}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "${chapter.wordCount} 字",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DotBadge(
                    text = chapter.status,
                    color = if (chapter.status == "已加料")
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.secondary
                )
                Icon(
                    Icons.Rounded.ChevronRight,
                    contentDescription = "阅读",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
```

- [ ] **Step 7: Add bottom import button**

```kotlin
item {
    OutlinedButton(
        onClick = onImportTxt,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(20.dp))
        Text("添加更多项目")
    }
}
```

- [ ] **Step 8: Add SectionTitle for chapter list**

Before the filtered chapters `items` call, add a section title:

```kotlin
item {
    SectionTitle(
        "章节列表",
        subtitle = if (searchQuery.isNotBlank() || statusFilter != "全部")
            "找到 ${filteredChapters.size} 章" else ""
    )
}
```

---

### Task 3: Verify and resolve imports

- [ ] **Step 1: Check all imports in MojiangApp.kt**

Ensure all new symbols are imported:
- `TopAppBar`, `TopAppBarDefaults` — material3
- `DropdownMenu`, `DropdownMenuItem` — material3
- `rememberLauncherForActivityResult` — activity.compose
- `ActivityResultContracts` — activity.result.contract
- `LocalContext` — compose.ui.platform
- `Icons.Rounded.Add`, `Icons.Rounded.ArrowDropDown` — material.icons.rounded
- `RowScope` — already accessible inside TopAppBar actions

Remove unused:
- `import androidx.compose.material3.SmallTopAppBar` — already removed

- [ ] **Step 2: Check all imports in ProjectScreen.kt**

Ensure all needed imports exist:
```kotlin
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.java.myapplication.data.LocalNovelStore
import com.java.myapplication.ui.components.DotBadge
import com.java.myapplication.ui.components.SectionTitle
```

- [ ] **Step 3: Remove unused imports from ProjectScreen.kt**

Old imports no longer needed (from the previous implementation):
- The old import pattern used `LocalNovelStore.activeNovel` as a property — keep it.
- `import com.java.myapplication.data.LocalNovelStore` — keep.
- Others are covered by Step 2's import list.

---

### Task 4: Commit

```bash
git add app/src/main/java/com/java/myapplication/app/MojiangApp.kt
git add app/src/main/java/com/java/myapplication/ui/screens/ProjectScreen.kt
git commit -m "feat: 修复状态栏遮蔽，重构项目界面支持导入/切换/搜索/筛选"
```

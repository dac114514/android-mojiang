from pathlib import Path
from zipfile import ZipFile, ZIP_DEFLATED

root = Path('/data/user/0/com.ai.assistance.operit/files/workspace/66edb7d6-e181-4207-a871-8d729e9bf196')
out = Path('/storage/emulated/0/Download/MojiangRewrite-v1.13-stable-bg.zip')
out.parent.mkdir(parents=True, exist_ok=True)
if out.exists():
    out.unlink()

skip_dirs = {'.git', '.gradle', 'build'}
files = []
for p in root.rglob('*'):
    if not p.is_file():
        continue
    rel = p.relative_to(root)
    if any(part in skip_dirs for part in rel.parts):
        continue
    if len(rel.parts) >= 2 and rel.parts[0] == 'app' and rel.parts[1] == 'build':
        continue
    files.append((p, rel))

with ZipFile(out, 'w', ZIP_DEFLATED) as z:
    for p, rel in files:
        z.write(p, rel.as_posix())

print(f'ZIP_OK {out} {out.stat().st_size} bytes files={len(files)}')
with ZipFile(out) as z:
    names = set(z.namelist())
    required = [
        'app/src/main/java/com/java/myapplication/data/LocalNovelStore.kt',
        'app/src/main/java/com/java/myapplication/worker/RewriteWorker.kt',
        'app/src/main/java/com/java/myapplication/ui/screens/RewriteScreen.kt',
        'gradle/libs.versions.toml',
        'app/build.gradle.kts',
        'README.md',
        'DELIVERY_CHECKLIST.md',
    ]
    for name in required:
        print(('HAS ' if name in names else 'MISS ') + name)

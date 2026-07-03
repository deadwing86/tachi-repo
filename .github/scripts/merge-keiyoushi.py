import json
import os
import subprocess
import urllib.request
from pathlib import Path

PKGS = [
    "eu.kanade.tachiyomi.extension.all.hentai3",
    "eu.kanade.tachiyomi.extension.all.cosplaytele",
    "eu.kanade.tachiyomi.extension.all.hentaiera",
    "eu.kanade.tachiyomi.extension.all.hentaifox",
    "eu.kanade.tachiyomi.extension.all.hentaihand",
    "eu.kanade.tachiyomi.extension.all.hentaizap",
    "eu.kanade.tachiyomi.extension.all.nhentaicom",
    "eu.kanade.tachiyomi.extension.all.nhentaixxx",
    "eu.kanade.tachiyomi.extension.all.photos18",
    "eu.kanade.tachiyomi.extension.all.xarthunter",
    "eu.kanade.tachiyomi.extension.all.webtoons",
    "eu.kanade.tachiyomi.extension.en.ninehentai",
    "eu.kanade.tachiyomi.extension.en.eighteenporncomic",
    "eu.kanade.tachiyomi.extension.en.allporncomic",
    "eu.kanade.tachiyomi.extension.en.aquamanga",
    "eu.kanade.tachiyomi.extension.en.hentaidex",
    "eu.kanade.tachiyomi.extension.en.hentairead",
    "eu.kanade.tachiyomi.extension.en.infinityscans",
    "eu.kanade.tachiyomi.extension.en.madaradex",
    "eu.kanade.tachiyomi.extension.en.mangahentai",
    "eu.kanade.tachiyomi.extension.en.mangafox",
    "eu.kanade.tachiyomi.extension.en.mangatx",
    "eu.kanade.tachiyomi.extension.en.manhuaplus",
    "eu.kanade.tachiyomi.extension.en.manhuafast",
    "eu.kanade.tachiyomi.extension.en.manhuaplusorg",
    "eu.kanade.tachiyomi.extension.en.toonily",
    "eu.kanade.tachiyomi.extension.en.toonilyme",
    "eu.kanade.tachiyomi.extension.vi.goctruyentranh",
    "eu.kanade.tachiyomi.extension.vi.hentaivnplus",
    "eu.kanade.tachiyomi.extension.vi.nettruyenco",
    "eu.kanade.tachiyomi.extension.vi.truyenhentai18",
]

KEIYOUSHI_BASE = "https://github.com/keiyoushi/extensions/raw/repo"
REPO_DIR = Path("repo")
APK_DIR = REPO_DIR / "apk"
ICON_DIR = REPO_DIR / "icon"
APK_DIR.mkdir(parents=True, exist_ok=True)
ICON_DIR.mkdir(parents=True, exist_ok=True)

# Get cached files from repo branch
result = subprocess.run(
    ["git", "ls-tree", "-r", "origin/repo", "--name-only"],
    capture_output=True, text=True
)
cached_files = set(result.stdout.splitlines())


def download(url, dest):
    try:
        urllib.request.urlretrieve(url, dest)
        return True
    except Exception as e:
        print(f"WARN: Failed to download {url}: {e}")
        return False


def restore_from_cache(path):
    result = subprocess.run(
        ["git", "show", f"origin/repo:{path}"],
        capture_output=True
    )
    if result.returncode == 0:
        destination = REPO_DIR / path
        destination.parent.mkdir(parents=True, exist_ok=True)
        destination.write_bytes(result.stdout)
        return True
    return False


# Fetch keiyoushi index
print("Fetching keiyoushi index...")
with urllib.request.urlopen(
    "https://raw.githubusercontent.com/keiyoushi/extensions/repo/index.min.json"
) as r:
    keiyoushi_index = json.load(r)

keiyoushi_by_pkg = {e["pkg"]: e for e in keiyoushi_index}

extra_entries = []

for pkg in PKGS:
    entry = keiyoushi_by_pkg.get(pkg)

    if entry is None:
        # Not in keiyoushi — try to restore from our cache
        cached = next((f for f in cached_files if f.startswith(f"apk/{pkg.split('.')[-1]}")), None)
        # More precise: find by pkg prefix in filename
        cached = next(
            (f for f in cached_files
             if f.startswith("apk/") and pkg.split(".")[-1] in f),
            None
        )
        if cached:
            apk_name = cached.split("/")[-1]
            if restore_from_cache(cached):
                print(f"Restored from cache: {apk_name}")
                icon_path = f"icon/{pkg}.png"
                if icon_path in cached_files:
                    restore_from_cache(icon_path)
            else:
                print(f"WARN: Could not restore {pkg}, skipping")
                continue
        else:
            print(f"WARN: {pkg} not found anywhere, skipping")
            continue
    else:
        apk = entry["apk"]
        apk_dest = APK_DIR / apk
        icon_dest = ICON_DIR / f"{pkg}.png"

        # APK: try cache first, then download
        if f"apk/{apk}" in cached_files and restore_from_cache(f"apk/{apk}"):
            print(f"Cached: {apk}")
        elif download(f"{KEIYOUSHI_BASE}/apk/{apk}", apk_dest):
            print(f"Downloaded: {apk}")
        else:
            print(f"WARN: Could not get {apk}, skipping")
            continue

        # Icon: try cache first, then download
        icon_cache = f"icon/{pkg}.png"
        if icon_cache in cached_files and restore_from_cache(icon_cache):
            pass
        else:
            download(f"{KEIYOUSHI_BASE}/icon/{pkg}.png", icon_dest)

    extra_entries.append(entry)

# Merge into index.min.json
index_path = REPO_DIR / "index.min.json"
with open(index_path, encoding="utf-8") as f:
    base_index = json.load(f)

merged = base_index + extra_entries

with open(index_path, "w", encoding="utf-8") as f:
    json.dump(merged, f, ensure_ascii=False, separators=(",", ":"))

print(f"Final index count: {len(merged)}")

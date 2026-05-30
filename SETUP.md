# Setup Guide

## 1. Generate a signing keystore

Run this once on your machine (requires Java/keytool):

```bash
keytool -genkey -v \
  -keystore signingkey.jks \
  -alias tachiyomi \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
```

Then base64-encode it:
```bash
# Linux/Mac
base64 signingkey.jks

# Windows PowerShell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("signingkey.jks"))
```

## 2. Add GitHub repository secrets

Go to your repo → **Settings → Secrets and variables → Actions → New repository secret**

| Secret name | Value |
|---|---|
| `SIGNING_KEY` | base64 output from step 1 |
| `ALIAS` | `tachiyomi` (or whatever alias you used) |
| `KEY_STORE_PASSWORD` | password you set for the keystore |
| `KEY_PASSWORD` | password you set for the key |

## 3. Push the repo

```powershell
cd D:\DG\tachi-repo
git init
git add .
git commit -m "Initial commit"
gh repo create tachi-repo --public --push
```

## 4. Enable GitHub Pages

- Go to repo **Settings → Pages**
- Source: **Deploy from a branch**
- Branch: **repo** / root

## 5. Add to Mihon

Browse → Extension repos → Add:
```
https://raw.githubusercontent.com/deadwing86/tachi-repo/repo/index.min.json
```

## Adding more extensions

1. Copy source folder into `src/<lang>/<name>/`
2. Push to main — GitHub Actions builds and deploys automatically

## Current extensions

- `src/all/hitomi` — Hitomi.la (recovered from keiyoushi git history, version 41)

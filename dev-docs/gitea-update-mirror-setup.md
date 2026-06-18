# Gitea 更新鏡像設定手冊

本文說明如何把更新發布流程從 GitCode 改成 Gitea，並保留 GitHub + Gitee + Gitea 三路 manifest。

## 1. 清除 GitCode 設定

到 GitHub repository 的 `Settings -> Secrets and variables -> Actions`：

- 刪除 secret：`GITCODE_TOKEN`
- 刪除 secret：`GITCODE_SSH_PRIVATE_KEY`
- 若有 GitCode 相關 variable，也一併刪除

目前 workflow 不再讀取任何 `GITCODE_*` 變數或 secret。

## 2. 建立 Gitea 鏡像 repo

在 Gitea 建立公開 repo：

- 建議 owner：`LittleSurvival`
- 建議 repo：`ymb-apk-release`
- 預設分支：`main`
- Repo 用途：只放 `update/manifest.json`、`update/stable.json` 與 release APK assets

如果使用 gitea.com，app 內建 manifest URL 是：

```text
https://gitea.com/LittleSurvival/ymb-apk-release/raw/branch/main/update/stable.json
```

如果改用自建 Gitea，需同步修改：

- `shared/src/commonMain/kotlin/me/thenano/yamibo/yamibo_app/repository/appupdate/DefaultAppUpdateRepository.kt`
- GitHub Actions variable：`GITEA_BASE_URL`

## 3. 建立 Gitea token

在 Gitea 使用者設定建立 access token，權限至少需要：

- repository read/write
- release read/write

在 GitHub Actions 新增 secret：

```text
GITEA_TOKEN=<Gitea access token>
```

## 4. 建立 SSH deploy key

先確認 Windows 已安裝 OpenSSH Client：

```powershell
ssh-keygen -V
```

若指令不存在，到 `Windows 設定 -> 系統 -> 選用功能` 安裝 `OpenSSH Client`。

在本機產生一組專用 SSH key。這組 key 只供 GitHub Actions 推送 Gitea 鏡像，不要和個人 SSH key 共用：

```powershell
ssh-keygen -t ed25519 -C "yamibo-gitea-release-bot" -f "$env:USERPROFILE\.ssh\yamibo_gitea_release"
```

執行後會詢問 passphrase。GitHub Actions 無法互動輸入，因此這組專用 deploy key 的 passphrase 請留空，直接按兩次 Enter。

指令會產生兩個檔案：

```text
C:\Users\<你的 Windows 使用者名稱>\.ssh\yamibo_gitea_release      # private key，不可公開
C:\Users\<你的 Windows 使用者名稱>\.ssh\yamibo_gitea_release.pub  # public key，可加到 Gitea
```

如果 `yamibo_gitea_release` 已經存在，先確認它是否仍被使用；不要直接覆寫，否則 Gitea 上既有 deploy key 會立即失效。

把 public key 加到 Gitea repo：

```powershell
Get-Content -Path "$env:USERPROFILE\.ssh\yamibo_gitea_release.pub" -Encoding UTF8
```

Gitea repo 設定：

- `Settings -> Deploy Keys`
- 新增 public key
- 勾選 write access

接著取得 private key。private key 是沒有 `.pub` 副檔名的檔案：

```powershell
Get-Content -Path "$env:USERPROFILE\.ssh\yamibo_gitea_release" -Raw -Encoding UTF8
```

輸出應該包含完整的多行內容：

```text
-----BEGIN OPENSSH PRIVATE KEY-----
...
-----END OPENSSH PRIVATE KEY-----
```

也可以直接複製到 Windows 剪貼簿，避免漏掉換行：

```powershell
Get-Content -Path "$env:USERPROFILE\.ssh\yamibo_gitea_release" -Raw -Encoding UTF8 | Set-Clipboard
```

到 GitHub repository 的 `Settings -> Secrets and variables -> Actions -> New repository secret`：

- Name：`GITEA_SSH_PRIVATE_KEY`
- Secret：貼上剛才複製的完整 private key，包括開頭、結尾與所有換行

不要把 `.pub` 公鑰填進此 Secret，也不要把 private key 上傳到 Gitea、commit 到 git，或貼到 issue/log。最終設定關係如下：

```text
Gitea Deploy Key             <- yamibo_gitea_release.pub（public key）
GitHub GITEA_SSH_PRIVATE_KEY <- yamibo_gitea_release（private key）
```

## 5. 設定 GitHub Actions variables

如果使用 gitea.com，可以不設定 variables，workflow 會使用預設值。

自建 Gitea 需要設定：

```text
GITEA_BASE_URL=https://gitea.example.com
GITEA_SSH_HOST=gitea.example.com
GITEA_SSH_PORT=22
```

`GITEA_SSH_PORT` 若不是 22，workflow 會使用 `ssh://git@host:port/owner/repo.git` remote。

## 6. 需要保留的 GitHub/Gitee secrets

GitHub release 仍使用：

```text
GITHUB_TOKEN
MINE_KEYSTORE_BASE64
MINE_KEY_ALIAS
MINE_KEYSTORE_PASSWORD
MINE_KEY_PASSWORD
```

Gitee 鏡像仍使用：

```text
GITEE_TOKEN
GITEE_SSH_PRIVATE_KEY
```

## 7. 驗證流程

先在本機確認 manifest 一致：

```powershell
.\gradlew syncStableManifest validateUpdateManifest --console=plain
```

GitHub Actions 可用兩種方式驗證：

- 推 tag 觸發 `Release Android APK`
- 手動執行 `Sync Update Folder To Mirrors`

如果只是同步 not-ready manifest，執行 `Sync Update Folder To Mirrors` 並保持 `use_latest_release_asset=false`。

如果要從既有 GitHub release 產生 ready manifest，執行：

```text
Sync Update Folder To Mirrors -> use_latest_release_asset=true
```

成功後檢查三個來源：

- GitHub `update-release` branch 的 `update/stable.json`
- Gitee repo `main` branch 的 `update/stable.json`
- Gitea repo `main` branch 的 `update/stable.json`

Gitea ready manifest 的 `assets[0].url` 必須指向 Gitea release asset，不能指回 GitHub。

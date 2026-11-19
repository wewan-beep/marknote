# 把 docs-site 推到 GitHub，交给 Actions 发布到 GitHub Pages
# 用法：
#   $env:GITHUB_TOKEN='ghp_xxx'; .\deploy-github.ps1           # 用 token 推送
#   .\deploy-github.ps1 -LoginOnly                             # 只是拉起浏览器登录（Git Credential Manager）
param(
  [string]$Repo = 'wewan-beep/marknote',
  [string]$Branch = 'master',
  [string]$Message = 'docs: 更新 VitePress 文档站',
  [switch]$LoginOnly
)

$ErrorActionPreference = 'Stop'

# 仓库根 = docs-site 的上一级
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

if ($LoginOnly) {
  git -c credential.helper=manager ls-remote "https://github.com/$Repo.git" | Out-Null
  Write-Host '如果弹出浏览器，登录 GitHub 后凭据会被 GCM 保存，后续 push 不再要密码。'
  exit 0
}

git add docs-site .github/workflows/deploy-docs.yml
git commit -m $Message

$token = $env:GITHUB_TOKEN
if ($token) {
  $url = "https://x-access-token:$token@github.com/$Repo.git"
  git -c credential.helper= push $url "HEAD:$Branch"
} else {
  git push "https://github.com/$Repo.git" "HEAD:$Branch"
}

Write-Host ''
Write-Host '推送完成。接下来在 GitHub 上做一次设置：'
Write-Host "  1. 打开 https://github.com/$Repo/settings/pages"
Write-Host '  2. Source 选 “GitHub Actions”（只需点一次）'
Write-Host '  3. 到 Actions 页面看 “Deploy docs site” 跑完，站点就在 https://wewan-beep.github.io/marknote/'

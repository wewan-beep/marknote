# MarkNote 文档站（VitePress）

MarkNote 项目的官网 + 文档站，中英双语，深色科技风，可切浅色。内容与 `marknote/` 工程保持一致，图与代码都取自真实实现。

## 开发

```bash
cd marknote-site
npm install
npm run dev      # http://localhost:5173
```

## 构建

```bash
npm run build    # 产物在 docs/.vitepress/dist
npm run preview  # 本地预览构建结果
npm run serve    # 构建后固定 4173 端口预览
```

## 部署到 GitHub Pages

站点放在本仓库的 `docs-site/` 目录，发布由仓库根目录的 `.github/workflows/deploy-docs.yml` 完成：push 到 `master` 后 Actions 自动 `npm ci` + `npm run build`（`BASE_PATH=/<仓库名>/`）再部署到 Pages。

地址：`https://wewan-beep.github.io/marknote/`

首次需要在仓库 Settings → Pages 把 Source 选成 **GitHub Actions**（workflow 里的 `configure-pages` 已带 `enablement: true`，多数情况下会自动开）。

一次推送：

```powershell
cd marknote/docs-site
$env:GITHUB_TOKEN='ghp_xxx'      # classic token，勾 repo + workflow
.\deploy-github.ps1
```

没有 token 就让 Git Credential Manager 弹浏览器登录：

```powershell
.\deploy-github.ps1 -LoginOnly
git push https://github.com/wewan-beep/marknote.git master
```

## 部署（自建服务器）

产物是纯静态文件，丢给任意静态服务器即可。Nginx（部署在域名根路径）：

```nginx
server {
    listen 80;
    server_name notes-docs.example.com;
    root /usr/share/nginx/html/marknote-site;
    index index.html;
    location / { try_files $uri $uri/ /index.html; }
}
```

### 部署到子路径（/marknote/）

GitHub Pages、Gitee Pages 这类项目站点都在子路径下（`https://<user>.github.io/marknote/`），必须让 `base` 与之一致，否则 CSS / JS / 图片全是 404。

`base` 通过环境变量 `BASE_PATH` 注入，默认 `/`（本地 dev、根路径部署都不受影响）：

```powershell
# Windows PowerShell
$env:BASE_PATH='/marknote/'; npm run build
```

```bash
# macOS / Linux
BASE_PATH=/marknote/ npm run build
```

也可以直接用现成脚本（Windows）：

```bash
npm run build:sub     # 等价于 set BASE_PATH=/marknote/ && vitepress build docs
npm run serve:sub     # 子路径预览 http://localhost:4174/marknote/
```

部署后把 `docs/.vitepress/dist` 里的**内容**（不是 dist 目录本身）上传到 `.../marknote/` 下即可。

Nginx 子路径写法：

```nginx
location /marknote/ {
    alias /usr/share/nginx/html/marknote-site/;
    try_files $uri $uri/ /marknote/index.html;
}
```

### 关于 base 的三条约定

1. `themeConfig.logo` 只能写 `/logo.svg`，VitePress 会自动加 base；写成 `${BASE}logo.svg` 会变成 `/marknote/marknote/logo.svg`。
2. 组件里的静态资源必须走 `withBase('/images/x.png')`（见 `HomeHero`、`ScreenshotWall`、`MpFollow` 等）。
3. Markdown 里的图片写 `![x](/images/x.png)` 会被自动加 base；但**裸 HTML 的 `href` 不会被处理**，站内链接要写成相对路径（如 `href="./guide/quick-start"`）。

## 目录

```
marknote-site/
├── package.json
└── docs/
    ├── .vitepress/
    │   ├── config.mts                 # 站点配置、导航、侧边栏、i18n、base
    │   ├── theme/
    │   │   ├── index.ts               # 注册全局组件
    │   │   ├── Layout.vue             # 套 Layout，挂文档页底部的关注带
    │   │   ├── custom.css             # 深色科技风 + html:not(.dark) 浅色覆盖
    │   │   └── components/
    │   │       ├── HomeHero.vue       # 首屏：光斑、轨道环、数字滚动、倾斜截图卡
    │   │       ├── TechMarquee.vue    # 技术栈跑马灯
    │   │       ├── FeatureGrid.vue    # 六能力卡片（SVG 图标，无 emoji）
    │   │       ├── ThemeLab.vue       # 10 套主题可点预览
    │   │       ├── PipelineFlow.vue   # 请求链路四节点
    │   │       ├── ScreenshotWall.vue # 界面墙 + 全屏 lightbox
    │   │       ├── CommandBlock.vue   # 带复制按钮的命令块
    │   │       ├── CopyAnatomy.vue    # 复制流水线六步
    │   │       ├── MpFollow.vue       # 首页公众号推广区
    │   │       ├── MpFooterBand.vue   # 文档页底部关注带
    │   │       └── MpPromoPopup.vue   # 首页首次进入浮层（可不再提示）
    ├── public/
    │   ├── logo.svg
    │   └── images/                    # 6 张界面图 + 公众号二维码/搜一搜物料
    ├── index.md                       # 中文首页
    ├── wechat.md                      # 中文公众号页
    ├── guide/                         # 中文文档：quick-start / deploy / architecture / features / faq
    └── en/
        ├── index.md                   # English home
        ├── wechat.md                  # English WeChat page
        └── guide/                     # English docs
```

## 加一篇文档

1. 在 `docs/guide/` 新建 `xxx.md`，写 `# 标题`
2. 在 `docs/.vitepress/config.mts` 的 `zhSidebar['/guide/']` 里加一条 `{ text, link }`
3. 英文版同样在 `docs/en/guide/xxx.md` 写一份，并在 `enSidebar` 里加一条

首页自定义区块直接在 `index.md` 里用已注册的全局组件（`HomeHero`、`ThemeLab` 等）。

## 设计说明

- 深色是默认外观（`appearance: 'dark'`），强调色分层：cyan `#22d3ee` / blue `#4f7cff` / violet `#a78bfa`
- 全局背景用 fixed 网格 + 三处径向光晕，`mask-image` 让网格在顶部渐隐
- 卡片、代码块统一 1px 半透明描边 + hover 微光，动效只用 transform / opacity
- 所有交互组件零外部依赖，不加载远程字体或 CDN，离线可跑
- 浅色主题集中在 `custom.css` 末尾的 `html:not(.dark)` 区块，靠覆盖变量 + 少量硬编码点

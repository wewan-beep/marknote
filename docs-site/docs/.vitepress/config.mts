import { defineConfig } from 'vitepress'

// 子路径部署用：$env:BASE_PATH='/marknote/' 后再构建；默认根路径 /
const BASE = process.env.BASE_PATH || '/'

const zhNav = [
  { text: '快速开始', link: '/guide/quick-start' },
  { text: '详细部署', link: '/guide/deploy' },
  { text: '系统架构', link: '/guide/architecture' },
  { text: '功能详解', link: '/guide/features' },
  { text: 'FAQ', link: '/guide/faq' },
  { text: '公众号', link: '/wechat' }
]

const enNav = [
  { text: 'Quick Start', link: '/en/guide/quick-start' },
  { text: 'Deployment', link: '/en/guide/deploy' },
  { text: 'Architecture', link: '/en/guide/architecture' },
  { text: 'Features', link: '/en/guide/features' },
  { text: 'FAQ', link: '/en/guide/faq' },
  { text: 'WeChat', link: '/en/wechat' }
]

const zhSidebar = {
  '/guide/': [
    {
      text: '开始使用',
      items: [
        { text: '快速开始', link: '/guide/quick-start' },
        { text: '详细部署', link: '/guide/deploy' }
      ]
    },
    {
      text: '原理与实现',
      items: [
        { text: '系统架构', link: '/guide/architecture' },
        { text: '功能详解', link: '/guide/features' },
        { text: '常见问题', link: '/guide/faq' }
      ]
    }
  ]
}

const enSidebar = {
  '/en/guide/': [
    {
      text: 'Getting Started',
      items: [
        { text: 'Quick Start', link: '/en/guide/quick-start' },
        { text: 'Deployment', link: '/en/guide/deploy' }
      ]
    },
    {
      text: 'How It Works',
      items: [
        { text: 'Architecture', link: '/en/guide/architecture' },
        { text: 'Features', link: '/en/guide/features' },
        { text: 'FAQ', link: '/en/guide/faq' }
      ]
    }
  ]
}

export default defineConfig({
  title: 'MarkNote',
  description: '仿 mdnice 的 Markdown 笔记系统 · Vue3 + Spring Boot 3 + MySQL + MinIO',
  lang: 'zh-CN',
  // 部署在子路径（如 GitHub/Gitee Pages 的 /marknote/）时用环境变量注入：
  //   PowerShell: $env:BASE_PATH='/marknote/'; npm run build
  // 不设置时默认根路径 /，本地 dev / preview 不受影响
  base: BASE,
  appearance: 'dark',
  cleanUrls: true,
  lastUpdated: true,
  // 文档里出现的本机地址不是站内链接，忽略掉
  ignoreDeadLinks: [/^http:\/\/localhost:/, /^http:\/\/127\.0\.0\.1:/],
  head: [
    ['link', { rel: 'icon', type: 'image/svg+xml', href: `${BASE}logo.svg` }],
    ['meta', { name: 'theme-color', content: '#05070d' }],
    ['meta', { property: 'og:type', content: 'website' }],
    ['meta', { property: 'og:title', content: 'MarkNote — Markdown 笔记 + 公众号排版' }],
    ['meta', { property: 'og:description', content: '写完即发布：左写右排，一键复制到公众号，格式不掉。' }]
  ],
  locales: {
    root: {
      label: '简体中文',
      lang: 'zh-CN',
      themeConfig: {
        nav: zhNav,
        sidebar: zhSidebar,
        outline: { level: [2, 3], label: '本页导航' },
        docFooter: { prev: '上一篇', next: '下一篇' },
        returnToTopLabel: '回到顶部',
        sidebarMenuLabel: '目录',
        darkModeSwitchLabel: '主题',
        lastUpdatedText: '最后更新',
        footer: {
          message: '基于 MIT 协议开源 · 可自由二次开发',
          copyright: 'Copyright © 2026 MarkNote'
        }
      }
    },
    en: {
      label: 'English',
      lang: 'en-US',
      link: '/en/',
      themeConfig: {
        nav: enNav,
        sidebar: enSidebar,
        outline: { level: [2, 3], label: 'On this page' },
        docFooter: { prev: 'Previous', next: 'Next' },
        returnToTopLabel: 'Back to top',
        sidebarMenuLabel: 'Menu',
        darkModeSwitchLabel: 'Appearance',
        lastUpdatedText: 'Last updated',
        footer: {
          message: 'Open source under MIT · Free for commercial use',
          copyright: 'Copyright © 2026 MarkNote'
        }
      }
    }
  },
  themeConfig: {
    // 注意：themeConfig.logo 由 VitePress 自动加 base，这里必须写根路径，否则子路径下会变成 /marknote/marknote/
    logo: '/logo.svg',
    siteTitle: 'MarkNote',
    socialLinks: [{ icon: 'github', link: 'https://github.com/wewan-beep/marknote' }],
    search: {
      provider: 'local'
    },
    editLink: undefined
  }
})

import type { RouteRecordRaw } from 'vue-router';

import { BasicLayout } from '#/layouts';

const routes: RouteRecordRaw[] = [
  {
    component: BasicLayout,
    meta: {
      authority: ['super_admin', 'content_admin'],
      icon: 'lucide:network',
      order: 95,
      title: '站点结构',
    },
    name: 'SiteStructureRoot',
    path: '/site-structure-root',
    children: [
      {
        component: () => import('#/views/system/site-structure.vue'),
        meta: { icon: 'lucide:panel-top', title: '导航与页面首屏' },
        name: 'SiteStructure',
        path: '/system/site-structure',
      },
    ],
  },
  {
    component: BasicLayout,
    meta: {
      authority: ['super_admin', 'content_admin', 'inquiry_admin'],
      icon: 'lucide:messages-square',
      order: 70,
      title: '互动管理',
    },
    name: 'Interaction',
    path: '/interaction',
    children: [
      {
        component: () => import('#/views/interaction/inquiries.vue'),
        meta: { icon: 'lucide:message-square-more', title: '询盘管理' },
        name: 'Inquiries',
        path: '/interaction/inquiries',
      },
    ],
  },
  {
    component: BasicLayout,
    meta: {
      authority: ['super_admin', 'content_admin'],
      icon: 'lucide:images',
      order: 80,
      title: '素材库',
    },
    name: 'MediaRoot',
    path: '/media-root',
    children: [
      {
        component: () => import('#/views/media/index.vue'),
        meta: { icon: 'lucide:library-big', title: '文件与图片' },
        name: 'Media',
        path: '/media',
      },
    ],
  },
  {
    component: BasicLayout,
    meta: {
      authority: ['super_admin', 'content_admin'],
      icon: 'lucide:search-check',
      order: 90,
      title: 'SEO 管理',
    },
    name: 'SeoRoot',
    path: '/seo-root',
    children: [
      {
        component: () => import('#/views/seo/index.vue'),
        meta: { icon: 'lucide:scan-search', title: 'SEO 与重定向' },
        name: 'Seo',
        path: '/seo',
      },
    ],
  },
  {
    component: BasicLayout,
    meta: {
      authority: ['super_admin'],
      icon: 'lucide:settings-2',
      order: 100,
      title: '系统管理',
    },
    name: 'SystemRoot',
    path: '/system-root',
    children: [
      {
        component: () => import('#/views/system/settings.vue'),
        meta: {
          authority: ['super_admin'],
          icon: 'lucide:sliders-horizontal',
          title: '设置与权限',
        },
        name: 'Settings',
        path: '/system/settings',
      },
    ],
  },
];

export default routes;

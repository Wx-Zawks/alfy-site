import type { RouteRecordRaw } from 'vue-router';

import { BasicLayout } from '#/layouts';

const manager = () => import('#/views/content/manager.vue');

const routes: RouteRecordRaw[] = [
  {
    component: BasicLayout,
    meta: { authority: ['super_admin', 'content_admin'], icon: 'lucide:panels-top-left', order: 10, title: '首页管理' },
    name: 'HomeContent',
    path: '/home-content',
    children: [
      { component: manager, meta: { icon: 'lucide:gallery-horizontal', title: 'Banner' }, name: 'Banners', path: '/home-content/banners', props: { resource: 'banners' } },
    ],
  },
  {
    component: BasicLayout,
    meta: { authority: ['super_admin', 'content_admin'], icon: 'lucide:package-open', order: 20, title: '产品管理' },
    name: 'ProductContent',
    path: '/content',
    children: [
      { component: manager, meta: { icon: 'lucide:package', title: '产品列表' }, name: 'Products', path: '/content/products', props: { resource: 'products' } },
    ],
  },
  {
    component: BasicLayout,
    meta: { authority: ['super_admin', 'content_admin'], icon: 'lucide:briefcase-business', order: 30, title: '应用与案例' },
    name: 'Applications',
    path: '/applications',
    children: [
      { component: manager, meta: { icon: 'lucide:blocks', title: '应用场景' }, name: 'Scenes', path: '/applications/scenes', props: { resource: 'scenes' } },
      { component: manager, meta: { icon: 'lucide:badge-check', title: '案例管理' }, name: 'Cases', path: '/applications/cases', props: { resource: 'cases' } },
    ],
  },
  {
    component: BasicLayout,
    meta: { authority: ['super_admin', 'content_admin'], icon: 'lucide:flask-conical', order: 40, title: '技术管理' },
    name: 'TechnologyContent',
    path: '/technology-content',
    children: [
      { component: manager, meta: { icon: 'lucide:microscope', title: '技术内容' }, name: 'Technologies', path: '/technology-content/list', props: { resource: 'technologies' } },
    ],
  },
  {
    component: BasicLayout,
    meta: { authority: ['super_admin', 'content_admin'], icon: 'lucide:newspaper', order: 50, title: '新闻管理' },
    name: 'NewsContent',
    path: '/news-content',
    children: [
      { component: manager, meta: { icon: 'lucide:file-pen-line', title: '新闻文章' }, name: 'Articles', path: '/content/articles', props: { resource: 'articles' } },
    ],
  },
  {
    component: BasicLayout,
    meta: { authority: ['super_admin', 'content_admin'], icon: 'lucide:handshake', order: 60, title: '合作与单页' },
    name: 'PagesContent',
    path: '/pages-content',
    children: [
      { component: manager, meta: { icon: 'lucide:building-2', title: '合作伙伴' }, name: 'Partners', path: '/pages-content/partners', props: { resource: 'partners' } },
      { component: manager, meta: { icon: 'lucide:file-text', title: '单页管理' }, name: 'Pages', path: '/pages-content/pages', props: { resource: 'pages' } },
    ],
  },
];

export default routes;

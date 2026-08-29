import type { RouteRecordRaw } from 'vue-router';

import { BasicLayout } from '#/layouts';

const manager = () => import('#/views/content/manager.vue');
const homeSections = () => import('#/views/content/home-sections.vue');
const homeCases = () => import('#/views/content/home-cases.vue');
const homeNews = () => import('#/views/content/home-news.vue');
const caseCategories = () => import('#/views/content/case-categories.vue');
const productCategories = () =>
  import('#/views/content/product-categories.vue');
const technologyPages = () => import('#/views/content/technology-pages.vue');
const cooperationPages = () => import('#/views/content/cooperation-pages.vue');

const routes: RouteRecordRaw[] = [
  {
    component: BasicLayout,
    meta: {
      authority: ['super_admin', 'content_admin'],
      icon: 'lucide:panels-top-left',
      order: 10,
      title: '首页管理',
    },
    name: 'HomeContent',
    path: '/home-content',
    children: [
      {
        component: homeSections,
        meta: { icon: 'lucide:layout-template', title: '首页内容配置' },
        name: 'HomeSections',
        path: '/home-content/sections',
      },
      {
        component: manager,
        meta: { icon: 'lucide:gallery-horizontal', title: 'Banner' },
        name: 'Banners',
        path: '/home-content/banners',
        props: { resource: 'banners' },
      },
      {
        component: homeCases,
        meta: { icon: 'lucide:pin', title: '典型案例展示' },
        name: 'HomeCases',
        path: '/home-content/cases',
      },
      {
        component: homeNews,
        meta: { icon: 'lucide:newspaper', title: '新闻资讯展示' },
        name: 'HomeNews',
        path: '/home-content/news',
      },
    ],
  },
  {
    component: BasicLayout,
    meta: {
      authority: ['super_admin', 'content_admin'],
      icon: 'lucide:package-open',
      order: 20,
      title: '产品管理',
    },
    name: 'ProductContent',
    path: '/content',
    children: [
      {
        component: productCategories,
        meta: { icon: 'lucide:tags', title: '产品分类' },
        name: 'ProductCategories',
        path: '/content/product-categories',
      },
      {
        component: manager,
        meta: { icon: 'lucide:package', title: '产品列表' },
        name: 'Products',
        path: '/content/products',
        props: { resource: 'products' },
      },
    ],
  },
  {
    component: BasicLayout,
    meta: {
      authority: ['super_admin', 'content_admin'],
      icon: 'lucide:briefcase-business',
      order: 30,
      title: '应用与案例',
    },
    name: 'Applications',
    path: '/applications',
    children: [
      {
        component: manager,
        meta: { icon: 'lucide:blocks', title: '应用场景' },
        name: 'Scenes',
        path: '/applications/scenes',
        props: { resource: 'scenes' },
      },
      {
        component: caseCategories,
        meta: { icon: 'lucide:tags', title: '案例分类' },
        name: 'CaseCategories',
        path: '/applications/case-categories',
      },
      {
        component: manager,
        meta: { icon: 'lucide:badge-check', title: '案例管理' },
        name: 'Cases',
        path: '/applications/cases',
        props: { resource: 'cases' },
      },
    ],
  },
  {
    component: BasicLayout,
    meta: {
      authority: ['super_admin', 'content_admin'],
      icon: 'lucide:flask-conical',
      order: 40,
      title: '技术管理',
    },
    name: 'TechnologyContent',
    path: '/technology-content',
    children: [
      {
        component: technologyPages,
        meta: { icon: 'lucide:microscope', title: '技术内容' },
        name: 'Technologies',
        path: '/technology-content/list',
      },
    ],
  },
  {
    component: BasicLayout,
    meta: {
      authority: ['super_admin', 'content_admin'],
      icon: 'lucide:newspaper',
      order: 50,
      title: '新闻管理',
    },
    name: 'NewsContent',
    path: '/news-content',
    children: [
      {
        component: manager,
        meta: { icon: 'lucide:file-pen-line', title: '新闻文章' },
        name: 'Articles',
        path: '/content/articles',
        props: { resource: 'articles' },
      },
    ],
  },
  {
    component: BasicLayout,
    meta: {
      authority: ['super_admin', 'content_admin'],
      icon: 'lucide:handshake',
      order: 60,
      title: '合作与单页',
    },
    name: 'PagesContent',
    path: '/pages-content',
    children: [
      {
        component: cooperationPages,
        meta: { icon: 'lucide:handshake', title: '合作页面' },
        name: 'CooperationPages',
        path: '/pages-content/cooperation',
      },
      {
        component: manager,
        meta: { icon: 'lucide:building-2', title: '合作伙伴' },
        name: 'Partners',
        path: '/pages-content/partners',
        props: { resource: 'partners' },
      },
      {
        component: manager,
        meta: { icon: 'lucide:file-text', title: '单页管理' },
        name: 'Pages',
        path: '/pages-content/pages',
        props: { resource: 'pages' },
      },
    ],
  },
];

export default routes;

import type { RouteRecordRaw } from 'vue-router';

import { BasicLayout } from '#/layouts';

const routes: RouteRecordRaw[] = [
  {
    component: BasicLayout,
    meta: { icon: 'lucide:layout-dashboard', order: -1, title: '控制台' },
    name: 'DashboardRoot',
    path: '/',
    redirect: '/dashboard',
    children: [
      {
        component: () => import('#/views/dashboard/analytics/index.vue'),
        meta: { affixTab: true, icon: 'lucide:gauge', title: '运营概览' },
        name: 'Dashboard',
        path: '/dashboard',
      },
    ],
  },
];

export default routes;

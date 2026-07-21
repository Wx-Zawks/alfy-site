import { reactive, watch } from 'vue';

export type ContentResource =
  | 'articles'
  | 'banners'
  | 'cases'
  | 'pages'
  | 'partners'
  | 'products'
  | 'scenes'
  | 'technologies';

export type ContentStatus = 'draft' | 'offline' | 'published';

export interface ContentItem {
  category: string;
  cover: string;
  createdAt: string;
  featured: boolean;
  id: number;
  resource: ContentResource;
  seoDescription: string;
  seoKeywords: string;
  seoTitle: string;
  slug: string;
  sortOrder: number;
  status: ContentStatus;
  summary: string;
  title: string;
  updatedAt: string;
}

export interface Inquiry {
  assignee: string;
  company: string;
  createdAt: string;
  email: string;
  followUp: string;
  id: number;
  message: string;
  name: string;
  phone: string;
  source: string;
  status: 'completed' | 'following' | 'invalid' | 'pending';
  type: string;
}

export interface MediaAsset {
  alt: string;
  createdAt: string;
  id: number;
  name: string;
  size: string;
  type: 'document' | 'image' | 'video';
  url: string;
}

export interface RedirectRule {
  enabled: boolean;
  id: number;
  source: string;
  target: string;
  updatedAt: string;
}

export interface CmsState {
  content: ContentItem[];
  inquiries: Inquiry[];
  media: MediaAsset[];
  redirects: RedirectRule[];
  settings: {
    address: string;
    email: string;
    icp: string;
    phone: string;
    siteDescription: string;
    siteName: string;
  };
}

export const resourceMeta: Record<
  ContentResource,
  { categories: string[]; description: string; label: string }
> = {
  banners: {
    categories: ['首页主视觉', '活动推广'],
    description: '维护首页首屏主题、图片、跳转链接与展示顺序',
    label: '首页 Banner',
  },
  products: {
    categories: ['粉体/分散体', '气凝胶毡/板', '气凝胶涂料', '配套体系'],
    description: '维护产品资料、技术参数、应用关联与发布状态',
    label: '产品',
  },
  scenes: {
    categories: ['建筑节能', '工业节能', '石油石化', '新能源'],
    description: '维护应用场景、客户痛点和推荐产品',
    label: '应用场景',
  },
  cases: {
    categories: ['建筑节能', '工业节能', '联合创新'],
    description: '维护项目背景、解决方案、应用效果和合作伙伴',
    label: '案例',
  },
  technologies: {
    categories: ['核心工艺', '研发平台', '检测能力'],
    description: '维护已验证技术能力和研发平台资料',
    label: '技术',
  },
  articles: {
    categories: ['公司新闻', '行业资讯', '技术文章'],
    description: '维护新闻稿、置顶内容、发布时间和 SEO 信息',
    label: '新闻文章',
  },
  partners: {
    categories: ['高校科研', '产业伙伴', '工程伙伴'],
    description: '维护已授权展示的合作伙伴和品牌标识',
    label: '合作伙伴',
  },
  pages: {
    categories: ['商务合作', '关于我们', '招聘'],
    description: '维护合作、公司简介与招聘等固定页面内容',
    label: '单页',
  },
};

const now = '2026-07-17 15:30';
const content = (
  resource: ContentResource,
  id: number,
  title: string,
  slug: string,
  category: string,
  summary: string,
  status: ContentStatus = 'published',
  featured = false,
  sortOrder = id,
  cover = '',
): ContentItem => ({
  category,
  cover,
  createdAt: now,
  featured,
  id,
  resource,
  seoDescription: summary,
  seoKeywords: '奥飞新材,气凝胶',
  seoTitle: `${title}｜奥飞新材`,
  slug,
  sortOrder,
  status,
  summary,
  title,
  updatedAt: now,
});

const seedState: CmsState = {
  content: [
    content('banners', 1, '新一代气凝胶及其复合材料技术', 'new-generation-aerogel', '首页主视觉', '连接科研源头与真实工程场景，让材料创新进入产业应用。', 'published', true, 1, '/images/launch-1.jpg'),
    content('products', 101, '气凝胶粉体', 'aerogel-powder', '粉体/分散体', '面向涂料、浆料和复合材料体系的轻质多孔基础材料。', 'published', true, 1, '/images/aerogel-powder.jpg'),
    content('products', 102, '气凝胶分散体', 'aerogel-dispersion', '粉体/分散体', '针对水性体系的预分散形态，便于后续复合开发。', 'published', false, 2, '/images/dispersion.png'),
    content('products', 103, '气凝胶毡', 'aerogel-blanket', '气凝胶毡/板', '轻薄柔性，适配管道、设备和异形表面保温。', 'draft', false, 3, '/images/aerogel-fabric.jpg'),
    content('products', 104, '气凝胶外墙隔热涂料', 'exterior-coating', '气凝胶涂料', '面向建筑外墙的轻薄隔热与系统施工需求。', 'published', true, 4, '/images/dispersion.png'),
    content('scenes', 201, '建筑节能', 'green-building', '建筑节能', '服务外墙保温、内墙防霉防潮与装配式建筑。', 'published', true, 1, '/images/news-3.jpeg'),
    content('scenes', 202, '工业节能', 'industrial-energy', '工业节能', '面向工业管道、窑炉、罐体及复杂设备表面保温。', 'published', false, 2, '/images/heated-block.jpg'),
    content('cases', 301, '三一云谷气凝胶节能应用示范', 'sany-cloud-valley', '建筑节能', '装配式建筑外墙及夹层保温应用路径。', 'published', true, 1, '/images/news-1.jpeg'),
    content('cases', 302, '工业设备隔热验证', 'industrial-equipment', '工业节能', '通过材料与结构组合评估热损失改善。', 'draft', false, 2, '/images/heated-block.jpg'),
    content('technologies', 401, '常压干燥工艺', 'ambient-pressure-drying', '核心工艺', '面向规模化制造的核心工艺能力，公开数据以已验证口径为准。', 'published', true, 1, '/images/research-institute.jpg'),
    content('technologies', 402, '气凝胶研究院', 'aerogel-institute', '研发平台', '依托产学研合作构建材料、复合和应用验证能力。', 'published', false, 2, '/images/university.jpeg'),
    content('articles', 501, '奥飞新材携手产学研伙伴探索气凝胶产业化应用新范式', 'industry-collaboration', '公司新闻', '连接科研源头、复合开发与工程现场，让创新通过真实项目持续验证。', 'published', true, 1, '/images/news-2.jpeg'),
    content('articles', 502, '新一代气凝胶及复合材料技术发布活动举行', 'technology-launch', '公司新闻', '多方嘉宾共同交流材料技术与产业化应用方向。', 'published', false, 2, '/images/launch-1.jpg'),
    content('articles', 503, '为什么气凝胶常压干燥技术值得关注', 'ambient-drying', '技术文章', '从工艺路径理解规模化制备的关键问题。', 'draft', false, 3, '/images/research-institute.jpg'),
    content('partners', 601, '中南大学', 'csu', '高校科研', '气凝胶成果转化与联合创新伙伴。', 'published', true, 1, '/images/university.jpeg'),
    content('partners', 602, '三一筑工', 'sany-precast', '工程伙伴', '装配式建筑应用联合验证伙伴。', 'published', false, 2, '/images/news-1.jpeg'),
    content('pages', 701, '商务合作', 'cooperation', '商务合作', '经销合作、复合产品开发与产业链合作说明。'),
    content('pages', 702, '关于奥飞', 'about', '关于我们', '公司简介、研发实力、发展历程与联系方式。'),
  ],
  inquiries: [
    { id: 9001, name: '张先生', phone: '138****6821', email: 'zhang@example.com', company: '长沙某建筑科技公司', type: '产品咨询', message: '咨询外墙隔热涂料样品与技术参数。', source: '/products/exterior-coating', status: 'pending', assignee: '未分配', followUp: '', createdAt: '2026-07-17 10:24' },
    { id: 9002, name: '李女士', phone: '186****1093', email: 'li@example.com', company: '湖南某工业设备公司', type: '项目合作', message: '希望了解工业管道保温解决方案。', source: '/applications?category=industry', status: 'following', assignee: '周明', followUp: '已电话沟通，待补充工况参数。', createdAt: '2026-07-16 16:40' },
    { id: 9003, name: '王先生', phone: '139****2270', email: 'wang@example.com', company: '华南材料研究中心', type: '联合研发', message: '咨询气凝胶分散体联合开发。', source: '/cooperation', status: 'completed', assignee: '陈颖', followUp: '已转交研发团队。', createdAt: '2026-07-15 09:12' },
  ],
  media: [
    { id: 801, name: '气凝胶粉体.jpg', type: 'image', size: '1.5 MB', url: '/images/aerogel-powder.jpg', alt: '奥飞新材气凝胶粉体', createdAt: '2026-07-17 11:20' },
    { id: 802, name: '发布会现场.jpg', type: 'image', size: '7.4 MB', url: '/images/launch-1.jpg', alt: '新一代气凝胶技术发布活动', createdAt: '2026-07-16 17:05' },
    { id: 803, name: '产品手册.pdf', type: 'document', size: '12.8 MB', url: '#', alt: '奥飞气凝胶研究院产品手册', createdAt: '2026-07-15 14:31' },
  ],
  redirects: [
    { id: 1, source: '/product/aerogel-powder', target: '/products/aerogel-powder', enabled: true, updatedAt: now },
    { id: 2, source: '/news/company/12', target: '/news/technology-launch', enabled: true, updatedAt: now },
  ],
  settings: {
    address: '湖南省长沙市岳麓区',
    email: 'contact@alfy.com.cn',
    icp: '待确认',
    phone: '待确认',
    siteDescription: '奥飞新材新一代气凝胶及其复合材料技术与应用。',
    siteName: '奥飞新材',
  },
};

const STORAGE_KEY = 'alfy-admin-cms-v1';

function cloneSeed(): CmsState {
  return JSON.parse(JSON.stringify(seedState)) as CmsState;
}

function loadState(): CmsState {
  if (typeof window === 'undefined') return cloneSeed();
  try {
    const cached = window.localStorage.getItem(STORAGE_KEY);
    return cached ? (JSON.parse(cached) as CmsState) : cloneSeed();
  } catch {
    return cloneSeed();
  }
}

export const cmsState = reactive<CmsState>(loadState());

if (typeof window !== 'undefined') {
  watch(
    cmsState,
    (value) => window.localStorage.setItem(STORAGE_KEY, JSON.stringify(value)),
    { deep: true },
  );
}

export function resetCmsState() {
  const fresh = cloneSeed();
  cmsState.content.splice(0, cmsState.content.length, ...fresh.content);
  cmsState.inquiries.splice(0, cmsState.inquiries.length, ...fresh.inquiries);
  cmsState.media.splice(0, cmsState.media.length, ...fresh.media);
  cmsState.redirects.splice(0, cmsState.redirects.length, ...fresh.redirects);
  Object.assign(cmsState.settings, fresh.settings);
}

export function nextId(items: Array<{ id: number }>) {
  return Math.max(0, ...items.map((item) => item.id)) + 1;
}

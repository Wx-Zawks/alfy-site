import type { ApplicationScene, CaseProject } from '~/types/content'

export const applicationScenes: ApplicationScene[] = [
  { key: 'building', name: '建筑节能', slogan: '让建筑更节能，让居住更舒适', summary: '外墙外保温、外墙内保温、内墙防霉防潮与装配式建筑。', image: '/images/news-3.jpeg' },
  { key: 'industry', name: '工业节能', slogan: '降低热损失，提升设备能效', summary: '工业管道、窑炉、罐体及复杂设备表面保温。', image: '/images/heated-block.jpg' },
  { key: 'petrochemical', name: '石油石化', slogan: '面向复杂工况的保温体系', summary: '结合温度、腐蚀、检修和施工条件匹配材料组合。', image: '/images/news-5.jpeg' },
  { key: 'new-energy', name: '新能源汽车', slogan: '有限空间内的热防护', summary: '围绕电池、座舱和关键部件开展复合材料开发。', image: '/images/news-4.jpeg' }
]

export const cases: CaseProject[] = [
  { id: '1', slug: 'sany-cloud-valley', title: '三一云谷气凝胶节能应用示范', category: '建筑节能', summary: '装配式建筑外墙及夹层保温应用路径。', image: '/images/news-1.jpeg' },
  { id: '2', slug: 'industrial-park', title: '产业园建筑节能应用', category: '建筑节能', summary: '面向公共建筑围护结构的节能改造方向。', image: '/images/news-3.jpeg' },
  { id: '3', slug: 'industrial-equipment', title: '工业设备隔热验证', category: '工业节能', summary: '通过材料与结构组合评估热损失改善。', image: '/images/heated-block.jpg' }
]

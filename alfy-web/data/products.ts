import type { Product } from '~/types/content'

export const products: Product[] = [
  { id: '1', name: '气凝胶粉体', slug: 'aerogel-powder', category: 'raw', categoryName: '粉体/分散体', summary: '面向涂料、浆料和复合材料体系的轻质多孔基础材料。', image: '/images/aerogel-powder.jpg', features: ['轻质多孔', '复合开发', '多场景适配'] },
  { id: '2', name: '气凝胶分散体', slug: 'aerogel-dispersion', category: 'raw', categoryName: '粉体/分散体', summary: '针对水性体系的预分散形态，便于后续复合开发。', image: '/images/dispersion.png', features: ['水性体系', '均匀分散', '配方友好'] },
  { id: '3', name: '气凝胶颗粒', slug: 'aerogel-granules', category: 'raw', categoryName: '粉体/分散体', summary: '适用于填充、保温构造与多种材料复合方向。', image: '/images/aerogel-granules.jpg', features: ['轻质填充', '多种粒径', '保温隔热'] },
  { id: '4', name: '气凝胶毡', slug: 'aerogel-blanket', category: 'blanket', categoryName: '气凝胶毡/板', summary: '轻薄柔性，适配管道、设备和异形表面保温。', image: '/images/aerogel-fabric.jpg', features: ['柔性施工', '轻薄隔热', '异形适配'] },
  { id: '5', name: '气凝胶复合板', slug: 'aerogel-board', category: 'blanket', categoryName: '气凝胶毡/板', summary: '面向平整构件、墙体和夹层结构的隔热需求。', image: '/images/aerogel-block.jpg', features: ['结构稳定', '构造适配', '建筑节能'] },
  { id: '6', name: '气凝胶外墙隔热涂料', slug: 'exterior-coating', category: 'coating', categoryName: '气凝胶涂料', summary: '面向建筑外墙的轻薄隔热与系统施工需求。', image: '/images/dispersion.png', features: ['水性体系', '轻薄隔热', '施工便利'] },
  { id: '7', name: '气凝胶内墙保温涂料', slug: 'interior-coating', category: 'coating', categoryName: '气凝胶涂料', summary: '面向内墙热舒适、防霉防潮等应用方向。', image: '/images/ambient-particles.png', features: ['热舒适', '防潮方向', '室内应用'] },
  { id: '8', name: '界面与基层配套', slug: 'interface-system', category: 'support', categoryName: '气凝胶配套', summary: '根据基材状态提供粘结、找平和界面处理支持。', image: '/images/heated-block.jpg', features: ['基层处理', '系统配套', '施工协同'] },
  { id: '9', name: '气凝胶工业隔热涂料', slug: 'industrial-coating', category: 'coating', categoryName: '气凝胶涂料', summary: '面向窑炉、管道及高温设备的隔热防烫与节能改造。', image: '/images/dispersion.png', features: ['隔热防烫', '设备节能', '复杂表面施工'] },
  { id: '10', name: '气凝胶水泥复合材料', slug: 'aerogel-cement', category: 'support', categoryName: '气凝胶配套', summary: '实现结构、保温和装饰一体化，适配装配式建筑与工程构造。', image: '/images/aerogel-block-2.jpg', features: ['保温结构一体化', '工程适配', '轻质复合'] },
  { id: '11', name: '气凝胶隔音复合板', slug: 'acoustic-panel', category: 'blanket', categoryName: '气凝胶毡/板', summary: '面向轨道交通、船舶和设备舱体的轻量化隔音降噪。', image: '/images/aerogel-fabric.jpg', features: ['轻量隔音', '多层复合', '交通装备'] },
  { id: '12', name: '气凝胶阻燃复合板', slug: 'fireproof-panel', category: 'blanket', categoryName: '气凝胶毡/板', summary: '面向新能源电池、储能和高安全场景的防火隔热保护。', image: '/images/alumina-aerogel.jpg', features: ['阻燃隔热', '高温防护', '新能源安全'] }
]

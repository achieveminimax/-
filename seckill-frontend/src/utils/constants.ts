// 电商秒杀系统 - 常量定义

// ========== 订单状态 ==========
export const ORDER_STATUS = {
  PENDING: 1,
  PAID: 2,
  SHIPPED: 3,
  COMPLETED: 4,
  CANCELLED: 5,
  REFUNDED: 6,
} as const;

export const ORDER_STATUS_TEXT: Record<number, string> = {
  1: '待支付',
  2: '已支付',
  3: '已发货',
  4: '已完成',
  5: '已取消',
  6: '已退款',
};

export const ORDER_STATUS_COLOR: Record<number, string> = {
  1: '#FAAD14',
  2: '#1890FF',
  3: '#52C41A',
  4: '#8C8C8C',
  5: '#8C8C8C',
  6: '#FF4D4F',
};

// ========== 秒杀状态 ==========
export const SECKILL_STATUS = {
  NOT_STARTED: 0,
  IN_PROGRESS: 1,
  ENDED: 2,
} as const;

export const SECKILL_STATUS_TEXT: Record<number, string> = {
  0: '未开始',
  1: '进行中',
  2: '已结束',
};

// ========== 秒杀记录状态 ==========
export const SECKILL_RECORD_STATUS = {
  QUEUING: 0,
  SUCCESS: 1,
  FAILED_SOLD_OUT: 2,
  FAILED_ALREADY_BOUGHT: 3,
  FAILED_OTHER: 4,
} as const;

export const SECKILL_RECORD_STATUS_TEXT: Record<number, string> = {
  0: '排队中',
  1: '秒杀成功',
  2: '商品已售罄',
  3: '您已购买过该商品',
  4: '系统繁忙',
};

// ========== 支付方式 ==========
export const PAY_TYPE = {
  BALANCE: 1,
  ALIPAY: 2,
  WECHAT: 3,
} as const;

export const PAY_TYPE_TEXT: Record<number, string> = {
  1: '余额支付',
  2: '支付宝',
  3: '微信支付',
};

// ========== 商品状态 ==========
export const GOODS_STATUS = {
  OFFLINE: 0,
  ONLINE: 1,
} as const;

export const GOODS_STATUS_TEXT: Record<number, string> = {
  0: '已下架',
  1: '上架中',
};

// ========== 路由路径 ==========
export const ROUTES = {
  // 用户端
  LOGIN: '/login',
  REGISTER: '/register',
  HOME: '/',
  GOODS: '/goods',
  GOODS_DETAIL: '/goods/:id',
  SECKILL: '/seckill/:id',
  SECKILL_BUY: '/seckill/:id/buy',
  SECKILL_RESULT: '/seckill/result/:recordId',
  ORDERS: '/orders',
  ORDER_DETAIL: '/order/:orderNo',
  PAY: '/pay/:orderNo',
  PROFILE: '/profile',
  ADDRESS: '/address',
  SETTINGS: '/settings',
  
  // 管理后台
  ADMIN_LOGIN: '/admin/login',
  ADMIN_DASHBOARD: '/admin',
  ADMIN_GOODS: '/admin/goods',
  ADMIN_GOODS_CREATE: '/admin/goods/create',
  ADMIN_GOODS_EDIT: '/admin/goods/:id/edit',
  ADMIN_SECKILL: '/admin/seckill',
  ADMIN_SECKILL_CREATE: '/admin/seckill/create',
  ADMIN_SECKILL_EDIT: '/admin/seckill/:id/edit',
  ADMIN_SECKILL_STATS: '/admin/seckill/:id/stats',
  ADMIN_ORDERS: '/admin/orders',
  ADMIN_USERS: '/admin/users',
} as const;

// ========== 本地存储 Key ==========
export const STORAGE_KEYS = {
  TOKEN: 'token',
  USER: 'user',
  REMEMBER_ME: 'rememberMe',
} as const;

// ========== 分页配置 ==========
export const PAGE_CONFIG = {
  DEFAULT_PAGE: 1,
  DEFAULT_SIZE: 10,
  SIZE_OPTIONS: [10, 20, 50, 100],
} as const;

// ========== 秒杀配置 ==========
export const SECKILL_CONFIG = {
  MAX_PER_USER: 1,
  PAY_TIMEOUT_MINUTES: 15,
  QUEUE_TIMEOUT_SECONDS: 30,
} as const;

// 电商秒杀系统 - 类型定义

// ========== 用户相关 ==========
export interface User {
  id: number;
  username: string;
  phone: string;
  avatar?: string;
  token?: string;
}

export interface LoginForm {
  username: string;
  password: string;
  remember?: boolean;
}

export interface RegisterForm {
  username: string;
  phone: string;
  code: string;
  password: string;
  confirmPassword: string;
  agreement: boolean;
}

// ========== 商品相关 ==========
export interface Goods {
  id: number;
  name: string;
  description?: string;
  price: number;
  stock: number;
  image: string;
  images?: string[];
  status: number; // 0-下架 1-上架
  createTime?: string;
}

// ========== 秒杀活动相关 ==========
export type SeckillStatus = 0 | 1 | 2; // 0-未开始 1-进行中 2-已结束

export interface SeckillGoods {
  id: number;
  goodsId: number;
  goodsName: string;
  goodsImage: string;
  seckillPrice: number;
  originalPrice: number;
  stock: number;
  sold: number;
}

export interface SeckillActivity {
  id: number;
  title: string;
  goodsList: SeckillGoods[];
  startTime: string;
  endTime: string;
  status: SeckillStatus;
  createTime?: string;
}

// ========== 订单相关 ==========
export type OrderStatus = 1 | 2 | 3 | 4 | 5 | 6; 
// 1-待支付 2-已支付 3-已发货 4-已完成 5-已取消 6-已退款

export type PayType = 1 | 2 | 3; // 1-余额 2-支付宝 3-微信

export interface OrderItem {
  id: number;
  goodsId: number;
  goodsName: string;
  goodsImage: string;
  price: number;
  quantity: number;
}

export interface Order {
  id: number;
  orderNo: string;
  userId: number;
  items: OrderItem[];
  totalAmount: number;
  freightAmount: number;
  payAmount: number;
  status: OrderStatus;
  payType?: PayType;
  payTime?: string;
  address?: Address;
  createTime: string;
  expireTime?: string;
}

// ========== 地址相关 ==========
export interface Address {
  id: number;
  userId: number;
  name: string;
  phone: string;
  province: string;
  city: string;
  district: string;
  detail: string;
  isDefault: boolean;
}

// ========== 秒杀记录相关 ==========
export type SeckillRecordStatus = 0 | 1 | 2 | 3 | 4;
// 0-排队中 1-成功 2-失败-售罄 3-失败-已购买 4-失败-其他

export interface SeckillRecord {
  id: number;
  activityId: number;
  goodsId: number;
  userId: number;
  status: SeckillRecordStatus;
  orderNo?: string;
  message?: string;
  createTime: string;
}

// ========== API 响应 ==========
export interface ApiResponse<T = unknown> {
  code: number;
  message: string;
  data: T;
}

export interface PageData<T> {
  list: T[];
  total: number;
  page: number;
  size: number;
}

// ========== 管理后台 ==========
export interface DashboardStats {
  todayOrders: number;
  todayAmount: number;
  activityCount: number;
  userCount: number;
  orderGrowth: number;
  amountGrowth: number;
}

export interface ActivityStats {
  totalStock: number;
  soldCount: number;
  participantCount: number;
  successRate: number;
  qpsPeak: number;
  qpsData: { time: string; value: number }[];
  stockData: { time: string; value: number }[];
  orderStats: {
    pending: number;
    paid: number;
    cancelled: number;
  };
}

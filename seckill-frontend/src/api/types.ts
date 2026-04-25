export interface ApiResponse<T = unknown> {
  code: number
  message: string
  data: T
}

export interface PageResponse<T = unknown> {
  records: T[]
  total: number
  current: number
  size: number
  pages: number
}

export interface LoginRequest {
  username: string
  password: string
}

export interface LoginResponse {
  token: string
  refreshToken: string
  expiresIn: number
  userInfo: UserInfoResponse
}

export interface RegisterRequest {
  username: string
  password: string
  phone: string
  code: string
}

export interface RegisterResponse {
  id: number
  username: string
}

export interface UserInfoResponse {
  id: number
  username: string
  phone: string
  email: string
  avatar: string
  nickname: string
  status: number
  createTime: string
}

export interface RefreshTokenRequest {
  refreshToken: string
}

export interface RefreshTokenResponse {
  token: string
  refreshToken: string
  expiresIn: number
}

export interface AddressRequest {
  name: string
  phone: string
  province: string
  city: string
  district: string
  detail: string
  isDefault: boolean
}

export interface AddressResponse {
  id: number
  userId: number
  name: string
  phone: string
  province: string
  city: string
  district: string
  detail: string
  isDefault: boolean
  createTime: string
}

export interface GoodsListResponse {
  id: number
  name: string
  description: string
  price: number
  originalPrice: number
  stock: number
  sales: number
  categoryId: number
  categoryName: string
  images: string[]
  status: number
  createTime: string
}

export interface GoodsDetailResponse {
  id: number
  name: string
  description: string
  price: number
  originalPrice: number
  stock: number
  sales: number
  categoryId: number
  categoryName: string
  images: string[]
  detail: string
  status: number
  createTime: string
}

export interface CategoryNode {
  id: number
  name: string
  parentId: number
  level: number
  sort: number
  icon: string
  children: CategoryNode[]
}

export interface CategoryTreeResponse extends CategoryNode {}

export interface SeckillActivityListResponse {
  id: number
  title: string
  description: string
  startTime: string
  endTime: string
  status: number
  goodsCount: number
  banner: string
}

export interface SeckillActivityDetailResponse {
  id: number
  title: string
  description: string
  startTime: string
  endTime: string
  status: number
  goodsList: SeckillGoodsItem[]
}

export interface SeckillGoodsItem {
  id: number
  activityId: number
  goodsId: number
  goodsName: string
  goodsImage: string
  seckillPrice: number
  originalPrice: number
  stock: number
  sold: number
  limitPerUser: number
}

export interface SeckillPathResponse {
  path: string
  expireTime: number
}

export interface SeckillExecuteRequest {
  activityId: number
  goodsId: number
  path: string
  addressId: number
}

export interface SeckillExecuteResponse {
  recordId: number
  status: number
  message: string
}

export interface SeckillResultResponse {
  id: number
  activityId: number
  goodsId: number
  goodsName: string
  goodsImage: string
  price: number
  status: number
  orderNo: string
  createTime: string
}

export interface OrderItem {
  id: number
  goodsId: number
  goodsName: string
  goodsImage: string
  price: number
  quantity: number
}

export interface OrderListResponse {
  id: number
  orderNo: string
  userId: number
  status: number
  totalAmount: number
  payAmount: number
  freightAmount: number
  items: OrderItem[]
  createTime: string
  payTime: string
  expireTime: string
}

export interface OrderDetailResponse {
  id: number
  orderNo: string
  userId: number
  status: number
  totalAmount: number
  payAmount: number
  freightAmount: number
  items: OrderItem[]
  address: AddressResponse
  createTime: string
  payTime: string
  deliverTime: string
  finishTime: string
  cancelTime: string
  cancelReason: string
}

export interface CancelOrderRequest {
  orderNo: string
  cancelReason: string
}

export interface PayCreateRequest {
  orderNo: string
  payType: number
}

export interface PayCreateResponse {
  orderNo: string
  payType: number
  payUrl: string
  payStatus: number
  amount: number
}

export interface PayCallbackRequest {
  orderNo: string
  payType: number
  transactionId: string
}

export interface PayStatusResponse {
  orderNo: string
  payType: number
  payStatus: number
  amount: number
  payTime: string
}

export interface AdminLoginRequest {
  username: string
  password: string
}

export interface AdminLoginResponse {
  token: string
  adminInfo: {
    id: number
    username: string
    role: string
  }
}

export interface AdminGoodsRequest {
  name: string
  description: string
  price: number
  originalPrice: number
  stock: number
  categoryId: number
  images: string[]
  detail: string
  status: number
}

export interface AdminSeckillGoodsRequest {
  goodsId: number
  seckillPrice: number
  stock: number
  limitPerUser: number
}

export interface AdminSeckillRequest {
  id: number
  title: string
  description: string
  startTime: string
  endTime: string
  status: number
  banner: string
  goodsList: AdminSeckillGoodsRequest[]
}

export interface AdminUserItem {
  id: number
  username: string
  phone: string
  email: string
  status: number
  createTime: string
}

export interface AdminUserListResponse {
  records: AdminUserItem[]
  total: number
  current: number
  size: number
}

export interface AdminOrderItem {
  id: number
  orderNo: string
  username: string
  status: number
  payAmount: number
  payType: number
  createTime: string
  payTime: string
}

export interface AdminOrderListResponse {
  records: AdminOrderItem[]
  total: number
  current: number
  size: number
}

export interface DashboardStatsData {
  todayOrders: number
  todayAmount: number
  totalOrders: number
  totalAmount: number
  userCount: number
  goodsCount: number
  seckillCount: number
  orderTrend: { date: string; count: number }[]
  amountTrend: { date: string; amount: number }[]
}

export interface DashboardResponse {
  stats: DashboardStatsData
  recentOrders: AdminOrderItem[]
  hotGoods: {
    id: number
    name: string
    sales: number
    amount: number
  }[]
}

export interface ShipOrderRequest {
  expressCompany: string
  expressNo: string
}

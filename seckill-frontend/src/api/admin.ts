import { request } from './request'
import type {
  AdminLoginRequest,
  AdminLoginResponse,
  AdminGoodsRequest,
  AdminSeckillRequest,
  AdminUserListResponse,
  AdminOrderListResponse,
  DashboardResponse,
  PageResponse,
  ShipOrderRequest,
  GoodsListResponse,
  SeckillActivityListResponse,
} from './types'

export const adminLogin = (data: AdminLoginRequest) => {
  return request<AdminLoginResponse>({
    url: '/api/admin/login',
    method: 'POST',
    data,
  })
}

export const getDashboard = () => {
  return request<DashboardResponse>({
    url: '/api/admin/dashboard',
    method: 'GET',
  })
}

export const getAdminGoodsList = (params: { current?: number; size?: number; keyword?: string; status?: number }) => {
  return request<PageResponse<GoodsListResponse>>({
    url: '/api/admin/goods/list',
    method: 'GET',
    params,
  })
}

export const createAdminGoods = (data: AdminGoodsRequest) => {
  return request({
    url: '/api/admin/goods/create',
    method: 'POST',
    data,
  })
}

export const updateAdminGoods = (id: number, data: AdminGoodsRequest) => {
  return request({
    url: `/api/admin/goods/update/${id}`,
    method: 'PUT',
    data,
  })
}

export const deleteAdminGoods = (id: number) => {
  return request({
    url: `/api/admin/goods/delete/${id}`,
    method: 'DELETE',
  })
}

export const updateAdminGoodsStatus = (id: number, status: number) => {
  return request({
    url: `/api/admin/goods/status/${id}`,
    method: 'PUT',
    data: { status },
  })
}

export const getAdminSeckillList = (params: { current?: number; size?: number; status?: number }) => {
  return request<PageResponse<SeckillActivityListResponse>>({
    url: '/api/admin/seckill/list',
    method: 'GET',
    params,
  })
}

export const createAdminSeckill = (data: AdminSeckillRequest) => {
  return request({
    url: '/api/admin/seckill/create',
    method: 'POST',
    data,
  })
}

export const updateAdminSeckill = (data: AdminSeckillRequest) => {
  return request({
    url: '/api/admin/seckill/update',
    method: 'PUT',
    data,
  })
}

export const getAdminSeckillStats = (activityId: number) => {
  return request({
    url: `/api/admin/seckill/stats/${activityId}`,
    method: 'GET',
  })
}

export const getAdminOrderList = (params: { current?: number; size?: number; status?: number; orderNo?: string }) => {
  return request<PageResponse<AdminOrderListResponse>>({
    url: '/api/admin/order/list',
    method: 'GET',
    params,
  })
}

export const shipOrder = (orderNo: string, data: ShipOrderRequest) => {
  return request({
    url: `/api/admin/order/ship/${orderNo}`,
    method: 'PUT',
    data,
  })
}

export const getAdminUserList = (params: { current?: number; size?: number; keyword?: string; status?: number }) => {
  return request<PageResponse<AdminUserListResponse>>({
    url: '/api/admin/user/list',
    method: 'GET',
    params,
  })
}

export const updateUserStatus = (userId: number, status: number) => {
  return request({
    url: `/api/admin/user/status/${userId}`,
    method: 'PUT',
    data: { status },
  })
}

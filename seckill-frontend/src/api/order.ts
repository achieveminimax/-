import { request } from './request'
import type { OrderListResponse, OrderDetailResponse, PageResponse } from './types'

export const getOrderList = (params: { status?: number; current?: number; size?: number }) => {
  return request<PageResponse<OrderListResponse>>({
    url: '/api/order/list',
    method: 'GET',
    params,
  })
}

export const getOrderDetail = (orderNo: string) => {
  return request<OrderDetailResponse>({
    url: `/api/order/detail/${orderNo}`,
    method: 'GET',
  })
}

export const cancelOrder = (orderNo: string, cancelReason?: string) => {
  return request({
    url: `/api/order/cancel/${orderNo}`,
    method: 'PUT',
    data: { cancelReason },
  })
}

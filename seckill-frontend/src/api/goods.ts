import { request } from './request'
import type { GoodsListResponse, GoodsDetailResponse, PageResponse } from './types'

export const getGoodsList = (params: { current?: number; size?: number; categoryId?: number; keyword?: string; sort?: string }) => {
  return request<PageResponse<GoodsListResponse>>({
    url: '/api/goods/list',
    method: 'GET',
    params,
  })
}

export const getGoodsDetail = (id: number) => {
  return request<GoodsDetailResponse>({
    url: `/api/goods/detail/${id}`,
    method: 'GET',
  })
}

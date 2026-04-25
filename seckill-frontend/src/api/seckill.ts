import { request } from './request'
import type {
  SeckillActivityListResponse,
  SeckillActivityDetailResponse,
  SeckillPathResponse,
  SeckillExecuteRequest,
  SeckillExecuteResponse,
  SeckillResultResponse,
  PageResponse,
} from './types'

export const getSeckillActivityList = (params: { status?: number; current?: number; size?: number }) => {
  return request<PageResponse<SeckillActivityListResponse>>({
    url: '/api/seckill/activity/list',
    method: 'GET',
    params,
  })
}

export const getSeckillActivityDetail = (activityId: number) => {
  return request<SeckillActivityDetailResponse>({
    url: `/api/seckill/activity/detail/${activityId}`,
    method: 'GET',
  })
}

export const getSeckillPath = (activityId: number, goodsId: number) => {
  return request<SeckillPathResponse>({
    url: `/api/seckill/path/${activityId}/${goodsId}`,
    method: 'GET',
  })
}

export const executeSeckill = (data: SeckillExecuteRequest) => {
  return request<SeckillExecuteResponse>({
    url: '/api/seckill/execute',
    method: 'POST',
    data,
  })
}

export const getSeckillResult = (recordId: number) => {
  return request<SeckillResultResponse>({
    url: `/api/seckill/result/${recordId}`,
    method: 'GET',
  })
}

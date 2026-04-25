import { request } from './request'
import type { PayCreateRequest, PayCreateResponse, PayStatusResponse } from './types'

export const createPay = (data: PayCreateRequest) => {
  return request<PayCreateResponse>({
    url: '/api/pay/create',
    method: 'POST',
    data,
  })
}

export const getPayStatus = (orderNo: string) => {
  return request<PayStatusResponse>({
    url: `/api/pay/status/${orderNo}`,
    method: 'GET',
  })
}

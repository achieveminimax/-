import { request } from './request'
import type { CategoryTreeResponse } from './types'

export const getCategoryList = () => {
  return request<CategoryTreeResponse[]>({
    url: '/api/category/tree',
    method: 'GET',
  })
}

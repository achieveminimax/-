import { request } from './request'
import type { AddressRequest, AddressResponse } from './types'

export const getAddressList = () => {
  return request<AddressResponse[]>({
    url: '/api/address/list',
    method: 'GET',
  })
}

export const createAddress = (data: AddressRequest) => {
  return request<AddressResponse>({
    url: '/api/address/create',
    method: 'POST',
    data,
  })
}

export const updateAddress = (id: number, data: AddressRequest) => {
  return request<AddressResponse>({
    url: `/api/address/update/${id}`,
    method: 'PUT',
    data,
  })
}

export const deleteAddress = (id: number) => {
  return request({
    url: `/api/address/delete/${id}`,
    method: 'DELETE',
  })
}

export const setDefaultAddress = (id: number) => {
  return request({
    url: `/api/address/default/${id}`,
    method: 'PUT',
  })
}

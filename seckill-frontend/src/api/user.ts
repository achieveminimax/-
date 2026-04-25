import { request } from './request'
import type {
  LoginRequest,
  LoginResponse,
  RegisterRequest,
  RegisterResponse,
  UserInfoResponse,
  RefreshTokenRequest,
  RefreshTokenResponse,
} from './types'

export const login = (data: LoginRequest) => {
  return request<LoginResponse>({
    url: '/api/user/login',
    method: 'POST',
    data,
  })
}

export const register = (data: RegisterRequest) => {
  return request<RegisterResponse>({
    url: '/api/user/register',
    method: 'POST',
    data,
  })
}

export const logout = () => {
  return request({
    url: '/api/user/logout',
    method: 'POST',
  })
}

export const getUserInfo = () => {
  return request<UserInfoResponse>({
    url: '/api/user/info',
    method: 'GET',
  })
}

export const updateUserInfo = (data: Partial<UserInfoResponse>) => {
  return request({
    url: '/api/user/update',
    method: 'PUT',
    data,
  })
}

export const updatePassword = (data: { oldPassword: string; newPassword: string }) => {
  return request({
    url: '/api/user/password',
    method: 'PUT',
    data,
  })
}

export const refreshToken = (data: RefreshTokenRequest) => {
  return request<RefreshTokenResponse>({
    url: '/api/user/refresh-token',
    method: 'POST',
    data,
  })
}

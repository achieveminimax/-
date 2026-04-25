import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { UserInfoResponse } from '../api/types';

export type UserInfo = UserInfoResponse;

// 管理员信息
export interface AdminInfo {
  id: number;
  username: string;
  role: string;
}

// 用户状态接口
interface UserState {
  // 普通用户相关
  token: string | null;
  refreshToken: string | null;
  userInfo: UserInfo | null;
  isLogin: boolean;

  // 管理员相关
  adminToken: string | null;
  adminRefreshToken: string | null;
  adminInfo: AdminInfo | null;
  isAdminLogin: boolean;

  // 普通用户操作
  login: (token: string, refreshToken: string, userInfo: UserInfo) => void;
  logout: () => void;
  updateUserInfo: (userInfo: Partial<UserInfo>) => void;
  setUserToken: (token: string) => void;
  setUserRefreshToken: (refreshToken: string) => void;

  // 管理员操作
  adminLogin: (token: string, adminRefreshToken: string, adminInfo: AdminInfo) => void;
  adminLogout: () => void;
  setAdminToken: (token: string) => void;
  setAdminRefreshToken: (refreshToken: string) => void;

  // 通用操作
  clearAll: () => void;
}

export const useUserStore = create<UserState>()(
  persist(
    (set) => ({
      // 初始状态 - 普通用户
      token: null,
      refreshToken: null,
      userInfo: null,
      isLogin: false,

      // 初始状态 - 管理员
      adminToken: null,
      adminRefreshToken: null,
      adminInfo: null,
      isAdminLogin: false,

      // 普通用户登录
      login: (token: string, refreshToken: string, userInfo: UserInfo) => {
        set({
          token,
          refreshToken,
          userInfo,
          isLogin: true,
        });
      },

      // 普通用户登出
      logout: () => {
        set({
          token: null,
          refreshToken: null,
          userInfo: null,
          isLogin: false,
        });
      },

      // 更新用户信息
      updateUserInfo: (userInfo: Partial<UserInfo>) => {
        set((state) => ({
          userInfo: state.userInfo ? { ...state.userInfo, ...userInfo } : null,
        }));
      },

      // 仅更新用户 token（用于刷新 token 场景）
      setUserToken: (token: string) => {
        set({ token });
      },

      // 仅更新用户 refreshToken
      setUserRefreshToken: (refreshToken: string) => {
        set({ refreshToken });
      },

      // 管理员登录
      adminLogin: (token: string, adminRefreshToken: string, adminInfo: AdminInfo) => {
        set({
          adminToken: token,
          adminRefreshToken: adminRefreshToken,
          adminInfo,
          isAdminLogin: true,
        });
      },

      // 管理员登出
      adminLogout: () => {
        set({
          adminToken: null,
          adminRefreshToken: null,
          adminInfo: null,
          isAdminLogin: false,
        });
      },

      // 仅更新管理员 token
      setAdminToken: (token: string) => {
        set({ adminToken: token });
      },

      // 仅更新管理员 refreshToken
      setAdminRefreshToken: (refreshToken: string) => {
        set({ adminRefreshToken: refreshToken });
      },

      // 清除所有状态
      clearAll: () => {
        set({
          token: null,
          refreshToken: null,
          userInfo: null,
          isLogin: false,
          adminToken: null,
          adminRefreshToken: null,
          adminInfo: null,
          isAdminLogin: false,
        });
      },
    }),
    {
      name: 'seckill-user-storage',
      // 持久化所有字段
      partialize: (state) => ({
        token: state.token,
        refreshToken: state.refreshToken,
        userInfo: state.userInfo,
        isLogin: state.isLogin,
        adminToken: state.adminToken,
        adminRefreshToken: state.adminRefreshToken,
        adminInfo: state.adminInfo,
        isAdminLogin: state.isAdminLogin,
      }),
    }
  )
);

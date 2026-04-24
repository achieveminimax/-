import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { User } from '../types';

interface UserState {
  user: User | null;
  isLogin: boolean;
  setUser: (user: User | null) => void;
  logout: () => void;
}

export const useUserStore = create<UserState>()(
  persist(
    (set) => ({
      user: null,
      isLogin: false,
      setUser: (user) => set({ user, isLogin: !!user }),
      logout: () => set({ user: null, isLogin: false }),
    }),
    {
      name: 'user-storage',
    }
  )
);

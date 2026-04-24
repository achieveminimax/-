import { createBrowserRouter, Navigate } from 'react-router-dom';
import { AdminLayout } from '../layouts';
import { Login, Home, Orders, Profile } from '../pages';
import { Dashboard } from '../pages/Admin';

// 用户端路由
const mobileRoutes = [
  {
    path: '/login',
    element: <Login />,
  },
  {
    path: '/',
    element: <Home />,
  },
  {
    path: '/goods',
    element: <div>商品列表页（待实现）</div>,
  },
  {
    path: '/goods/:id',
    element: <div>商品详情页（待实现）</div>,
  },
  {
    path: '/seckill/:id',
    element: <div>秒杀活动详情页（待实现）</div>,
  },
  {
    path: '/seckill/:id/buy',
    element: <div>秒杀抢购页（待实现）</div>,
  },
  {
    path: '/seckill/result/:recordId',
    element: <div>秒杀结果页（待实现）</div>,
  },
  {
    path: '/orders',
    element: <Orders />,
  },
  {
    path: '/order/:orderNo',
    element: <div>订单详情页（待实现）</div>,
  },
  {
    path: '/pay/:orderNo',
    element: <div>支付页（待实现）</div>,
  },
  {
    path: '/profile',
    element: <Profile />,
  },
  {
    path: '/address',
    element: <div>收货地址页（待实现）</div>,
  },
  {
    path: '/settings',
    element: <div>设置页（待实现）</div>,
  },
];

// 管理后台路由
const adminRoutes = {
  path: '/admin',
  element: <AdminLayout />,
  children: [
    {
      index: true,
      element: <Dashboard />,
    },
    {
      path: 'goods',
      element: <div>商品管理页（待实现）</div>,
    },
    {
      path: 'goods/create',
      element: <div>新增商品页（待实现）</div>,
    },
    {
      path: 'goods/:id/edit',
      element: <div>编辑商品页（待实现）</div>,
    },
    {
      path: 'seckill',
      element: <div>秒杀活动列表页（待实现）</div>,
    },
    {
      path: 'seckill/create',
      element: <div>创建秒杀活动页（待实现）</div>,
    },
    {
      path: 'seckill/:id/edit',
      element: <div>编辑秒杀活动页（待实现）</div>,
    },
    {
      path: 'seckill/:id/stats',
      element: <div>活动统计页（待实现）</div>,
    },
    {
      path: 'orders',
      element: <div>订单管理页（待实现）</div>,
    },
    {
      path: 'users',
      element: <div>用户管理页（待实现）</div>,
    },
  ],
};

// 管理后台登录页
const adminLoginRoute = {
  path: '/admin/login',
  element: <div>管理员登录页（待实现）</div>,
};

export const router = createBrowserRouter([
  ...mobileRoutes,
  adminRoutes,
  adminLoginRoute,
  {
    path: '*',
    element: <Navigate to="/" replace />,
  },
]);

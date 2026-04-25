import { createBrowserRouter, Navigate, type RouteObject } from 'react-router-dom';
import { MobileLayout, AdminLayout } from '../layouts';
import {
  Login,
  Home,
  Register,
  GoodsList,
  GoodsDetail,
  SeckillList,
  SeckillDetail,
  SeckillBuy,
  SeckillResult,
  Orders,
  OrderDetail,
  Pay,
  Profile,
  Address,
  Settings,
} from '../pages';
import {
  Dashboard,
  AdminLogin,
  AdminGoods,
  AdminGoodsCreate,
  AdminGoodsEdit,
  AdminSeckill,
  AdminSeckillCreate,
  AdminSeckillEdit,
  AdminSeckillStats,
  AdminOrders,
  AdminUsers,
} from '../pages/Admin';
import { UserAuthRoute, AdminAuthRoute } from './guards';

// 用户端路由配置
const mobileRoutes: RouteObject[] = [
  {
    path: '/login',
    element: <Login />,
  },
  {
    path: '/register',
    element: <Register />,
  },
  {
    element: <UserAuthRoute />,
    children: [
      {
        path: '/home',
        element: <MobileLayout title="首页" showBack={false} showTabBar={true}><Home /></MobileLayout>,
      },
      {
        path: '/',
        element: <Navigate to="/home" replace />,
      },
      {
        path: '/goods',
        element: <MobileLayout title="商品列表" showBack={true} showTabBar={true}><GoodsList /></MobileLayout>,
      },
      {
        path: '/goods/:id',
        element: <MobileLayout title="商品详情" showBack={true} showTabBar={false}><GoodsDetail /></MobileLayout>,
      },
      {
        path: '/seckill',
        element: <MobileLayout title="秒杀活动" showBack={true} showTabBar={true}><SeckillList /></MobileLayout>,
      },
      {
        path: '/seckill/:id',
        element: <MobileLayout title="秒杀活动详情" showBack={true} showTabBar={false}><SeckillDetail /></MobileLayout>,
      },
      {
        path: '/seckill/:id/buy',
        element: <MobileLayout title="抢购确认" showBack={true} showTabBar={false}><SeckillBuy /></MobileLayout>,
      },
      {
        path: '/seckill/result/:recordId',
        element: <MobileLayout title="秒杀结果" showBack={true} showTabBar={false}><SeckillResult /></MobileLayout>,
      },
      {
        path: '/orders',
        element: <MobileLayout title="我的订单" showBack={true} showTabBar={true}><Orders /></MobileLayout>,
      },
      {
        path: '/order/:orderNo',
        element: <MobileLayout title="订单详情" showBack={true} showTabBar={false}><OrderDetail /></MobileLayout>,
      },
      {
        path: '/pay/:orderNo',
        element: <MobileLayout title="支付" showBack={true} showTabBar={false}><Pay /></MobileLayout>,
      },
      {
        path: '/profile',
        element: <MobileLayout title="个人中心" showBack={false} showTabBar={true}><Profile /></MobileLayout>,
      },
      {
        path: '/address',
        element: <MobileLayout title="收货地址" showBack={true} showTabBar={false}><Address /></MobileLayout>,
      },
      {
        path: '/settings',
        element: <MobileLayout title="设置" showBack={true} showTabBar={false}><Settings /></MobileLayout>,
      },
    ],
  },
];

// 管理后台路由配置（不需要管理员权限的路由）
const adminPublicRoutes: RouteObject[] = [
  {
    path: '/admin/login',
    element: <AdminLogin />,
  },
];

// 管理后台路由配置（需要管理员权限的路由）
const adminAuthRoutes: RouteObject = {
  path: '/admin',
  element: <AdminAuthRoute />,
  children: [
    {
      element: <AdminLayout />,
      children: [
        {
          index: true,
          element: <Dashboard />,
        },
        {
          path: 'dashboard',
          element: <Dashboard />,
        },
        {
          path: 'goods',
          element: <AdminGoods />,
        },
        {
          path: 'goods/create',
          element: <AdminGoodsCreate />,
        },
        {
          path: 'goods/:id/edit',
          element: <AdminGoodsEdit />,
        },
        {
          path: 'seckill',
          element: <AdminSeckill />,
        },
        {
          path: 'seckill/create',
          element: <AdminSeckillCreate />,
        },
        {
          path: 'seckill/:id/edit',
          element: <AdminSeckillEdit />,
        },
        {
          path: 'seckill/:id/stats',
          element: <AdminSeckillStats />,
        },
        {
          path: 'orders',
          element: <AdminOrders />,
        },
        {
          path: 'users',
          element: <AdminUsers />,
        },
      ],
    },
  ],
};

export const router = createBrowserRouter([
  ...mobileRoutes,
  ...adminPublicRoutes,
  adminAuthRoutes,
  {
    path: '*',
    element: <Navigate to="/home" replace />,
  },
]);

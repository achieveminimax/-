import { Layout, Menu, Button, Avatar, Dropdown, Badge } from 'antd';
import {
  DashboardOutlined,
  ShoppingOutlined,
  ThunderboltOutlined,
  FileTextOutlined,
  UserOutlined,
  BellOutlined,
  LogoutOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
} from '@ant-design/icons';
import { useState } from 'react';
import { useNavigate, useLocation, Outlet } from 'react-router-dom';
import './index.css';

const { Header, Sider, Content } = Layout;

const menuItems = [
  {
    key: '/admin',
    icon: <DashboardOutlined />,
    label: '仪表盘',
  },
  {
    key: '/admin/goods',
    icon: <ShoppingOutlined />,
    label: '商品管理',
  },
  {
    key: '/admin/seckill',
    icon: <ThunderboltOutlined />,
    label: '秒杀管理',
  },
  {
    key: '/admin/orders',
    icon: <FileTextOutlined />,
    label: '订单管理',
  },
  {
    key: '/admin/users',
    icon: <UserOutlined />,
    label: '用户管理',
  },
];

export function AdminLayout() {
  const [collapsed, setCollapsed] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();

  const userDropdownItems = [
    {
      key: 'profile',
      icon: <UserOutlined />,
      label: '个人中心',
    },
    {
      key: 'logout',
      icon: <LogoutOutlined />,
      label: '退出登录',
    },
  ];

  const handleMenuClick = ({ key }: { key: string }) => {
    if (key === 'logout') {
      // 处理退出登录
      navigate('/admin/login');
    } else if (key === 'profile') {
      // 处理个人中心
    }
  };

  return (
    <Layout className="admin-layout">
      <Sider
        trigger={null}
        collapsible
        collapsed={collapsed}
        theme="light"
        className="admin-sider"
      >
        <div className="admin-logo">
          {collapsed ? '秒' : '秒杀管理系统'}
        </div>
        <Menu
          mode="inline"
          selectedKeys={[location.pathname]}
          items={menuItems}
          onClick={({ key }) => navigate(key)}
          className="admin-menu"
        />
      </Sider>
      
      <Layout>
        <Header className="admin-header">
          <Button
            type="text"
            icon={collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
            onClick={() => setCollapsed(!collapsed)}
            className="collapse-btn"
          />
          
          <div className="header-right">
            <Badge count={5} size="small">
              <Button type="text" icon={<BellOutlined />} />
            </Badge>
            
            <Dropdown
              menu={{ items: userDropdownItems, onClick: handleMenuClick }}
              placement="bottomRight"
            >
              <div className="user-info">
                <Avatar size="small" icon={<UserOutlined />} />
                <span className="username">管理员</span>
              </div>
            </Dropdown>
          </div>
        </Header>
        
        <Content className="admin-content">
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  );
}

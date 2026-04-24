import { TabBar as AntdTabBar } from 'antd-mobile';
import { 
  HomeOutlined, 
  ShoppingOutlined, 
  FileTextOutlined, 
  UserOutlined 
} from '@ant-design/icons';
import { useLocation, useNavigate } from 'react-router-dom';
import './index.css';

const tabs = [
  {
    key: '/',
    title: '首页',
    icon: <HomeOutlined />,
  },
  {
    key: '/goods',
    title: '商品',
    icon: <ShoppingOutlined />,
  },
  {
    key: '/orders',
    title: '订单',
    icon: <FileTextOutlined />,
  },
  {
    key: '/profile',
    title: '我的',
    icon: <UserOutlined />,
  },
];

export function TabBar() {
  const location = useLocation();
  const navigate = useNavigate();

  // 只在特定页面显示 TabBar
  const showTabBarPaths = ['/', '/goods', '/orders', '/profile'];
  if (!showTabBarPaths.includes(location.pathname)) {
    return null;
  }

  return (
    <div className="tab-bar-wrapper safe-area-bottom">
      <AntdTabBar
        activeKey={location.pathname}
        onChange={(key) => navigate(key)}
      >
        {tabs.map((item) => (
          <AntdTabBar.Item
            key={item.key}
            icon={item.icon}
            title={item.title}
          />
        ))}
      </AntdTabBar>
    </div>
  );
}

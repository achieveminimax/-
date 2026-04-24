import { useNavigate } from 'react-router-dom';
import { List, Avatar, Badge, Grid } from 'antd-mobile';
import {
  UserOutlined,
  EnvironmentOutlined,
  GiftOutlined,
  HeartOutlined,
  CustomerServiceOutlined,
  SettingOutlined,
  QuestionCircleOutlined,
  FileTextOutlined,
} from '@ant-design/icons';
import { MobileLayout } from '../../layouts';
import { useUserStore } from '../../store';
import './index.css';

export function Profile() {
  const navigate = useNavigate();
  const { user, logout } = useUserStore();

  const orderShortcuts = [
    { key: 'pending', icon: '💰', label: '待支付', count: 2 },
    { key: 'shipped', icon: '📦', label: '待发货', count: 1 },
    { key: 'completed', icon: '✅', label: '已完成', count: 0 },
  ];

  const services = [
    { key: 'address', icon: <EnvironmentOutlined />, label: '收货地址' },
    { key: 'coupon', icon: <GiftOutlined />, label: '优惠券' },
    { key: 'favorite', icon: <HeartOutlined />, label: '我的收藏' },
    { key: 'service', icon: <CustomerServiceOutlined />, label: '客服中心' },
  ];

  const handleServiceClick = (key: string) => {
    switch (key) {
      case 'address':
        navigate('/address');
        break;
      case 'coupon':
        // 优惠券
        break;
      case 'favorite':
        // 收藏
        break;
      case 'service':
        // 客服
        break;
    }
  };

  return (
    <MobileLayout showTabBar>
      <div className="profile-page">
        {/* 用户信息卡片 */}
        <div className="profile-header">
          <div className="user-info" onClick={() => navigate('/settings')}>
            <Avatar src={user?.avatar} className="user-avatar">
              {user?.username?.[0] || <UserOutlined />}
            </Avatar>
            <div className="user-detail">
              <h3 className="username">{user?.username || '未登录'}</h3>
              <p className="phone">{user?.phone || '点击登录'}</p>
            </div>
            <span className="arrow">&gt;</span>
          </div>
        </div>

        {/* 订单快捷入口 */}
        <div className="profile-section">
          <div className="section-title" onClick={() => navigate('/orders')}>
            <span>我的订单</span>
            <span className="view-all">查看全部 &gt;</span>
          </div>
          <Grid columns={3} gap={8} className="order-shortcuts">
            {orderShortcuts.map((item) => (
              <Grid.Item key={item.key}>
                <Badge content={item.count || null}>
                  <div className="shortcut-item">
                    <span className="shortcut-icon">{item.icon}</span>
                    <span className="shortcut-label">{item.label}</span>
                  </div>
                </Badge>
              </Grid.Item>
            ))}
          </Grid>
        </div>

        {/* 我的服务 */}
        <div className="profile-section">
          <div className="section-title">
            <span>我的服务</span>
          </div>
          <Grid columns={4} gap={8} className="service-grid">
            {services.map((item) => (
              <Grid.Item key={item.key}>
                <div
                  className="service-item"
                  onClick={() => handleServiceClick(item.key)}
                >
                  <span className="service-icon">{item.icon}</span>
                  <span className="service-label">{item.label}</span>
                </div>
              </Grid.Item>
            ))}
          </Grid>
        </div>

        {/* 更多功能 */}
        <div className="profile-section">
          <List>
            <List.Item
              prefix={<SettingOutlined />}
              onClick={() => navigate('/settings')}
              arrow
            >
              设置
            </List.Item>
            <List.Item
              prefix={<QuestionCircleOutlined />}
              onClick={() => {}}
              arrow
            >
              帮助中心
            </List.Item>
            <List.Item
              prefix={<FileTextOutlined />}
              onClick={() => {}}
              arrow
            >
              关于我们
            </List.Item>
          </List>
        </div>

        {/* 退出登录 */}
        {user && (
          <div className="profile-section">
            <div className="logout-btn" onClick={logout}>
              退出登录
            </div>
          </div>
        )}
      </div>
    </MobileLayout>
  );
}

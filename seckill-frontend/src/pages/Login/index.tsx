import { useState } from 'react';
import { Form, Input, Button, Checkbox, Toast } from 'antd-mobile';
import { EyeInvisibleOutlined, EyeOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { useUserStore } from '../../store';
import './index.css';

export function Login() {
  const navigate = useNavigate();
  const { setUser } = useUserStore();
  const [visible, setVisible] = useState(false);
  const [loading, setLoading] = useState(false);

  const onFinish = async (values: { username: string; password: string; remember?: boolean }) => {
    setLoading(true);
    
    // 模拟登录请求
    setTimeout(() => {
      setLoading(false);
      
      // 模拟登录成功
      setUser({
        id: 1,
        username: values.username,
        phone: '13800138000',
        token: 'mock_token_' + Date.now(),
      });
      
      Toast.show({
        icon: 'success',
        content: '登录成功',
      });
      
      navigate('/');
    }, 1000);
  };

  return (
    <div className="login-page">
      <div className="login-header">
        <div className="logo">
          <div className="logo-icon">秒</div>
          <h1 className="logo-text">欢迎回来</h1>
        </div>
      </div>

      <Form
        layout="vertical"
        onFinish={onFinish}
        footer={
          <Button
            block
            type="submit"
            color="primary"
            size="large"
            loading={loading}
            className="login-btn"
          >
            登 录
          </Button>
        }
        className="login-form"
      >
        <Form.Item
          name="username"
          label="用户名/手机号"
          rules={[{ required: true, message: '请输入用户名或手机号' }]}
        >
          <Input placeholder="请输入用户名或手机号" clearable />
        </Form.Item>

        <Form.Item
          name="password"
          label="密码"
          rules={[{ required: true, message: '请输入密码' }]}
        >
          <div className="password-input">
            <Input
              placeholder="请输入密码"
              type={visible ? 'text' : 'password'}
              clearable
            />
            <div className="eye-icon" onClick={() => setVisible(!visible)}>
              {visible ? <EyeOutlined /> : <EyeInvisibleOutlined />}
            </div>
          </div>
        </Form.Item>

        <div className="login-options">
          <Form.Item name="remember" noStyle valuePropName="checked">
            <Checkbox>记住我</Checkbox>
          </Form.Item>
          <span className="forgot-link">忘记密码？</span>
        </div>
      </Form>

      <div className="login-divider">
        <span>其他登录方式</span>
      </div>

      <div className="login-other">
        <div className="other-item">
          <div className="other-icon">📱</div>
          <span>手机号</span>
        </div>
        <div className="other-item">
          <div className="other-icon">💬</div>
          <span>微信</span>
        </div>
        <div className="other-item">
          <div className="other-icon">📧</div>
          <span>邮箱</span>
        </div>
      </div>

      <div className="login-footer">
        <span>还没有账号？</span>
        <span className="register-link" onClick={() => navigate('/register')}>
          立即注册
        </span>
      </div>
    </div>
  );
}

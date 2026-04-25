import { Result, Button } from 'antd';
import { useNavigate } from 'react-router-dom';

export function AdminLogin() {
  const navigate = useNavigate();

  return (
    <div style={{ 
      display: 'flex', 
      justifyContent: 'center', 
      alignItems: 'center', 
      height: '100vh',
      background: '#f5f5f5'
    }}>
      <div style={{ 
        background: '#fff', 
        padding: '40px', 
        borderRadius: '8px',
        boxShadow: '0 2px 8px rgba(0,0,0,0.1)',
        textAlign: 'center'
      }}>
        <Result
          status="info"
          title="管理员登录"
          subTitle="管理员登录页面待实现"
        />
        <Button type="primary" onClick={() => navigate('/admin')}>
          进入管理后台
        </Button>
      </div>
    </div>
  );
}

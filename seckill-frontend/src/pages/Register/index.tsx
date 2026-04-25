import { Result, Button } from 'antd-mobile';
import { useNavigate } from 'react-router-dom';

export function Register() {
  const navigate = useNavigate();

  return (
    <div style={{ padding: '20px' }}>
      <Result
        status="info"
        title="注册页"
        description="注册页面待实现"
      />
      <Button block onClick={() => navigate('/login')}>
        返回登录页
      </Button>
    </div>
  );
}

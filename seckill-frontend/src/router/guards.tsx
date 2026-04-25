import { useEffect } from 'react';
import { useNavigate, useLocation, Outlet } from 'react-router-dom';
import { useUserStore } from '../store';

export function UserAuthRoute() {
  const navigate = useNavigate();
  const location = useLocation();
  const { isLogin } = useUserStore();

  useEffect(() => {
    if (!isLogin) {
      navigate('/login', { replace: true, state: { from: location.pathname } });
    }
  }, [isLogin, navigate, location]);

  return isLogin ? <Outlet /> : null;
}

export function AdminAuthRoute() {
  const navigate = useNavigate();
  const location = useLocation();
  const { isAdminLogin } = useUserStore();

  useEffect(() => {
    if (!isAdminLogin) {
      navigate('/admin/login', {
        replace: true,
        state: { from: location.pathname },
      });
    }
  }, [isAdminLogin, navigate, location]);

  return isAdminLogin ? <Outlet /> : null;
}

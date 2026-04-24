import { NavBar, Toast } from 'antd-mobile';
import { useNavigate, useLocation } from 'react-router-dom';
import { TabBar } from '../../components';
import './index.css';

interface MobileLayoutProps {
  children: React.ReactNode;
  title?: string;
  showBack?: boolean;
  showTabBar?: boolean;
  right?: React.ReactNode;
  onBack?: () => void;
}

export function MobileLayout({
  children,
  title,
  showBack = false,
  showTabBar = true,
  right,
  onBack,
}: MobileLayoutProps) {
  const navigate = useNavigate();
  const location = useLocation();

  const handleBack = () => {
    if (onBack) {
      onBack();
    } else {
      navigate(-1);
    }
  };

  // 登录页和注册页不显示导航栏
  const hideNavBarPaths = ['/login', '/register'];
  const showNavBar = !hideNavBarPaths.includes(location.pathname);

  return (
    <div className="mobile-layout">
      {showNavBar && (
        <div className="mobile-nav-bar safe-area-top">
          <NavBar
            back={showBack ? '' : null}
            onBack={handleBack}
            right={right}
          >
            {title}
          </NavBar>
        </div>
      )}
      
      <div 
        className="mobile-content" 
        style={{ 
          paddingTop: showNavBar ? 'var(--header-height)' : 0,
          paddingBottom: showTabBar ? 'calc(var(--tabbar-height) + env(safe-area-inset-bottom))' : 0,
        }}
      >
        {children}
      </div>
      
      {showTabBar && <TabBar />}
    </div>
  );
}

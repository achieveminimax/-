import { useNavigate } from 'react-router-dom';
import { SearchBar, Swiper, Grid } from 'antd-mobile';
import { FireOutlined, ClockCircleOutlined } from '@ant-design/icons';
import { SeckillCard, Countdown } from '../../components';
import { SECKILL_STATUS } from '../../utils/constants';
import type { SeckillActivity, SeckillGoods } from '../../types';
import './index.css';

// 模拟数据
const mockBanners = [
  { id: 1, title: '今日秒杀专区', subtitle: '限时特惠 · 先到先得', image: 'https://via.placeholder.com/375x150/FF4D4F/FFFFFF?text=Seckill+Banner' },
  { id: 2, title: '爆款推荐', subtitle: '精选好物 · 超值抢购', image: 'https://via.placeholder.com/375x150/FF7A45/FFFFFF?text=Hot+Items' },
];

const mockOngoingActivities: SeckillActivity[] = [
  {
    id: 1,
    title: 'iPhone 秒杀专场',
    startTime: '2026-04-24T10:00:00',
    endTime: '2026-04-24T14:00:00',
    status: SECKILL_STATUS.IN_PROGRESS,
    goodsList: [
      {
        id: 1,
        goodsId: 101,
        goodsName: 'iPhone 16 Pro 256GB 钛金属原色',
        goodsImage: 'https://via.placeholder.com/200x200/F5F5F5/262626?text=iPhone',
        seckillPrice: 599900,
        originalPrice: 899900,
        stock: 100,
        sold: 78,
      },
      {
        id: 2,
        goodsId: 102,
        goodsName: '小米 14 Pro 16GB+512GB',
        goodsImage: 'https://via.placeholder.com/200x200/F5F5F5/262626?text=Xiaomi',
        seckillPrice: 399900,
        originalPrice: 549900,
        stock: 50,
        sold: 45,
      },
    ],
  },
];

const mockUpcomingActivities: SeckillGoods[] = [
  {
    id: 3,
    goodsId: 103,
    goodsName: 'MacBook Pro 14英寸 M3芯片',
    goodsImage: 'https://via.placeholder.com/200x200/F5F5F5/262626?text=MacBook',
    seckillPrice: 899900,
    originalPrice: 1299900,
    stock: 30,
    sold: 0,
  },
  {
    id: 4,
    goodsId: 104,
    goodsName: 'iPad Air 11英寸 M2芯片',
    goodsImage: 'https://via.placeholder.com/200x200/F5F5F5/262626?text=iPad',
    seckillPrice: 399900,
    originalPrice: 599900,
    stock: 50,
    sold: 0,
  },
];

const mockHotGoods: SeckillGoods[] = [
  {
    id: 5,
    goodsId: 105,
    goodsName: 'AirPods Pro 2',
    goodsImage: 'https://via.placeholder.com/200x200/F5F5F5/262626?text=AirPods',
    seckillPrice: 9900,
    originalPrice: 19900,
    stock: 200,
    sold: 156,
  },
  {
    id: 6,
    goodsId: 106,
    goodsName: 'Apple Watch Series 9',
    goodsImage: 'https://via.placeholder.com/200x200/F5F5F5/262626?text=Watch',
    seckillPrice: 19900,
    originalPrice: 29900,
    stock: 100,
    sold: 89,
  },
];

export function Home() {
  const navigate = useNavigate();

  return (
      <div className="home-page">
        {/* 搜索栏 */}
        <div className="home-search">
          <SearchBar 
            placeholder="搜索商品" 
            onFocus={() => navigate('/goods')}
          />
        </div>

        {/* Banner 轮播 */}
        <Swiper className="home-banner" autoplay loop>
          {mockBanners.map((banner) => (
            <Swiper.Item key={banner.id}>
              <div 
                className="banner-item"
                style={{ backgroundImage: `url(${banner.image})` }}
              >
                <div className="banner-content">
                  <h3><FireOutlined /> {banner.title}</h3>
                  <p>{banner.subtitle}</p>
                  <div className="banner-countdown">
                    <ClockCircleOutlined />
                    距结束 <Countdown targetTime={mockOngoingActivities[0]?.endTime || new Date()} />
                  </div>
                </div>
              </div>
            </Swiper.Item>
          ))}
        </Swiper>

        {/* 正在秒杀 */}
        <div className="home-section">
          <div className="section-header">
            <h3 className="section-title">
              <FireOutlined className="fire-icon" /> 正在秒杀
            </h3>
            <span className="view-more" onClick={() => navigate('/goods')}>
              查看更多
            </span>
          </div>
          
          <div className="seckill-grid">
            {mockOngoingActivities[0]?.goodsList.map((goods) => (
              <SeckillCard
                key={goods.id}
                goods={goods}
                status={SECKILL_STATUS.IN_PROGRESS}
                onClick={() => navigate(`/seckill/${mockOngoingActivities[0].id}`)}
              />
            ))}
          </div>
        </div>

        {/* 即将开始 */}
        <div className="home-section">
          <div className="section-header">
            <h3 className="section-title">
              <ClockCircleOutlined /> 即将开始
            </h3>
          </div>
          
          <div className="seckill-grid">
            {mockUpcomingActivities.map((goods) => (
              <SeckillCard
                key={goods.id}
                goods={goods}
                status={SECKILL_STATUS.NOT_STARTED}
                onClick={() => {}}
              />
            ))}
          </div>
        </div>

        {/* 热门商品 */}
        <div className="home-section">
          <div className="section-header">
            <h3 className="section-title">热门商品</h3>
          </div>
          
          <Grid columns={2} gap={8}>
            {mockHotGoods.map((goods) => (
              <Grid.Item key={goods.id}>
                <div 
                  className="hot-goods-card"
                  onClick={() => navigate(`/goods/${goods.goodsId}`)}
                >
                  <img src={goods.goodsImage} alt={goods.goodsName} />
                  <h4 className="ellipsis-2">{goods.goodsName}</h4>
                  <p className="hot-goods-price">¥{(goods.seckillPrice / 100).toFixed(0)}</p>
                </div>
              </Grid.Item>
            ))}
          </Grid>
        </div>
      </div>
  );
}

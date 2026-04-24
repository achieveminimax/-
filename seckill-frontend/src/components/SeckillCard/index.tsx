import { Button, ProgressBar } from 'antd-mobile';
import { FireOutlined } from '@ant-design/icons';
import type { SeckillGoods } from '../../types';
import { formatPrice } from '../../utils/format';
import { SECKILL_STATUS } from '../../utils/constants';
import './index.css';

interface SeckillCardProps {
  goods: SeckillGoods;
  status: number;
  onClick: () => void;
}

export function SeckillCard({ goods, status, onClick }: SeckillCardProps) {
  const soldPercent = Math.round((goods.sold / goods.stock) * 100);
  const isSoldOut = goods.sold >= goods.stock;

  const getButtonText = () => {
    if (status === SECKILL_STATUS.NOT_STARTED) return '提醒我';
    if (status === SECKILL_STATUS.ENDED) return '已结束';
    if (isSoldOut) return '已售罄';
    return '立即抢购';
  };

  const getButtonDisabled = () => {
    return status !== SECKILL_STATUS.IN_PROGRESS || isSoldOut;
  };

  return (
    <div className="seckill-card">
      <div className="seckill-card-image">
        <img src={goods.goodsImage} alt={goods.goodsName} />
        {status === SECKILL_STATUS.IN_PROGRESS && !isSoldOut && (
          <div className="seckill-card-tag">
            <FireOutlined /> 热销
          </div>
        )}
      </div>
      
      <div className="seckill-card-content">
        <h4 className="seckill-card-title ellipsis-2">{goods.goodsName}</h4>
        
        <div className="seckill-card-price">
          <span className="seckill-price">{formatPrice(goods.seckillPrice)}</span>
          <span className="original-price">{formatPrice(goods.originalPrice)}</span>
        </div>
        
        <div className="seckill-card-progress">
          <ProgressBar 
            percent={soldPercent} 
            style={{ '--fill-color': 'var(--color-primary)' }}
          />
          <span className="progress-text">已售 {soldPercent}%</span>
        </div>
        
        <Button 
          color={getButtonDisabled() ? 'default' : 'primary'}
          fill="solid"
          block
          disabled={getButtonDisabled()}
          onClick={onClick}
          className="seckill-card-btn"
        >
          {getButtonText()}
        </Button>
      </div>
    </div>
  );
}

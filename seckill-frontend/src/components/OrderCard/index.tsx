import { Tag, Button } from 'antd-mobile';
import type { Order } from '../../types';
import { ORDER_STATUS_TEXT, ORDER_STATUS_COLOR } from '../../utils/constants';
import { formatPrice, formatDateTime } from '../../utils/format';
import './index.css';

interface OrderCardProps {
  order: Order;
  onClick?: () => void;
  onPay?: () => void;
  onCancel?: () => void;
}

export function OrderCard({ order, onClick, onPay, onCancel }: OrderCardProps) {
  const statusText = ORDER_STATUS_TEXT[order.status];
  const statusColor = ORDER_STATUS_COLOR[order.status];

  return (
    <div className="order-card" onClick={onClick}>
      <div className="order-card-header">
        <span className="order-no">订单号: {order.orderNo}</span>
        <Tag color={statusColor} className="order-status">
          {statusText}
        </Tag>
      </div>

      <div className="order-card-body">
        {order.items.map((item) => (
          <div key={item.id} className="order-item">
            <img src={item.goodsImage} alt={item.goodsName} className="order-item-image" />
            <div className="order-item-info">
              <h4 className="order-item-name ellipsis-2">{item.goodsName}</h4>
              <div className="order-item-price">
                <span>{formatPrice(item.price)}</span>
                <span className="quantity">x{item.quantity}</span>
              </div>
            </div>
          </div>
        ))}
      </div>

      <div className="order-card-footer">
        <div className="order-total">
          共{order.items.reduce((sum, item) => sum + item.quantity, 0)}件
          <span className="total-amount">
            实付 <strong>{formatPrice(order.payAmount)}</strong>
          </span>
        </div>
        
        {order.status === 1 && (
          <div className="order-actions">
            <Button size="small" onClick={(e) => { e.stopPropagation(); onCancel?.(); }}>
              取消订单
            </Button>
            <Button 
              size="small" 
              color="primary" 
              onClick={(e) => { e.stopPropagation(); onPay?.(); }}
            >
              立即支付
            </Button>
          </div>
        )}
        
        <div className="order-time">{formatDateTime(order.createTime)}</div>
      </div>
    </div>
  );
}

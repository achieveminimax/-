import { useState } from 'react';
import { Tabs, Empty, PullToRefresh } from 'antd-mobile';
import { OrderCard } from '../../components';
import type { Order } from '../../types';
import { ORDER_STATUS } from '../../utils/constants';
import './index.css';

// 模拟订单数据
const mockOrders: Order[] = [
  {
    id: 1,
    orderNo: '20260501100001001',
    userId: 1,
    items: [
      {
        id: 1,
        goodsId: 101,
        goodsName: 'iPhone 16 Pro 256GB 钛金属原色',
        goodsImage: 'https://via.placeholder.com/200x200/F5F5F5/262626?text=iPhone',
        price: 599900,
        quantity: 1,
      },
    ],
    totalAmount: 599900,
    freightAmount: 0,
    payAmount: 599900,
    status: ORDER_STATUS.PENDING,
    createTime: '2026-05-01 10:00:00',
    expireTime: '2026-05-01 10:15:00',
  },
  {
    id: 2,
    orderNo: '20260501100001002',
    userId: 1,
    items: [
      {
        id: 2,
        goodsId: 102,
        goodsName: '小米 14 Pro 16GB+512GB',
        goodsImage: 'https://via.placeholder.com/200x200/F5F5F5/262626?text=Xiaomi',
        price: 399900,
        quantity: 1,
      },
    ],
    totalAmount: 399900,
    freightAmount: 0,
    payAmount: 399900,
    status: ORDER_STATUS.PAID,
    payType: 2,
    payTime: '2026-05-01 10:05:30',
    createTime: '2026-05-01 10:00:00',
  },
  {
    id: 3,
    orderNo: '20260501100001003',
    userId: 1,
    items: [
      {
        id: 3,
        goodsId: 103,
        goodsName: 'MacBook Pro 14英寸 M3芯片',
        goodsImage: 'https://via.placeholder.com/200x200/F5F5F5/262626?text=MacBook',
        price: 899900,
        quantity: 1,
      },
    ],
    totalAmount: 899900,
    freightAmount: 0,
    payAmount: 899900,
    status: ORDER_STATUS.COMPLETED,
    payType: 1,
    payTime: '2026-04-25 14:30:00',
    createTime: '2026-04-25 14:00:00',
  },
];

const tabs = [
  { key: 'all', title: '全部' },
  { key: 'pending', title: '待支付' },
  { key: 'paid', title: '已支付' },
  { key: 'shipped', title: '已发货' },
  { key: 'completed', title: '已完成' },
];

export function Orders() {
  const [activeTab, setActiveTab] = useState('all');
  const [orders] = useState(mockOrders);

  const getFilteredOrders = () => {
    if (activeTab === 'all') return orders;
    const statusMap: Record<string, number> = {
      pending: ORDER_STATUS.PENDING,
      paid: ORDER_STATUS.PAID,
      shipped: ORDER_STATUS.SHIPPED,
      completed: ORDER_STATUS.COMPLETED,
    };
    return orders.filter((order) => order.status === statusMap[activeTab]);
  };

  const onRefresh = async () => {
    // 模拟刷新
    await new Promise((resolve) => setTimeout(resolve, 1000));
  };

  const handlePay = (order: Order) => {
    // 处理支付
    console.log('Pay order:', order);
  };

  const handleCancel = (order: Order) => {
    // 处理取消
    console.log('Cancel order:', order);
  };

  const filteredOrders = getFilteredOrders();

  return (
      <div className="orders-page">
        <Tabs activeKey={activeTab} onChange={setActiveTab} className="order-tabs">
          {tabs.map((tab) => (
            <Tabs.Tab key={tab.key} title={tab.title} />
          ))}
        </Tabs>

        <PullToRefresh onRefresh={onRefresh}>
          <div className="order-list">
            {filteredOrders.length > 0 ? (
              filteredOrders.map((order) => (
                <OrderCard
                  key={order.id}
                  order={order}
                  onPay={() => handlePay(order)}
                  onCancel={() => handleCancel(order)}
                />
              ))
            ) : (
              <Empty
                description="暂无订单"
                style={{ padding: '60px 0' }}
              />
            )}
          </div>
        </PullToRefresh>
      </div>
  );
}

import { Card, Row, Col, Statistic, Table, Tag } from 'antd';
import {
  ShoppingOutlined,
  DollarOutlined,
  ThunderboltOutlined,
  UserOutlined,
  ArrowUpOutlined,
} from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import type { SeckillActivity } from '../../../types';
import { SECKILL_STATUS_TEXT } from '../../../utils/constants';
import './index.css';

// 模拟统计数据
const statsData = [
  {
    title: '今日订单',
    value: 128,
    icon: <ShoppingOutlined />,
    growth: 12,
    color: '#FF4D4F',
  },
  {
    title: '今日成交',
    value: 256000,
    prefix: '¥',
    icon: <DollarOutlined />,
    growth: 8,
    color: '#52C41A',
  },
  {
    title: '活动数量',
    value: 12,
    icon: <ThunderboltOutlined />,
    growth: 3,
    color: '#FAAD14',
  },
  {
    title: '用户总数',
    value: 5680,
    icon: <UserOutlined />,
    growth: 156,
    color: '#1890FF',
  },
];

// 模拟活动数据
const activityData: SeckillActivity[] = [
  {
    id: 1,
    title: 'iPhone 16 Pro 秒杀',
    goodsList: [],
    startTime: '2026-04-24 10:00:00',
    endTime: '2026-04-24 14:00:00',
    status: 1,
  },
  {
    id: 2,
    title: '小米 14 Pro 秒杀',
    goodsList: [],
    startTime: '2026-04-24 14:00:00',
    endTime: '2026-04-24 18:00:00',
    status: 1,
  },
  {
    id: 3,
    title: 'MacBook Pro 秒杀',
    goodsList: [],
    startTime: '2026-04-25 10:00:00',
    endTime: '2026-04-25 14:00:00',
    status: 0,
  },
];

const columns: ColumnsType<SeckillActivity> = [
  {
    title: '活动名称',
    dataIndex: 'title',
    key: 'title',
  },
  {
    title: '开始时间',
    dataIndex: 'startTime',
    key: 'startTime',
  },
  {
    title: '结束时间',
    dataIndex: 'endTime',
    key: 'endTime',
  },
  {
    title: '状态',
    dataIndex: 'status',
    key: 'status',
    render: (status: number) => (
      <Tag color={status === 1 ? 'success' : status === 0 ? 'warning' : 'default'}>
        {SECKILL_STATUS_TEXT[status]}
      </Tag>
    ),
  },
  {
    title: '操作',
    key: 'action',
    render: (_, record) => (
      <a href={`/admin/seckill/${record.id}/stats`}>查看详情</a>
    ),
  },
];

export function Dashboard() {
  return (
    <div className="dashboard-page">
      {/* 数据概览 */}
      <Row gutter={[16, 16]} className="stats-row">
        {statsData.map((stat, index) => (
          <Col xs={24} sm={12} lg={6} key={index}>
            <Card className="stat-card">
              <div className="stat-icon" style={{ background: `${stat.color}20`, color: stat.color }}>
                {stat.icon}
              </div>
              <Statistic
                title={stat.title}
                value={stat.value}
                prefix={stat.prefix}
                valueStyle={{ color: stat.color }}
              />
              <div className="stat-growth">
                <ArrowUpOutlined /> {stat.growth}%
              </div>
            </Card>
          </Col>
        ))}
      </Row>

      {/* 进行中的活动 */}
      <Card title="进行中的活动" className="activity-card">
        <Table
          columns={columns}
          dataSource={activityData}
          rowKey="id"
          pagination={false}
        />
      </Card>
    </div>
  );
}

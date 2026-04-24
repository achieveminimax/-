// 电商秒杀系统 - 格式化工具

import dayjs from 'dayjs';

/**
 * 格式化金额
 * @param amount 金额（分）
 * @param prefix 前缀
 * @returns 格式化后的金额字符串
 */
export function formatAmount(amount: number, prefix = '¥'): string {
  return `${prefix}${(amount / 100).toFixed(2)}`;
}

/**
 * 格式化价格（不带小数点后多余的0）
 * @param price 价格
 * @returns 格式化后的价格字符串
 */
export function formatPrice(price: number): string {
  if (price % 100 === 0) {
    return `¥${(price / 100).toFixed(0)}`;
  }
  return `¥${(price / 100).toFixed(2)}`;
}

/**
 * 格式化手机号（隐藏中间4位）
 * @param phone 手机号
 * @returns 格式化后的手机号
 */
export function formatPhone(phone: string): string {
  if (!phone || phone.length !== 11) return phone;
  return `${phone.slice(0, 3)}****${phone.slice(7)}`;
}

/**
 * 格式化日期时间
 * @param date 日期
 * @param format 格式
 * @returns 格式化后的日期字符串
 */
export function formatDateTime(date: string | Date, format = 'YYYY-MM-DD HH:mm:ss'): string {
  return dayjs(date).format(format);
}

/**
 * 格式化日期
 * @param date 日期
 * @returns 格式化后的日期字符串
 */
export function formatDate(date: string | Date): string {
  return dayjs(date).format('YYYY-MM-DD');
}

/**
 * 格式化时间
 * @param date 日期
 * @returns 格式化后的时间字符串
 */
export function formatTime(date: string | Date): string {
  return dayjs(date).format('HH:mm:ss');
}

/**
 * 格式化倒计时
 * @param seconds 剩余秒数
 * @returns 格式化后的倒计时字符串
 */
export function formatCountdown(seconds: number): string {
  const hours = Math.floor(seconds / 3600);
  const minutes = Math.floor((seconds % 3600) / 60);
  const secs = seconds % 60;
  return `${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;
}

/**
 * 格式化数字（添加千分位）
 * @param num 数字
 * @returns 格式化后的数字字符串
 */
export function formatNumber(num: number): string {
  return num.toLocaleString('zh-CN');
}

/**
 * 格式化百分比
 * @param value 数值（0-1）
 * @param decimals 小数位数
 * @returns 格式化后的百分比字符串
 */
export function formatPercent(value: number, decimals = 2): string {
  return `${(value * 100).toFixed(decimals)}%`;
}

/**
 * 截断文本
 * @param text 文本
 * @param maxLength 最大长度
 * @param suffix 后缀
 * @returns 截断后的文本
 */
export function truncateText(text: string, maxLength: number, suffix = '...'): string {
  if (text.length <= maxLength) return text;
  return text.slice(0, maxLength) + suffix;
}

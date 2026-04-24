# 电商秒杀系统 API 接口文档

> **版本：** v1.0.0
> **更新日期：** 2026-04-23
> **技术栈：** Spring Boot 3.x + MyBatis-Plus + Redis + RabbitMQ + MySQL

---

## 目录

- [1. 文档概述](#1-文档概述)
- [2. 通用规范](#2-通用规范)
- [3. 认证接口](#3-认证接口)
- [4. 用户接口](#4-用户接口)
- [5. 收货地址接口](#5-收货地址接口)
- [6. 商品分类接口](#6-商品分类接口)
- [7. 商品接口](#7-商品接口)
- [8. 秒杀接口](#8-秒杀接口)
- [9. 订单接口](#9-订单接口)
- [10. 支付接口](#10-支付接口)
- [11. 管理后台接口](#11-管理后台接口)

---

## 1. 文档概述

### 1.1 项目简介

本文档为电商秒杀系统的后端 API 接口文档，涵盖用户认证、商品管理、秒杀活动、订单支付及管理后台等完整业务模块。系统采用前后端分离架构，后端基于 Spring Boot 3.x 构建，使用 Redis 实现缓存与分布式锁，RabbitMQ 处理异步订单消息，MySQL 作为持久化存储。

### 1.2 Base URL

| 环境 | Base URL |
|------|----------|
| 开发环境 | `http://localhost:8080` |
| 测试环境 | `https://api-test.seckill.com` |
| 生产环境 | `https://api.seckill.com` |

### 1.3 认证方式

系统采用 **JWT（JSON Web Token）** 双 Token 认证机制：

- **Access Token：** 用于接口身份验证，有效期 30 分钟，放置于请求头 `Authorization` 中。
- **Refresh Token：** 用于刷新 Access Token，有效期 7 天，登录时返回。

**请求头格式：**

```
Authorization: Bearer <token>
```

**Token 刷新流程：**

1. Access Token 过期后，前端使用 Refresh Token 调用 `/api/user/refresh-token` 接口。
2. 后端验证 Refresh Token 有效性，返回新的 Access Token。
3. 若 Refresh Token 也已过期，需用户重新登录。

### 1.4 接口规范

- 数据交换格式：`application/json`
- 字符编码：`UTF-8`
- 时间格式：`yyyy-MM-dd HH:mm:ss`
- 所有接口均使用 HTTPS 协议（生产环境）
- 接口版本通过 URL 路径区分，当前版本为 v1（默认省略）

---

## 2. 通用规范

### 2.1 统一响应格式

所有接口返回统一的 JSON 响应结构：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {}
}
```

**字段说明：**

| 字段 | 类型 | 说明 |
|------|------|------|
| code | Integer | 业务状态码，200 表示成功 |
| message | String | 响应提示信息 |
| data | Object | 响应数据，失败时为 null |

**分页响应格式：**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "records": [],
    "total": 100,
    "size": 10,
    "current": 1,
    "pages": 10
  }
}
```

**分页字段说明：**

| 字段 | 类型 | 说明 |
|------|------|------|
| records | Array | 当前页数据列表 |
| total | Long | 总记录数 |
| size | Integer | 每页大小 |
| current | Integer | 当前页码 |
| pages | Integer | 总页数 |

### 2.2 状态码定义

| 状态码 | 说明 |
|--------|------|
| 200 | 操作成功 |
| 201 | 创建成功 |
| 400 | 请求参数错误 |
| 401 | 未认证（Token 缺失或无效） |
| 403 | 无权限访问 |
| 404 | 资源不存在 |
| 409 | 资源冲突（如重复注册） |
| 429 | 请求过于频繁（触发限流） |
| 500 | 服务器内部错误 |

### 2.3 分页参数

分页查询接口统一使用以下 Query 参数：

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| current | Integer | 否 | 1 | 当前页码，从 1 开始 |
| size | Integer | 否 | 10 | 每页大小，最大 100 |

### 2.4 错误码表

| 错误码 | 说明 |
|--------|------|
| 200 | 操作成功 |
| 201 | 创建成功 |
| 400 | 请求参数错误 |
| 40001 | 用户名或密码不能为空 |
| 40002 | 用户名格式不正确（4-20位字母数字下划线） |
| 40003 | 密码格式不正确（6-20位，需包含字母和数字） |
| 40004 | 手机号格式不正确 |
| 40005 | 邮箱格式不正确 |
| 40006 | 验证码不能为空 |
| 40007 | 验证码错误或已过期 |
| 40008 | 商品库存不足 |
| 40009 | 秒杀活动未开始 |
| 40010 | 秒杀活动已结束 |
| 40011 | 重复秒杀 |
| 40012 | 订单状态异常 |
| 40013 | 支付金额不正确 |
| 40014 | 收货地址信息不完整 |
| 40015 | 文件上传格式不支持 |
| 40016 | 文件大小超出限制 |
| 40101 | Token 缺失 |
| 40102 | Token 无效或已过期 |
| 40103 | Refresh Token 无效或已过期 |
| 40301 | 无权限访问该资源 |
| 40302 | 账号已被禁用 |
| 40401 | 用户不存在 |
| 40402 | 商品不存在 |
| 40403 | 秒杀活动不存在 |
| 40404 | 订单不存在 |
| 40405 | 收货地址不存在 |
| 40406 | 分类不存在 |
| 40901 | 用户名已存在 |
| 40902 | 手机号已注册 |
| 42901 | 请求过于频繁，请稍后再试 |
| 42902 | 秒杀接口限流，请稍后再试 |
| 50001 | 服务器内部错误 |
| 50002 | Redis 连接异常 |
| 50003 | RabbitMQ 消息发送失败 |
| 50004 | 数据库操作异常 |
| 50005 | 文件上传失败 |
| 50006 | 第三方支付服务异常 |

---

## 3. 认证接口

### 3.1 用户注册

**接口描述：** 新用户注册账号，注册成功后自动登录并返回 Token。

- **请求方式：** `POST`
- **请求路径：** `/api/user/register`
- **请求头：** `Content-Type: application/json`

**请求参数（Body - JSON）：**

| 字段 | 类型 | 必填 | 约束 | 描述 |
|------|------|------|------|------|
| username | String | 是 | 4-20位，字母、数字、下划线 | 用户名 |
| password | String | 是 | 6-20位，需包含字母和数字 | 密码 |
| confirmPassword | String | 是 | 需与 password 一致 | 确认密码 |
| phone | String | 是 | 11位手机号 | 手机号 |
| verifyCode | String | 是 | 6位数字 | 短信验证码 |
| email | String | 否 | 合法邮箱格式 | 邮箱 |
| nickname | String | 否 | 2-20位 | 昵称，不填则默认为用户名 |

**成功响应示例：**

```json
{
  "code": 201,
  "message": "注册成功",
  "data": {
    "userId": 10001,
    "username": "zhangsan",
    "nickname": "张三",
    "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMDAwMSIsImlhdCI6MTcxMzg0MDAwMCwiZXhwIjoxNzEzODQxODAwfQ.xxx",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMDAwMSIsImlhdCI6MTcxMzg0MDAwMCwiZXhwIjoxNzE0NDQ0ODAwfQ.yyy",
    "expiresIn": 1800
  }
}
```

**失败响应示例：**

```json
{
  "code": 40901,
  "message": "用户名已存在",
  "data": null
}
```

```json
{
  "code": 40003,
  "message": "密码格式不正确（6-20位，需包含字母和数字）",
  "data": null
}
```

**业务规则：**

1. 用户名、手机号需唯一，重复则返回对应错误码。
2. 密码需使用 BCrypt 加密后存储。
3. 注册成功后自动生成 JWT Token 返回。
4. 注册信息写入数据库后，异步发送欢迎邮件（RabbitMQ）。

**备注：**

- 限流规则：同一 IP 每分钟最多注册 5 次。
- 缓存策略：注册成功后将用户基本信息缓存至 Redis。

---

### 3.2 用户登录

**接口描述：** 用户使用账号（用户名或手机号）和密码登录，成功后返回双 Token。

- **请求方式：** `POST`
- **请求路径：** `/api/user/login`
- **请求头：** `Content-Type: application/json`

**请求参数（Body - JSON）：**

| 字段 | 类型 | 必填 | 约束 | 描述 |
|------|------|------|------|------|
| account | String | 是 | 用户名或手机号 | 账号 |
| password | String | 是 | - | 密码（明文传输，HTTPS 加密） |

**成功响应示例：**

```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "userId": 10001,
    "account": "zhangsan",
    "nickname": "张三",
    "avatar": "https://cdn.seckill.com/avatar/10001.jpg",
    "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMDAwMSIsImlhdCI6MTcxMzg0MDAwMCwiZXhwIjoxNzEzODQxODAwfQ.xxx",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMDAwMSIsImlhdCI6MTcxMzg0MDAwMCwiZXhwIjoxNzE0NDQ0ODAwfQ.yyy",
    "expiresIn": 1800
  }
}
```

**失败响应示例：**

```json
{
  "code": 40401,
  "message": "用户不存在",
  "data": null
}
```

```json
{
  "code": 400,
  "message": "账号或密码错误",
  "data": null
}
```

```json
{
  "code": 40302,
  "message": "账号已被禁用",
  "data": null
}
```

**业务规则：**

1. 密码错误连续 5 次，账号锁定 30 分钟。
2. 登录成功后将 Token 信息存入 Redis，支持主动失效。
3. 同一账号不允许多端同时登录，新登录会使旧 Token 失效。

**备注：**

- 限流规则：同一 IP 每分钟最多登录 10 次，同一账号每分钟最多 5 次。
- 安全策略：密码错误次数记录在 Redis 中，Key 格式为 `login:fail:{account}`。

---

### 3.3 用户登出

**接口描述：** 用户退出登录，使当前 Token 失效。

- **请求方式：** `POST`
- **请求路径：** `/api/user/logout`
- **请求头：** `Authorization: Bearer <token>`

**请求参数：** 无

**成功响应示例：**

```json
{
  "code": 200,
  "message": "登出成功",
  "data": null
}
```

**失败响应示例：**

```json
{
  "code": 40102,
  "message": "Token 无效或已过期",
  "data": null
}
```

**业务规则：**

1. 登出时将当前 Access Token 加入 Redis 黑名单，TTL 与 Token 剩余有效期一致。
2. 同时清除 Refresh Token。

**备注：**

- 缓存策略：Token 黑名单存储在 Redis 中，Key 格式为 `token:blacklist:{token}`。

---

### 3.4 Token 刷新

**接口描述：** 使用 Refresh Token 获取新的 Access Token。

- **请求方式：** `POST`
- **请求路径：** `/api/user/refresh-token`
- **请求头：** `Content-Type: application/json`

**请求参数（Body - JSON）：**

| 字段 | 类型 | 必填 | 约束 | 描述 |
|------|------|------|------|------|
| refreshToken | String | 是 | - | 刷新令牌 |

**成功响应示例：**

```json
{
  "code": 200,
  "message": "Token 刷新成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMDAwMSIsImlhdCI6MTcxMzg0MTgwMCwiZXhwIjoxNzEzODQzNjAwfQ.zzz",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMDAwMSIsImlhdCI6MTcxMzg0MTgwMCwiZXhwIjoxNzE0NDQ2NjAwfQ.www",
    "expiresIn": 1800
  }
}
```

**失败响应示例：**

```json
{
  "code": 40103,
  "message": "Refresh Token 无效或已过期",
  "data": null
}
```

**业务规则：**

1. 验证 Refresh Token 有效性和是否在黑名单中。
2. 刷新成功后，旧的 Refresh Token 立即失效。
3. 采用 Refresh Token 轮转机制，每次刷新生成新的 Refresh Token。

**备注：**

- 安全策略：Refresh Token 仅限使用一次，刷新后旧 Token 立即失效。

---

## 4. 用户接口

> 以下接口均需要在请求头中携带 `Authorization: Bearer <token>`。

### 4.1 获取当前用户信息

**接口描述：** 获取当前登录用户的详细信息。

- **请求方式：** `GET`
- **请求路径：** `/api/user/info`
- **请求头：** `Authorization: Bearer <token>`

**请求参数：** 无

**成功响应示例：**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "userId": 10001,
    "username": "zhangsan",
    "nickname": "张三",
    "avatar": "https://cdn.seckill.com/avatar/10001.jpg",
    "phone": "138****1234",
    "email": "zhangsan@example.com",
    "gender": 1,
    "birthday": "2000-01-15",
    "status": 1,
    "createTime": "2026-01-10 10:30:00",
    "lastLoginTime": "2026-04-23 09:15:00"
  }
}
```

**失败响应示例：**

```json
{
  "code": 40102,
  "message": "Token 无效或已过期",
  "data": null
}
```

**业务规则：**

1. 从 Token 中解析用户 ID，查询用户信息。
2. 手机号中间四位脱敏显示。

**备注：**

- 缓存策略：用户信息缓存在 Redis 中，Key 格式为 `user:info:{userId}`，TTL 30 分钟。

---

### 4.2 修改个人信息

**接口描述：** 修改当前用户的个人信息。

- **请求方式：** `PUT`
- **请求路径：** `/api/user/info`
- **请求头：** `Authorization: Bearer <token>`、`Content-Type: application/json`

**请求参数（Body - JSON）：**

| 字段 | 类型 | 必填 | 约束 | 描述 |
|------|------|------|------|------|
| nickname | String | 否 | 2-20位 | 昵称 |
| gender | Integer | 否 | 0-未知 1-男 2-女 | 性别 |
| birthday | String | 否 | yyyy-MM-dd 格式 | 生日 |
| email | String | 否 | 合法邮箱格式 | 邮箱 |

**成功响应示例：**

```json
{
  "code": 200,
  "message": "修改成功",
  "data": null
}
```

**失败响应示例：**

```json
{
  "code": 40005,
  "message": "邮箱格式不正确",
  "data": null
}
```

**业务规则：**

1. 仅允许修改非敏感信息，用户名不可修改。
2. 修改成功后清除 Redis 中的用户信息缓存。

---

### 4.3 修改密码

**接口描述：** 修改当前用户密码。

- **请求方式：** `PUT`
- **请求路径：** `/api/user/password`
- **请求头：** `Authorization: Bearer <token>`、`Content-Type: application/json`

**请求参数（Body - JSON）：**

| 字段 | 类型 | 必填 | 约束 | 描述 |
|------|------|------|------|------|
| oldPassword | String | 是 | - | 原密码 |
| newPassword | String | 是 | 6-20位，需包含字母和数字 | 新密码 |
| confirmPassword | String | 是 | 需与 newPassword 一致 | 确认新密码 |

**成功响应示例：**

```json
{
  "code": 200,
  "message": "密码修改成功",
  "data": null
}
```

**失败响应示例：**

```json
{
  "code": 400,
  "message": "原密码错误",
  "data": null
}
```

**业务规则：**

1. 验证原密码正确性。
2. 新密码不能与最近 3 次使用的密码相同。
3. 修改成功后，当前 Token 及所有 Refresh Token 立即失效，需重新登录。
4. 异步发送密码变更通知邮件（RabbitMQ）。

---

### 4.4 上传头像

**接口描述：** 上传或更新用户头像。

- **请求方式：** `POST`
- **请求路径：** `/api/user/avatar`
- **请求头：** `Authorization: Bearer <token>`、`Content-Type: multipart/form-data`

**请求参数（Body - Form Data）：**

| 字段 | 类型 | 必填 | 约束 | 描述 |
|------|------|------|------|------|
| avatar | File | 是 | 支持 jpg/jpeg/png/gif，最大 2MB | 头像文件 |

**成功响应示例：**

```json
{
  "code": 200,
  "message": "头像上传成功",
  "data": {
    "avatarUrl": "https://cdn.seckill.com/avatar/10001_1713840000.jpg"
  }
}
```

**失败响应示例：**

```json
{
  "code": 40015,
  "message": "文件上传格式不支持",
  "data": null
}
```

```json
{
  "code": 40016,
  "message": "文件大小超出限制",
  "data": null
}
```

**业务规则：**

1. 上传文件格式校验：仅支持 jpg、jpeg、png、gif。
2. 文件大小限制：最大 2MB。
3. 上传成功后生成缩略图（200x200），原图保留用于展示。
4. 文件命名规则：`{userId}_{timestamp}.{ext}`。
5. 上传成功后清除 Redis 中的用户信息缓存。

**备注：**

- 存储策略：文件存储至 OSS（对象存储服务），本地开发环境存储至 `uploads/avatar/` 目录。

---

## 5. 收货地址接口

> 以下接口均需要在请求头中携带 `Authorization: Bearer <token>`。

### 5.1 新增地址

**接口描述：** 新增收货地址，每个用户最多保存 10 个地址。

- **请求方式：** `POST`
- **请求路径：** `/api/address`
- **请求头：** `Authorization: Bearer <token>`、`Content-Type: application/json`

**请求参数（Body - JSON）：**

| 字段 | 类型 | 必填 | 约束 | 描述 |
|------|------|------|------|------|
| receiverName | String | 是 | 2-20位 | 收货人姓名 |
| receiverPhone | String | 是 | 11位手机号 | 收货人手机号 |
| province | String | 是 | - | 省 |
| city | String | 是 | - | 市 |
| district | String | 是 | - | 区 |
| detailAddress | String | 是 | 5-200位 | 详细地址 |
| isDefault | Integer | 否 | 0-否 1-是，默认 0 | 是否设为默认地址 |

**成功响应示例：**

```json
{
  "code": 201,
  "message": "地址添加成功",
  "data": {
    "addressId": 5001,
    "receiverName": "张三",
    "receiverPhone": "13812341234",
    "province": "浙江省",
    "city": "杭州市",
    "district": "西湖区",
    "detailAddress": "文三路 100 号 XX 小区 1 栋 502 室",
    "isDefault": 1,
    "createTime": "2026-04-23 10:00:00"
  }
}
```

**失败响应示例：**

```json
{
  "code": 40014,
  "message": "收货地址信息不完整",
  "data": null
}
```

**业务规则：**

1. 每个用户最多保存 10 个地址，超出限制返回错误。
2. 若 `isDefault` 为 1，则将原默认地址取消默认。
3. 若用户无任何地址，第一个地址自动设为默认。

---

### 5.2 编辑地址

**接口描述：** 修改已有的收货地址信息。

- **请求方式：** `PUT`
- **请求路径：** `/api/address/{id}`
- **请求头：** `Authorization: Bearer <token>`、`Content-Type: application/json`

**路径参数：**

| 字段 | 类型 | 必填 | 描述 |
|------|------|------|------|
| id | Long | 是 | 地址 ID |

**请求参数（Body - JSON）：**

| 字段 | 类型 | 必填 | 约束 | 描述 |
|------|------|------|------|------|
| receiverName | String | 否 | 2-20位 | 收货人姓名 |
| receiverPhone | String | 否 | 11位手机号 | 收货人手机号 |
| province | String | 否 | - | 省 |
| city | String | 否 | - | 市 |
| district | String | 否 | - | 区 |
| detailAddress | String | 否 | 5-200位 | 详细地址 |
| isDefault | Integer | 否 | 0-否 1-是 | 是否设为默认地址 |

**成功响应示例：**

```json
{
  "code": 200,
  "message": "地址修改成功",
  "data": null
}
```

**失败响应示例：**

```json
{
  "code": 40405,
  "message": "收货地址不存在",
  "data": null
}
```

```json
{
  "code": 40301,
  "message": "无权限访问该资源",
  "data": null
}
```

**业务规则：**

1. 仅允许修改自己名下的地址。
2. 若将 `isDefault` 设为 1，则将原默认地址取消默认。

---

### 5.3 删除地址

**接口描述：** 删除指定的收货地址。

- **请求方式：** `DELETE`
- **请求路径：** `/api/address/{id}`
- **请求头：** `Authorization: Bearer <token>`

**路径参数：**

| 字段 | 类型 | 必填 | 描述 |
|------|------|------|------|
| id | Long | 是 | 地址 ID |

**成功响应示例：**

```json
{
  "code": 200,
  "message": "地址删除成功",
  "data": null
}
```

**失败响应示例：**

```json
{
  "code": 40405,
  "message": "收货地址不存在",
  "data": null
}
```

**业务规则：**

1. 仅允许删除自己名下的地址。
2. 若删除的是默认地址，则自动将最早创建的地址设为默认。

---

### 5.4 地址列表

**接口描述：** 获取当前用户的所有收货地址列表，默认地址排在首位。

- **请求方式：** `GET`
- **请求路径：** `/api/address/list`
- **请求头：** `Authorization: Bearer <token>`

**请求参数：** 无

**成功响应示例：**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "addressId": 5001,
      "receiverName": "张三",
      "receiverPhone": "13812341234",
      "province": "浙江省",
      "city": "杭州市",
      "district": "西湖区",
      "detailAddress": "文三路 100 号 XX 小区 1 栋 502 室",
      "isDefault": 1,
      "createTime": "2026-04-23 10:00:00",
      "updateTime": "2026-04-23 10:00:00"
    },
    {
      "addressId": 5002,
      "receiverName": "张三",
      "receiverPhone": "13812341234",
      "province": "北京市",
      "city": "北京市",
      "district": "朝阳区",
      "detailAddress": "建国路 88 号 XX 大厦 12 层",
      "isDefault": 0,
      "createTime": "2026-04-20 14:30:00",
      "updateTime": "2026-04-20 14:30:00"
    }
  ]
}
```

**业务规则：**

1. 默认地址始终排在列表首位。
2. 其余地址按创建时间倒序排列。

---

### 5.5 设置默认地址

**接口描述：** 将指定地址设为默认收货地址。

- **请求方式：** `PUT`
- **请求路径：** `/api/address/default/{id}`
- **请求头：** `Authorization: Bearer <token>`

**路径参数：**

| 字段 | 类型 | 必填 | 描述 |
|------|------|------|------|
| id | Long | 是 | 地址 ID |

**成功响应示例：**

```json
{
  "code": 200,
  "message": "默认地址设置成功",
  "data": null
}
```

**失败响应示例：**

```json
{
  "code": 40405,
  "message": "收货地址不存在",
  "data": null
}
```

**业务规则：**

1. 仅允许操作自己名下的地址。
2. 设置新默认地址时，自动取消原默认地址。

---

## 6. 商品分类接口

### 6.1 分类列表

**接口描述：** 获取商品分类树形结构，用于前台分类导航。

- **请求方式：** `GET`
- **请求路径：** `/api/category/list`
- **请求头：** 无需认证

**请求参数：** 无

**成功响应示例：**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "categoryId": 1,
      "categoryName": "手机数码",
      "icon": "https://cdn.seckill.com/category/phone.png",
      "sort": 1,
      "children": [
        {
          "categoryId": 11,
          "categoryName": "手机",
          "icon": "https://cdn.seckill.com/category/smartphone.png",
          "sort": 1,
          "parentId": 1,
          "children": []
        },
        {
          "categoryId": 12,
          "categoryName": "平板电脑",
          "icon": "https://cdn.seckill.com/category/tablet.png",
          "sort": 2,
          "parentId": 1,
          "children": []
        },
        {
          "categoryId": 13,
          "categoryName": "耳机音箱",
          "icon": "https://cdn.seckill.com/category/earphone.png",
          "sort": 3,
          "parentId": 1,
          "children": []
        }
      ]
    },
    {
      "categoryId": 2,
      "categoryName": "电脑办公",
      "icon": "https://cdn.seckill.com/category/computer.png",
      "sort": 2,
      "children": [
        {
          "categoryId": 21,
          "categoryName": "笔记本",
          "icon": "https://cdn.seckill.com/category/laptop.png",
          "sort": 1,
          "parentId": 2,
          "children": []
        },
        {
          "categoryId": 22,
          "categoryName": "台式机",
          "icon": "https://cdn.seckill.com/category/desktop.png",
          "sort": 2,
          "parentId": 2,
          "children": []
        }
      ]
    },
    {
      "categoryId": 3,
      "categoryName": "家用电器",
      "icon": "https://cdn.seckill.com/category/appliance.png",
      "sort": 3,
      "children": [
        {
          "categoryId": 31,
          "categoryName": "电视",
          "icon": "https://cdn.seckill.com/category/tv.png",
          "sort": 1,
          "parentId": 3,
          "children": []
        },
        {
          "categoryId": 32,
          "categoryName": "空调",
          "icon": "https://cdn.seckill.com/category/aircon.png",
          "sort": 2,
          "parentId": 3,
          "children": []
        }
      ]
    }
  ]
}
```

**业务规则：**

1. 分类支持两级树形结构。
2. 同级分类按 `sort` 字段升序排列。
3. 仅返回状态为"启用"的分类。

**备注：**

- 缓存策略：分类数据缓存在 Redis 中，Key 为 `category:tree`，TTL 1 小时。分类变更时主动清除缓存。

---

## 7. 商品接口

### 7.1 商品列表

**接口描述：** 分页查询商品列表，支持分类筛选、关键词搜索和排序。

- **请求方式：** `GET`
- **请求路径：** `/api/goods/list`
- **请求头：** 无需认证

**请求参数（Query）：**

| 字段 | 类型 | 必填 | 默认值 | 描述 |
|------|------|------|--------|------|
| categoryId | Long | 否 | - | 分类 ID，支持二级分类 |
| keyword | String | 否 | - | 搜索关键词，模糊匹配商品名称 |
| sort | String | 否 | default | 排序方式：default-默认、priceAsc-价格升序、priceDesc-价格降序、salesDesc-销量降序、createTimeDesc-最新上架 |
| current | Integer | 否 | 1 | 当前页码 |
| size | Integer | 否 | 10 | 每页大小，最大 100 |

**成功响应示例：**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "records": [
      {
        "goodsId": 1001,
        "goodsName": "Apple iPhone 16 Pro Max 256GB",
        "coverImage": "https://cdn.seckill.com/goods/1001_main.jpg",
        "categoryId": 11,
        "categoryName": "手机",
        "price": 9999.00,
        "seckillPrice": 7999.00,
        "stock": 500,
        "sales": 1280,
        "status": 1,
        "isSeckill": 1,
        "createTime": "2026-04-01 10:00:00"
      },
      {
        "goodsId": 1002,
        "goodsName": "华为 Mate 70 Pro 512GB",
        "coverImage": "https://cdn.seckill.com/goods/1002_main.jpg",
        "categoryId": 11,
        "categoryName": "手机",
        "price": 6999.00,
        "seckillPrice": null,
        "stock": 300,
        "sales": 856,
        "status": 1,
        "isSeckill": 0,
        "createTime": "2026-04-05 14:00:00"
      }
    ],
    "total": 58,
    "size": 10,
    "current": 1,
    "pages": 6
  }
}
```

**失败响应示例：**

```json
{
  "code": 40406,
  "message": "分类不存在",
  "data": null
}
```

**业务规则：**

1. 仅返回状态为"上架"的商品。
2. `keyword` 搜索范围包括商品名称和商品描述。
3. `isSeckill` 标识该商品是否正在参与秒杀活动，前端可据此展示秒杀标签。
4. `seckillPrice` 仅在商品参与秒杀活动时返回，否则为 null。

**备注：**

- 缓存策略：商品列表查询结果缓存至 Redis，Key 格式为 `goods:list:{categoryId}:{keyword}:{sort}:{current}:{size}`，TTL 5 分钟。

---

### 7.2 商品详情

**接口描述：** 获取单个商品的详细信息。

- **请求方式：** `GET`
- **请求路径：** `/api/goods/{id}`
- **请求头：** 无需认证

**路径参数：**

| 字段 | 类型 | 必填 | 描述 |
|------|------|------|------|
| id | Long | 是 | 商品 ID |

**成功响应示例：**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "goodsId": 1001,
    "goodsName": "Apple iPhone 16 Pro Max 256GB",
    "goodsImg": "https://cdn.seckill.com/goods/1001_main.jpg",
    "goodsImages": [
      "https://cdn.seckill.com/goods/1001_1.jpg",
      "https://cdn.seckill.com/goods/1001_2.jpg",
      "https://cdn.seckill.com/goods/1001_3.jpg",
      "https://cdn.seckill.com/goods/1001_4.jpg"
    ],
    "categoryId": 11,
    "categoryName": "手机",
    "description": "全新 iPhone 16 Pro Max，搭载 A18 Pro 芯片，钛金属设计，超长续航。",
    "detail": "<html><body><p>商品详情富文本内容...</p></body></html>",
    "price": 9999.00,
    "seckillPrice": 7999.00,
    "stock": 500,
    "sales": 1280,
    "status": 1,
    "isSeckill": 1,
    "seckillActivityId": 2001,
    "seckillStartTime": "2026-04-25 10:00:00",
    "seckillEndTime": "2026-04-25 12:00:00",
    "createTime": "2026-04-01 10:00:00",
    "updateTime": "2026-04-20 15:30:00"
  }
}
```

**失败响应示例：**

```json
{
  "code": 40402,
  "message": "商品不存在",
  "data": null
}
```

**业务规则：**

1. 商品不存在或已下架时返回错误。
2. 若商品参与秒杀活动，返回秒杀价格、活动 ID 及活动时间。
3. `detail` 字段为富文本 HTML 内容，前端直接渲染。

**备注：**

- 缓存策略：商品详情缓存在 Redis 中，Key 格式为 `goods:detail:{goodsId}`，TTL 30 分钟。

---

## 8. 秒杀接口

> 除"秒杀活动列表"和"活动详情"外，以下接口均需要在请求头中携带 `Authorization: Bearer <token>`。

### 8.1 秒杀活动列表

**接口描述：** 获取当前可参与和即将开始的秒杀活动列表。

- **请求方式：** `GET`
- **请求路径：** `/api/seckill/list`
- **请求头：** 无需认证

**请求参数（Query）：**

| 字段 | 类型 | 必填 | 默认值 | 描述 |
|------|------|------|--------|------|
| status | Integer | 否 | - | 活动状态筛选：0-全部（查询专用）、1-进行中、2-即将开始、3-已结束。注：此为查询筛选值，非业务状态码 |
| current | Integer | 否 | 1 | 当前页码 |
| size | Integer | 否 | 10 | 每页大小 |

**成功响应示例：**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "records": [
      {
        "activityId": 2001,
        "activityName": "iPhone 16 限时秒杀",
        "activityImg": "https://cdn.seckill.com/seckill/2001_banner.jpg",
        "startTime": "2026-04-25 10:00:00",
        "endTime": "2026-04-25 12:00:00",
        "status": 1,
        "statusDesc": "进行中",
        "goodsList": [
          {
            "goodsId": 1001,
            "goodsName": "Apple iPhone 16 Pro Max 256GB",
            "goodsImg": "https://cdn.seckill.com/goods/1001_main.jpg",
            "originalPrice": 9999.00,
            "seckillPrice": 7999.00,
            "stock": 100,
            "remainStock": 23
          }
        ]
      },
      {
        "activityId": 2002,
        "activityName": "华为手机专场",
        "activityImg": "https://cdn.seckill.com/seckill/2002_banner.jpg",
        "startTime": "2026-04-26 20:00:00",
        "endTime": "2026-04-26 22:00:00",
        "status": 2,
        "statusDesc": "即将开始",
        "goodsList": [
          {
            "goodsId": 1002,
            "goodsName": "华为 Mate 70 Pro 512GB",
            "goodsImg": "https://cdn.seckill.com/goods/1002_main.jpg",
            "originalPrice": 6999.00,
            "seckillPrice": 5499.00,
            "stock": 200,
            "remainStock": 200
          }
        ]
      }
    ],
    "total": 8,
    "size": 10,
    "current": 1,
    "pages": 1
  }
}
```

**业务规则：**

1. 活动状态自动根据当前时间计算：未开始为"即将开始"，进行中为"进行中"，已过结束时间为"已结束"。
2. `remainStock` 为实时剩余库存，从 Redis 中获取。
3. 进行中的活动排在最前，即将开始的按开始时间升序排列。

**备注：**

- 缓存策略：活动列表缓存在 Redis 中，Key 为 `seckill:activity:list`，TTL 1 分钟。库存数据实时从 Redis 获取。

---

### 8.2 秒杀活动详情

**接口描述：** 获取指定秒杀活动的详细信息，包含活动下所有秒杀商品。

- **请求方式：** `GET`
- **请求路径：** `/api/seckill/{activityId}`
- **请求头：** 无需认证

**路径参数：**

| 字段 | 类型 | 必填 | 描述 |
|------|------|------|------|
| activityId | Long | 是 | 秒杀活动 ID |

**成功响应示例：**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "activityId": 2001,
    "activityName": "iPhone 16 限时秒杀",
    "activityImg": "https://cdn.seckill.com/seckill/2001_banner.jpg",
    "description": "iPhone 16 系列限时特惠，数量有限，先到先得！",
    "startTime": "2026-04-25 10:00:00",
    "endTime": "2026-04-25 12:00:00",
    "status": 1,
    "statusDesc": "进行中",
    "rules": [
      "每人限购 1 件",
      "秒杀商品不支持使用优惠券",
      "秒杀订单需在 15 分钟内完成支付",
      "秒杀成功后不可取消订单"
    ],
    "goodsList": [
      {
        "goodsId": 1001,
        "goodsName": "Apple iPhone 16 Pro Max 256GB",
        "goodsImg": "https://cdn.seckill.com/goods/1001_main.jpg",
        "originalPrice": 9999.00,
        "seckillPrice": 7999.00,
        "totalStock": 100,
        "remainStock": 23,
        "limitPerUser": 1,
        "salesCount": 77
      }
    ]
  }
}
```

**失败响应示例：**

```json
{
  "code": 40403,
  "message": "秒杀活动不存在",
  "data": null
}
```

**业务规则：**

1. 活动不存在时返回 40403 错误码。
2. 已结束的活动仍可查看详情，但前端应提示"活动已结束"。

---

### 8.3 获取秒杀地址

**接口描述：** 获取秒杀接口的动态地址，用于防止秒杀接口被恶意刷单。需在秒杀开始前调用。

- **请求方式：** `GET`
- **请求路径：** `/api/seckill/path/{activityId}`
- **请求头：** `Authorization: Bearer <token>`

**路径参数：**

| 字段 | 类型 | 必填 | 描述 |
|------|------|------|------|
| activityId | Long | 是 | 秒杀活动 ID |

**请求参数（Query）：**

| 字段 | 类型 | 必填 | 描述 |
|------|------|------|------|
| goodsId | Long | 是 | 秒杀商品 ID |

**成功响应示例：**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "seckillPath": "/a1b2c3d4e5f6",
    "expiresAt": "2026-04-25 10:05:00"
  }
}
```

**失败响应示例：**

```json
{
  "code": 40009,
  "message": "秒杀活动未开始",
  "data": null
}
```

```json
{
  "code": 42901,
  "message": "请求过于频繁，请稍后再试",
  "data": null
}
```

**业务规则：**

1. 仅在秒杀活动开始前 5 分钟至活动期间可获取秒杀地址。
2. 秒杀地址有效期 5 分钟，过期需重新获取。
3. 同一用户对同一活动最多获取 3 次秒杀地址。
4. 秒杀地址使用 MD5(用户ID + 商品ID + 活动ID + 盐值) 生成，存入 Redis。

**备注：**

- 安全策略：秒杀地址机制防止秒杀 URL 被提前暴露和恶意请求。
- 限流规则：同一用户每秒最多请求 1 次。
- 缓存策略：秒杀地址存储在 Redis 中，Key 格式为 `seckill:path:{userId}:{activityId}:{goodsId}`，TTL 5 分钟。

---

### 8.4 执行秒杀

**接口描述：** 用户执行秒杀操作，核心接口。系统通过 Redis 预减库存 + RabbitMQ 异步下单实现高并发秒杀。

- **请求方式：** `POST`
- **请求路径：** `/api/seckill/do`
- **请求头：** `Authorization: Bearer <token>`、`Content-Type: application/json`

**请求参数（Body - JSON）：**

| 字段 | 类型 | 必填 | 约束 | 描述 |
|------|------|------|------|------|
| activityId | Long | 是 | - | 秒杀活动 ID |
| goodsId | Long | 是 | - | 秒杀商品 ID |
| seckillPath | String | 是 | - | 秒杀地址（从 8.3 接口获取） |
| addressId | Long | 是 | - | 收货地址 ID |
| quantity | Integer | 否 | 默认 1，最大为 limitPerUser | 购买数量 |

**成功响应示例：**

```json
{
  "code": 200,
  "message": "秒杀请求已提交，正在处理中",
  "data": {
    "recordId": 30001,
    "status": 0,
    "statusDesc": "排队中",
    "message": "您的秒杀请求已进入处理队列，请稍后查询结果"
  }
}
```

**失败响应示例：**

```json
{
  "code": 40008,
  "message": "商品库存不足",
  "data": null
}
```

```json
{
  "code": 40011,
  "message": "重复秒杀",
  "data": null
}
```

```json
{
  "code": 40009,
  "message": "秒杀活动未开始",
  "data": null
}
```

```json
{
  "code": 40010,
  "message": "秒杀活动已结束",
  "data": null
}
```

```json
{
  "code": 42902,
  "message": "秒杀接口限流，请稍后再试",
  "data": null
}
```

**业务规则：**

1. **秒杀地址校验：** 验证 `seckillPath` 是否有效且未过期。`seckillPath` 必须先从 8.3 接口获取，执行秒杀时后端会校验该路径是否与 Redis 中存储的一致。
2. **活动时间校验：** 仅在活动开始至结束时间内允许秒杀。
3. **重复秒杀校验：** 通过 Redis Set 判断用户是否已参与过该商品秒杀，Key 格式为 `seckill:done:{activityId}:{goodsId}`。
4. **库存预减：** 使用 Redis `DECR` 原子操作预减库存，库存不足直接返回失败。
5. **异步下单：** 库存扣减成功后，将秒杀请求发送至 RabbitMQ 队列，异步创建订单。
6. **限购校验：** 购买数量不能超过该商品的 `limitPerUser` 限制。
7. **返回排队状态：** 秒杀请求提交后立即返回，前端需轮询查询秒杀结果。

**备注：**

- 限流规则：使用 Redis + Lua 脚本实现令牌桶限流，全局 QPS 限制为 5000，单用户 QPS 限制为 1。
- 消息队列：秒杀请求发送至 RabbitMQ 的 `seckill.order.queue`，消费端异步处理订单创建。
- 库存策略：Redis 预减库存 + 数据库最终一致性保证。Redis 库存 Key 格式为 `seckill:stock:{activityId}:{goodsId}`。
- 分布式锁：使用 Redisson 分布式锁防止并发重复秒杀，Key 格式为 `seckill:lock:{userId}:{activityId}:{goodsId}`。

---

### 8.5 查询秒杀结果

**接口描述：** 查询秒杀请求的处理结果，前端在提交秒杀后轮询此接口。

- **请求方式：** `GET`
- **请求路径：** `/api/seckill/result/{recordId}`
- **请求头：** `Authorization: Bearer <token>`

**路径参数：**

| 字段 | 类型 | 必填 | 描述 |
|------|------|------|------|
| recordId | Long | 是 | 秒杀记录 ID（执行秒杀时返回） |

**成功响应示例（排队中）：**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "recordId": 30001,
    "activityId": 2001,
    "goodsId": 1001,
    "goodsName": "Apple iPhone 16 Pro Max 256GB",
    "seckillPrice": 7999.00,
    "status": 0,
    "statusDesc": "排队中",
    "createTime": "2026-04-25 10:00:01"
  }
}
```

**成功响应示例（秒杀成功）：**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "recordId": 30001,
    "activityId": 2001,
    "goodsId": 1001,
    "goodsName": "Apple iPhone 16 Pro Max 256GB",
    "seckillPrice": 7999.00,
    "status": 1,
    "statusDesc": "秒杀成功",
    "orderNo": "SEK20260425100001001",
    "createTime": "2026-04-25 10:00:01",
    "finishTime": "2026-04-25 10:00:03"
  }
}
```

**成功响应示例（秒杀失败）：**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "recordId": 30001,
    "activityId": 2001,
    "goodsId": 1001,
    "goodsName": "Apple iPhone 16 Pro Max 256GB",
    "seckillPrice": 7999.00,
    "status": 2,
    "statusDesc": "秒杀失败",
    "failReason": "库存已售罄",
    "createTime": "2026-04-25 10:00:01",
    "finishTime": "2026-04-25 10:00:05"
  }
}
```

**业务规则：**

1. 秒杀结果状态：`2`-失败、`0`-排队中、`1`-成功。
2. 秒杀成功后返回订单号，用户可直接跳转至订单详情或支付页面。
3. 排队中的请求建议前端每 2 秒轮询一次，最多轮询 30 次（60 秒）。

**备注：**

- 缓存策略：秒杀结果缓存在 Redis 中，Key 格式为 `seckill:result:{recordId}`，TTL 24 小时。

---

## 9. 订单接口

> 以下接口均需要在请求头中携带 `Authorization: Bearer <token>`。

### 9.1 订单列表

**接口描述：** 分页查询当前用户的订单列表，支持按状态筛选。

- **请求方式：** `GET`
- **请求路径：** `/api/order/list`
- **请求头：** `Authorization: Bearer <token>`

**请求参数（Query）：**

| 字段 | 类型 | 必填 | 默认值 | 描述 |
|------|------|------|--------|------|
| status | Integer | 否 | - | 订单状态：0-全部（查询专用）、1-待支付、2-已支付、3-已发货、4-已完成、5-已取消、6-已退款 |
| current | Integer | 否 | 1 | 当前页码 |
| size | Integer | 否 | 10 | 每页大小 |

**成功响应示例：**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "records": [
      {
        "orderNo": "SEK20260425100001001",
        "goodsId": 1001,
        "goodsName": "Apple iPhone 16 Pro Max 256GB",
        "goodsImg": "https://cdn.seckill.com/goods/1001_main.jpg",
        "seckillPrice": 7999.00,
        "quantity": 1,
        "totalAmount": 7999.00,
        "status": 1,
        "statusDesc": "待支付",
        "createTime": "2026-04-25 10:00:03",
        "payDeadline": "2026-04-25 10:15:03"
      },
      {
        "orderNo": "SEK20260420150002002",
        "goodsId": 1003,
        "goodsName": "小米 15 Ultra 256GB",
        "goodsImg": "https://cdn.seckill.com/goods/1003_main.jpg",
        "seckillPrice": 4999.00,
        "quantity": 1,
        "totalAmount": 4999.00,
        "status": 3,
        "statusDesc": "已发货",
        "createTime": "2026-04-20 15:00:05",
        "payDeadline": null
      }
    ],
    "total": 5,
    "size": 10,
    "current": 1,
    "pages": 1
  }
}
```

**业务规则：**

1. 仅返回当前用户自己的订单。
2. 订单按创建时间倒序排列。
3. 待支付订单显示支付截止时间，生成规则为 `createTime + 15 分钟`。
4. 超过支付截止时间未支付的订单，系统自动取消并释放库存。

---

### 9.2 订单详情

**接口描述：** 查询指定订单的详细信息。

- **请求方式：** `GET`
- **请求路径：** `/api/order/{orderNo}`
- **请求头：** `Authorization: Bearer <token>`

**路径参数：**

| 字段 | 类型 | 必填 | 描述 |
|------|------|------|------|
| orderNo | String | 是 | 订单编号 |

**成功响应示例：**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "orderNo": "SEK20260425100001001",
    "userId": 10001,
    "activityId": 2001,
    "goodsId": 1001,
    "goodsName": "Apple iPhone 16 Pro Max 256GB",
    "goodsImg": "https://cdn.seckill.com/goods/1001_main.jpg",
    "seckillPrice": 7999.00,
    "quantity": 1,
    "totalAmount": 7999.00,
    "payAmount": 7999.00,
    "status": 1,
    "statusDesc": "待支付",
    "payType": null,
    "payTime": null,
    "addressSnapshot": {
      "receiverName": "张三",
      "receiverPhone": "13812341234",
      "province": "浙江省",
      "city": "杭州市",
      "district": "西湖区",
      "detailAddress": "文三路 100 号 XX 小区 1 栋 502 室"
    },
    "expressCompany": null,
    "expressNo": null,
    "createTime": "2026-04-25 10:00:03",
    "payDeadline": "2026-04-25 10:15:03",
    "updateTime": "2026-04-25 10:00:03"
  }
}
```

**失败响应示例：**

```json
{
  "code": 40404,
  "message": "订单不存在",
  "data": null
}
```

```json
{
  "code": 40301,
  "message": "无权限访问该资源",
  "data": null
}
```

**业务规则：**

1. 仅允许查看自己的订单详情。
2. `addressSnapshot` 为下单时收货地址的快照，不随地址修改而变化。
3. 订单状态流转：待支付 -> 已支付 -> 已发货 -> 已完成，或 待支付 -> 已取消，或 待支付 -> 已取消（取消后释放库存）。

---

### 9.3 取消订单

**接口描述：** 取消指定订单，仅"待支付"状态的订单可取消。

- **请求方式：** `PUT`
- **请求路径：** `/api/order/cancel/{orderNo}`
- **请求头：** `Authorization: Bearer <token>`

**路径参数：**

| 字段 | 类型 | 必填 | 描述 |
|------|------|------|------|
| orderNo | String | 是 | 订单编号 |

**请求参数（Body - JSON）：**

| 字段 | 类型 | 必填 | 描述 |
|------|------|------|------|
| cancelReason | String | 否 | 取消原因 |

**成功响应示例：**

```json
{
  "code": 200,
  "message": "订单取消成功",
  "data": null
}
```

**失败响应示例：**

```json
{
  "code": 40012,
  "message": "订单状态异常",
  "data": null
}
```

```json
{
  "code": 40404,
  "message": "订单不存在",
  "data": null
}
```

**业务规则：**

1. 仅"待支付"状态的订单可取消，其他状态返回 40012 错误。
2. 秒杀订单取消后释放库存（Redis INCR + 数据库乐观锁回滚），并删除用户已秒杀标记。
3. 取消操作写入订单日志表。

---

## 10. 支付接口

> 以下接口中，"发起支付"和"支付状态查询"需要在请求头中携带 `Authorization: Bearer <token>`。"支付回调"由第三方支付平台调用，无需用户 Token。

### 10.1 发起支付

**接口描述：** 对指定订单发起支付请求，返回支付参数供前端调起支付。

- **请求方式：** `POST`
- **请求路径：** `/api/pay/create`
- **请求头：** `Authorization: Bearer <token>`、`Content-Type: application/json`

**请求参数（Body - JSON）：**

| 字段 | 类型 | 必填 | 约束 | 描述 |
|------|------|------|------|------|
| orderNo | String | 是 | - | 订单编号 |
| payType | Integer | 是 | 1-余额 2-模拟支付宝 3-模拟微信 | 支付方式 |

**成功响应示例：**

```json
{
  "code": 200,
  "message": "支付创建成功",
  "data": {
    "payNo": "PAY20260425100003001",
    "orderNo": "SEK20260425100001001",
    "payAmount": 7999.00,
    "payType": 2,
    "payTypeDesc": "模拟支付宝",
    "payParams": {
      "alipayOrderStr": "alipay_sdk=alipay-sdk-java&app_id=2021001..."
    },
    "expireTime": "2026-04-25 10:30:03"
  }
}
```

**失败响应示例：**

```json
{
  "code": 40012,
  "message": "订单状态异常",
  "data": null
}
```

```json
{
  "code": 40404,
  "message": "订单不存在",
  "data": null
}
```

**业务规则：**

1. 仅"待支付"状态的订单可发起支付。
2. 支付参数根据支付方式不同而不同，前端根据 `payType` 调起对应的支付 SDK。
3. 支付记录写入数据库，状态为"待支付"。
4. 支付超时时间与订单支付截止时间一致。

**备注：**

- 幂等性：同一订单重复发起支付返回已有支付记录。
- 安全策略：支付金额需与订单金额二次校验，防止篡改。

---

### 10.2 支付回调

**接口描述：** 第三方支付平台支付成功后的异步回调通知接口。此接口由支付平台服务器调用，非前端调用。

- **请求方式：** `POST`
- **请求路径：** `/api/pay/callback`
- **请求头：** `Content-Type: application/x-www-form-urlencoded`（支付宝）/ `application/json`（微信支付）

**请求参数：**

由第三方支付平台推送，包含签名、订单号、支付金额、支付状态等信息。后端需验签后处理。

**支付宝回调参数示例：**

| 字段 | 类型 | 描述 |
|------|------|------|
| out_trade_no | String | 商户订单号 |
| trade_no | String | 支付宝交易号 |
| trade_status | String | 交易状态：TRADE_SUCCESS / TRADE_FINISHED |
| total_amount | String | 订单金额 |
| sign | String | 签名 |
| sign_type | String | 签名算法 |

**成功响应：** 返回 `success` 字符串（支付宝要求）

**失败响应：** 返回非 `success` 字符串，支付平台会重试通知。

**业务规则：**

1. 验证回调签名，防止伪造通知。
2. 验证订单金额与支付金额是否一致。
3. 通过分布式锁保证回调幂等性，防止重复处理。
4. 更新支付记录状态为"已支付"。
5. 更新订单状态为"已支付"。
6. 发送订单支付成功消息至 RabbitMQ，触发后续流程（如发送通知、扣减库存等）。
7. 回调处理失败时，支付平台会按策略重试。

**备注：**

- 幂等性：使用 Redis 分布式锁保证回调只处理一次，Key 格式为 `pay:callback:lock:{orderNo}`。
- 重试机制：支付宝默认重试 8 次，间隔递增。

---

### 10.3 支付状态查询

**接口描述：** 主动查询指定订单的支付状态。

- **请求方式：** `GET`
- **请求路径：** `/api/pay/status/{orderNo}`
- **请求头：** `Authorization: Bearer <token>`

**路径参数：**

| 字段 | 类型 | 必填 | 描述 |
|------|------|------|------|
| orderNo | String | 是 | 订单编号 |

**成功响应示例：**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "orderNo": "SEK20260425100001001",
    "payNo": "PAY20260425100003001",
    "payAmount": 7999.00,
    "payType": 2,
    "payTypeDesc": "模拟支付宝",
    "tradeNo": "2026042522001400001234567890",
    "status": 1,
    "statusDesc": "已支付",
    "payTime": "2026-04-25 10:05:30"
  }
}
```

**失败响应示例：**

```json
{
  "code": 40404,
  "message": "订单不存在",
  "data": null
}
```

**业务规则：**

1. 支付状态：`0`-待支付、`1`-已支付、`2`-支付失败、`3`-已退款。
2. 若本地状态为"待支付"，则主动向第三方支付平台查询最新状态并更新。

**备注：**

- 缓存策略：支付状态缓存在 Redis 中，Key 格式为 `pay:status:{orderNo}`，TTL 5 分钟。

---

## 11. 管理后台接口

> 管理后台接口使用独立的认证体系，管理员 Token 与用户 Token 分离。管理员接口均需要在请求头中携带 `Authorization: Bearer <admin_token>`。

### 11.1 管理员登录

**接口描述：** 管理员使用账号密码登录后台管理系统。

- **请求方式：** `POST`
- **请求路径：** `/api/admin/login`
- **请求头：** `Content-Type: application/json`

**请求参数（Body - JSON）：**

| 字段 | 类型 | 必填 | 约束 | 描述 |
|------|------|------|------|------|
| username | String | 是 | - | 管理员账号 |
| password | String | 是 | - | 密码 |

**成功响应示例：**

```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "adminId": 1,
    "username": "admin",
    "realName": "系统管理员",
    "role": "SUPER_ADMIN",
    "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiIsInJvbGUiOiJTVVBFUl9BRE1JTiIsImlhdCI6MTcxMzg0MDAwMCwiZXhwIjoxNzEzODQxODAwfQ.xxx",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiIsImlhdCI6MTcxMzg0MDAwMCwiZXhwIjoxNzE0NDQ0ODAwfQ.yyy",
    "expiresIn": 1800
  }
}
```

**失败响应示例：**

```json
{
  "code": 400,
  "message": "管理员账号或密码错误",
  "data": null
}
```

#### 11.1.2 图片上传

**接口描述：** 管理端上传图片（商品图片、活动封面图等），返回图片 URL。

- **请求方式：** `POST`
- **请求路径：** `/api/admin/upload/image`
- **请求头：** `Authorization: Bearer <admin_token>`、`Content-Type: multipart/form-data`

**请求参数（Body - Form Data）：**

| 字段 | 类型 | 必填 | 约束 | 描述 |
|------|------|------|------|------|
| image | File | 是 | 支持 jpg/jpeg/png/webp，最大 5MB | 图片文件 |

**成功响应示例：**

```json
{
  "code": 200,
  "message": "图片上传成功",
  "data": {
    "imageUrl": "https://cdn.seckill.com/upload/20260424/abc123.jpg"
  }
}
```

**失败响应示例：**

```json
{
  "code": 40015,
  "message": "文件上传格式不支持",
  "data": null
}
```

```json
{
  "code": 40016,
  "message": "文件大小超出限制",
  "data": null
}
```

**业务规则：**

1. 上传文件格式校验：仅支持 jpg、jpeg、png、webp。
2. 文件大小限制：最大 5MB。
3. 文件命名规则：随机字符串 + 时间戳 + 原扩展名，避免文件名冲突。
4. 上传成功后返回可访问的完整 URL。

**备注：**

- 存储策略：文件存储至 OSS（对象存储服务），本地开发环境存储至 `uploads/images/` 目录。

---

### 11.2 商品管理

**业务规则：**

1. 管理员角色分为：`SUPER_ADMIN`（超级管理员）、`ADMIN`（普通管理员）、`OPERATOR`（运营人员）。
2. 密码错误连续 5 次，账号锁定 30 分钟。
3. 管理员 Token 中包含 `role` 字段，用于接口权限控制。

**备注：**

- 限流规则：同一 IP 每分钟最多登录 5 次。
- 安全策略：管理员登录日志记录至数据库，包含 IP、时间、结果。

---

### 11.2 商品管理

#### 11.2.1 商品列表（管理端）

**接口描述：** 管理端分页查询商品列表，支持全部状态的商品。

- **请求方式：** `GET`
- **请求路径：** `/api/admin/goods`
- **请求头：** `Authorization: Bearer <admin_token>`

**请求参数（Query）：**

| 字段 | 类型 | 必填 | 默认值 | 描述 |
|------|------|------|--------|------|
| categoryId | Long | 否 | - | 分类 ID |
| keyword | String | 否 | - | 搜索关键词 |
| status | Integer | 否 | - | 商品状态：0-下架、1-上架 |
| current | Integer | 否 | 1 | 当前页码 |
| size | Integer | 否 | 10 | 每页大小 |

**成功响应示例：**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "records": [
      {
        "goodsId": 1001,
        "goodsName": "Apple iPhone 16 Pro Max 256GB",
        "goodsImg": "https://cdn.seckill.com/goods/1001_main.jpg",
        "categoryId": 11,
        "categoryName": "手机",
        "price": 9999.00,
        "stock": 500,
        "sales": 1280,
        "status": 1,
        "statusDesc": "上架",
        "createTime": "2026-04-01 10:00:00",
        "updateTime": "2026-04-20 15:30:00"
      }
    ],
    "total": 120,
    "size": 10,
    "current": 1,
    "pages": 12
  }
}
```

---

#### 11.2.2 新增商品

**接口描述：** 管理端新增商品。

- **请求方式：** `POST`
- **请求路径：** `/api/admin/goods`
- **请求头：** `Authorization: Bearer <admin_token>`、`Content-Type: application/json`

**请求参数（Body - JSON）：**

| 字段 | 类型 | 必填 | 约束 | 描述 |
|------|------|------|------|------|
| goodsName | String | 是 | 2-100位 | 商品名称 |
| categoryId | Long | 是 | - | 分类 ID |
| description | String | 否 | 最大 500 位 | 商品简介 |
| detail | String | 否 | - | 商品详情（富文本 HTML） |
| price | BigDecimal | 是 | 大于 0 | 商品原价 |
| stock | Integer | 是 | 大于等于 0 | 库存数量 |
| goodsImages | Array | 否 | 最多 9 张 | 商品图片 URL 列表 |
| status | Integer | 否 | 默认 0（下架） | 商品状态 |

**成功响应示例：**

```json
{
  "code": 201,
  "message": "商品创建成功",
  "data": {
    "goodsId": 1010
  }
}
```

**失败响应示例：**

```json
{
  "code": 40406,
  "message": "分类不存在",
  "data": null
}
```

**业务规则：**

1. 分类 ID 必须存在且为二级分类。
2. 商品图片 URL 需先通过文件上传接口获取。
3. 新增商品默认为"下架"状态，需手动上架。

---

#### 11.2.3 修改商品

**接口描述：** 管理端修改商品信息。

- **请求方式：** `PUT`
- **请求路径：** `/api/admin/goods/{id}`
- **请求头：** `Authorization: Bearer <admin_token>`、`Content-Type: application/json`

**路径参数：**

| 字段 | 类型 | 必填 | 描述 |
|------|------|------|------|
| id | Long | 是 | 商品 ID |

**请求参数（Body - JSON）：**
| goodsName | String | 否 | 2-100位 | 商品名称 |
| categoryId | Long | 否 | - | 分类 ID |
| description | String | 否 | 最大 500 位 | 商品简介 |
| detail | String | 否 | - | 商品详情（富文本 HTML） |
| price | BigDecimal | 否 | 大于 0 | 商品原价 |
| stock | Integer | 否 | 大于等于 0 | 库存数量 |
| goodsImages | Array | 否 | 最多 9 张 | 商品图片 URL 列表 |
| status | Integer | 否 | 0-下架 1-上架 | 商品状态 |

**成功响应示例：**

```json
{
  "code": 200,
  "message": "商品修改成功",
  "data": null
}
```

**失败响应示例：**

```json
{
  "code": 40402,
  "message": "商品不存在",
  "data": null
}
```

**业务规则：**

1. 修改商品信息后清除相关 Redis 缓存。
2. 正在参与秒杀活动的商品不允许下架。

---

#### 11.2.4 删除商品

**接口描述：** 管理端删除商品（逻辑删除）。

- **请求方式：** `DELETE`
- **请求路径：** `/api/admin/goods/{id}`
- **请求头：** `Authorization: Bearer <admin_token>`

**路径参数：**

| 字段 | 类型 | 必填 | 描述 |
|------|------|------|------|
| id | Long | 是 | 商品 ID |

**成功响应示例：**

```json
{
  "code": 200,
  "message": "商品删除成功",
  "data": null
}
```

**失败响应示例：**

```json
{
  "code": 400,
  "message": "该商品正在参与秒杀活动，无法删除",
  "data": null
}
```

**业务规则：**

1. 采用逻辑删除，不物理删除数据。
2. 正在参与秒杀活动或存在未完成订单的商品不允许删除。

---

### 11.3 秒杀活动管理

#### 11.3.1 秒杀活动列表（管理端）

**接口描述：** 管理端分页查询秒杀活动列表。

- **请求方式：** `GET`
- **请求路径：** `/api/admin/seckill/activity`
- **请求头：** `Authorization: Bearer <admin_token>`

**请求参数（Query）：**

| 字段 | 类型 | 必填 | 默认值 | 描述 |
|------|------|------|--------|------|
| status | Integer | 否 | - | 活动状态：0-全部、1-未开始、2-进行中、3-已结束 |
| current | Integer | 否 | 1 | 当前页码 |
| size | Integer | 否 | 10 | 每页大小 |

**成功响应示例：**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "records": [
      {
        "activityId": 2001,
        "activityName": "iPhone 16 限时秒杀",
        "startTime": "2026-04-25 10:00:00",
        "endTime": "2026-04-25 12:00:00",
        "status": 2,
        "statusDesc": "进行中",
        "goodsCount": 1,
        "totalStock": 100,
        "totalSales": 77,
        "createTime": "2026-04-20 09:00:00"
      }
    ],
    "total": 15,
    "size": 10,
    "current": 1,
    "pages": 2
  }
}
```

---

#### 11.3.2 新增秒杀活动

**接口描述：** 管理端创建新的秒杀活动。

- **请求方式：** `POST`
- **请求路径：** `/api/admin/seckill/activity`
- **请求头：** `Authorization: Bearer <admin_token>`、`Content-Type: application/json`

**请求参数（Body - JSON）：**

| 字段 | 类型 | 必填 | 约束 | 描述 |
|------|------|------|------|------|
| activityName | String | 是 | 2-50位 | 活动名称 |
| description | String | 否 | 最大 500 位 | 活动描述 |
| activityImg | String | 否 | - | 活动封面图 URL |
| startTime | String | 是 | yyyy-MM-dd HH:mm:ss | 活动开始时间 |
| endTime | String | 是 | yyyy-MM-dd HH:mm:ss | 活动结束时间 |
| rules | Array | 否 | - | 活动规则列表 |
| goodsList | Array | 是 | 至少 1 个 | 秒杀商品列表 |

**goodsList 子对象字段：**

| 字段 | 类型 | 必填 | 约束 | 描述 |
|------|------|------|------|------|
| goodsId | Long | 是 | - | 商品 ID |
| seckillPrice | BigDecimal | 是 | 大于 0，小于商品原价 | 秒杀价格 |
| seckillStock | Integer | 是 | 大于 0 | 秒杀库存数量 |
| limitPerUser | Integer | 否 | 默认 1，最大 10 | 每人限购数量 |

**成功响应示例：**

```json
{
  "code": 201,
  "message": "秒杀活动创建成功",
  "data": {
    "activityId": 2010
  }
}
```

**失败响应示例：**

```json
{
  "code": 400,
  "message": "活动结束时间必须大于开始时间",
  "data": null
}
```

```json
{
  "code": 400,
  "message": "秒杀价格必须小于商品原价",
  "data": null
}
```

**业务规则：**

1. 活动结束时间必须大于开始时间。
2. 秒杀价格必须小于商品原价。
3. 秒杀库存不能超过商品总库存。
4. 活动创建后，库存预加载至 Redis（Key 格式：`seckill:stock:{activityId}:{goodsId}`）。
5. 活动开始前 5 分钟，系统自动预热活动数据至 Redis。

---

#### 11.3.3 修改秒杀活动

**接口描述：** 管理端修改秒杀活动信息。

- **请求方式：** `PUT`
- **请求路径：** `/api/admin/seckill/activity`
- **请求头：** `Authorization: Bearer <admin_token>`、`Content-Type: application/json`

**请求参数（Body - JSON）：**

| 字段 | 类型 | 必填 | 约束 | 描述 |
|------|------|------|------|------|
| activityId | Long | 是 | - | 活动 ID |
| activityName | String | 否 | 2-50位 | 活动名称 |
| description | String | 否 | 最大 500 位 | 活动描述 |
| activityImg | String | 否 | - | 活动封面图 URL |
| startTime | String | 否 | yyyy-MM-dd HH:mm:ss | 活动开始时间 |
| endTime | String | 否 | yyyy-MM-dd HH:mm:ss | 活动结束时间 |
| rules | Array | 否 | - | 活动规则列表 |
| goodsList | Array | 否 | - | 秒杀商品列表（全量更新） |

**成功响应示例：**

```json
{
  "code": 200,
  "message": "秒杀活动修改成功",
  "data": null
}
```

**失败响应示例：**

```json
{
  "code": 40403,
  "message": "秒杀活动不存在",
  "data": null
}
```

```json
{
  "code": 400,
  "message": "进行中的活动不允许修改",
  "data": null
}
```

**业务规则：**

1. 进行中的活动不允许修改基本信息和商品配置。
2. 已结束的活动仅允许修改活动名称和描述。
3. 修改活动时间后，需重新计算 Redis 中的库存预热时间。

---

### 11.4 活动统计

**接口描述：** 获取指定秒杀活动的统计数据，包括销售情况、用户参与情况等。

- **请求方式：** `GET`
- **请求路径：** `/api/admin/seckill/statistics/{activityId}`
- **请求头：** `Authorization: Bearer <admin_token>`

**路径参数：**

| 字段 | 类型 | 必填 | 描述 |
|------|------|------|------|
| activityId | Long | 是 | 秒杀活动 ID |

**成功响应示例：**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "activityId": 2001,
    "activityName": "iPhone 16 限时秒杀",
    "startTime": "2026-04-25 10:00:00",
    "endTime": "2026-04-25 12:00:00",
    "totalStock": 100,
    "totalSales": 77,
    "salesRate": 77.0,
    "totalAmount": 615923.00,
    "totalUsers": 77,
    "totalRequests": 15280,
    "successRate": 0.50,
    "averageResponseTime": 156,
    "goodsStatistics": [
      {
        "goodsId": 1001,
        "goodsName": "Apple iPhone 16 Pro Max 256GB",
        "seckillPrice": 7999.00,
        "totalStock": 100,
        "totalSales": 77,
        "salesRate": 77.0,
        "totalAmount": 615923.00
      }
    ],
    "timeDistribution": [
      { "time": "10:00", "sales": 45 },
      { "time": "10:01", "sales": 20 },
      { "time": "10:02", "sales": 8 },
      { "time": "10:03", "sales": 3 },
      { "time": "10:05", "sales": 1 }
    ]
  }
}
```

**失败响应示例：**

```json
{
  "code": 40403,
  "message": "秒杀活动不存在",
  "data": null
}
```

**业务规则：**

1. 统计数据从数据库聚合计算，包含实时数据。
2. `timeDistribution` 为按分钟维度的销售分布。
3. `successRate` = 成功下单数 / 总请求数。

---

### 11.5 用户管理

#### 11.5.1 用户列表

**接口描述：** 管理端分页查询用户列表。

- **请求方式：** `GET`
- **请求路径：** `/api/admin/user/list`
- **请求头：** `Authorization: Bearer <admin_token>`

**请求参数（Query）：**

| 字段 | 类型 | 必填 | 默认值 | 描述 |
|------|------|------|--------|------|
| keyword | String | 否 | - | 搜索关键词（用户名/昵称/手机号） |
| status | Integer | 否 | - | 用户状态：0-禁用、1-正常 |
| current | Integer | 否 | 1 | 当前页码 |
| size | Integer | 否 | 10 | 每页大小 |

**成功响应示例：**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "records": [
      {
        "userId": 10001,
        "username": "zhangsan",
        "nickname": "张三",
        "phone": "138****1234",
        "email": "zhangsan@example.com",
        "status": 1,
        "statusDesc": "正常",
        "orderCount": 5,
        "totalAmount": 25996.00,
        "lastLoginTime": "2026-04-23 09:15:00",
        "createTime": "2026-01-10 10:30:00"
      },
      {
        "userId": 10002,
        "username": "lisi",
        "nickname": "李四",
        "phone": "139****5678",
        "email": "lisi@example.com",
        "status": 0,
        "statusDesc": "禁用",
        "orderCount": 2,
        "totalAmount": 9998.00,
        "lastLoginTime": "2026-04-20 14:00:00",
        "createTime": "2026-02-15 08:20:00"
      }
    ],
    "total": 3580,
    "size": 10,
    "current": 1,
    "pages": 358
  }
}
```

**业务规则：**

1. 手机号中间四位脱敏显示。
2. 按用户 ID 升序排列。

---

#### 11.5.2 禁用/启用用户

**接口描述：** 管理端禁用或启用指定用户账号。

- **请求方式：** `PUT`
- **请求路径：** `/api/admin/user/status/{userId}`
- **请求头：** `Authorization: Bearer <admin_token>`、`Content-Type: application/json`

**路径参数：**

| 字段 | 类型 | 必填 | 描述 |
|------|------|------|------|
| userId | Long | 是 | 用户 ID |

**请求参数（Body - JSON）：**

| 字段 | 类型 | 必填 | 约束 | 描述 |
|------|------|------|------|------|
| status | Integer | 是 | 0-禁用、1-启用 | 目标状态 |

**成功响应示例：**

```json
{
  "code": 200,
  "message": "用户状态更新成功",
  "data": null
}
```

**失败响应示例：**

```json
{
  "code": 40401,
  "message": "用户不存在",
  "data": null
}
```

**业务规则：**

1. 禁用用户后，该用户所有 Token 立即失效。
2. 禁用用户后，清除该用户的 Redis 缓存。
3. 不允许禁用自己（管理员自身）。
4. 操作记录写入管理员操作日志。

---

### 11.6 仪表盘数据

**接口描述：** 获取管理后台仪表盘的汇总统计数据。

- **请求方式：** `GET`
- **请求路径：** `/api/admin/dashboard`
- **请求头：** `Authorization: Bearer <admin_token>`

**请求参数：** 无

**成功响应示例：**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "overview": {
      "totalUsers": 3580,
      "todayNewUsers": 28,
      "totalOrders": 12560,
      "todayOrders": 156,
      "totalSales": 8965432.00,
      "todaySales": 128960.00,
      "totalGoods": 120,
      "onlineGoods": 98,
      "totalSeckillActivities": 15,
      "ongoingActivities": 2
    },
    "salesTrend": [
      { "date": "2026-04-17", "amount": 156800.00, "orderCount": 120 },
      { "date": "2026-04-18", "amount": 189200.00, "orderCount": 145 },
      { "date": "2026-04-19", "amount": 134500.00, "orderCount": 98 },
      { "date": "2026-04-20", "amount": 245600.00, "orderCount": 198 },
      { "date": "2026-04-21", "amount": 178900.00, "orderCount": 132 },
      { "date": "2026-04-22", "amount": 210300.00, "orderCount": 167 },
      { "date": "2026-04-23", "amount": 128960.00, "orderCount": 156 }
    ],
    "topSellingGoods": [
      {
        "goodsId": 1001,
        "goodsName": "Apple iPhone 16 Pro Max 256GB",
        "sales": 1280,
        "amount": 10238720.00
      },
      {
        "goodsId": 1002,
        "goodsName": "华为 Mate 70 Pro 512GB",
        "sales": 856,
        "amount": 5990544.00
      },
      {
        "goodsId": 1003,
        "goodsName": "小米 15 Ultra 256GB",
        "sales": 620,
        "amount": 3099380.00
      }
    ],
    "recentOrders": [
      {
        "orderNo": "SEK20260425100001001",
        "username": "zhangsan",
        "goodsName": "Apple iPhone 16 Pro Max 256GB",
        "totalAmount": 7999.00,
        "status": 1,
        "statusDesc": "待支付",
        "createTime": "2026-04-25 10:00:03"
      },
      {
        "orderNo": "SEK20260424200002005",
        "username": "wangwu",
        "goodsName": "华为 Mate 70 Pro 512GB",
        "totalAmount": 5499.00,
        "status": 2,
        "statusDesc": "已支付",
        "createTime": "2026-04-24 20:00:08"
      }
    ]
  }
}
```

**业务规则：**

1. `salesTrend` 返回最近 7 天的销售趋势数据。
2. `topSellingGoods` 返回销量 Top 10 商品。
3. `recentOrders` 返回最近 10 笔订单。

**备注：**

- 缓存策略：仪表盘数据缓存在 Redis 中，Key 为 `admin:dashboard`，TTL 5 分钟。

---

### 11.7 订单管理列表

**接口描述：** 管理端分页查询所有用户的订单列表，支持多条件筛选。

- **请求方式：** `GET`
- **请求路径：** `/api/admin/order/list`
- **请求头：** `Authorization: Bearer <admin_token>`

**请求参数（Query）：**

| 字段 | 类型 | 必填 | 默认值 | 描述 |
|------|------|------|--------|------|
| orderNo | String | 否 | - | 订单编号（精确匹配） |
| userId | Long | 否 | - | 用户 ID |
| status | Integer | 否 | - | 订单状态：0-全部、1-待支付、2-已支付、3-已发货、4-已完成、5-已取消、6-已退款 |
| startTime | String | 否 | yyyy-MM-dd HH:mm:ss | 下单起始时间 |
| endTime | String | 否 | yyyy-MM-dd HH:mm:ss | 下单结束时间 |
| current | Integer | 否 | 1 | 当前页码 |
| size | Integer | 否 | 10 | 每页大小 |

**成功响应示例：**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "records": [
      {
        "orderNo": "SEK20260425100001001",
        "userId": 10001,
        "username": "zhangsan",
        "goodsId": 1001,
        "goodsName": "Apple iPhone 16 Pro Max 256GB",
        "goodsImg": "https://cdn.seckill.com/goods/1001_main.jpg",
        "totalAmount": 7999.00,
        "status": 1,
        "statusDesc": "待支付",
        "createTime": "2026-04-25 10:00:03",
        "payTime": null
      },
      {
        "orderNo": "SEK20260424200002005",
        "userId": 10005,
        "username": "wangwu",
        "goodsId": 1002,
        "goodsName": "华为 Mate 70 Pro 512GB",
        "goodsImg": "https://cdn.seckill.com/goods/1002_main.jpg",
        "totalAmount": 5499.00,
        "status": 2,
        "statusDesc": "已支付",
        "createTime": "2026-04-24 20:00:08",
        "payTime": "2026-04-24 20:05:30"
      }
    ],
    "total": 12560,
    "size": 10,
    "current": 1,
    "pages": 1256
  }
}
```

**业务规则：**

1. 管理端可查看所有用户的订单。
2. 支持按订单编号、用户 ID、状态、时间范围组合筛选。
3. 按订单创建时间倒序排列。

---

### 11.8 订单发货

**接口描述：** 管理端对已支付的订单进行发货操作。

- **请求方式：** `PUT`
- **请求路径：** `/api/admin/order/{orderNo}/ship`
- **请求头：** `Authorization: Bearer <admin_token>`、`Content-Type: application/json`

**路径参数：**

| 字段 | 类型 | 必填 | 描述 |
|------|------|------|------|
| orderNo | String | 是 | 订单编号 |

**请求参数（Body - JSON）：**

| 字段 | 类型 | 必填 | 约束 | 描述 |
|------|------|------|------|------|
| expressCompany | String | 是 | 2-50位 | 快递公司名称 |
| expressNo | String | 是 | 5-30位 | 快递单号 |

**成功响应示例：**

```json
{
  "code": 200,
  "message": "发货成功",
  "data": null
}
```

**失败响应示例：**

```json
{
  "code": 40012,
  "message": "订单状态异常",
  "data": null
}
```

**业务规则：**

1. 仅"已支付"状态的订单可执行发货操作。
2. 发货后订单状态更新为"已发货"（3）。
3. 发货信息写入订单记录，用户可在订单详情中查看物流信息。
4. 操作记录写入管理员操作日志。

---

## 附录

### A. 数据库表结构概览

| 表名 | 说明 |
|------|------|
| `t_user` | 用户表 |
| `t_address` | 收货地址表 |
| `t_category` | 商品分类表 |
| `t_goods` | 商品表 |
| `t_seckill_activity` | 秒杀活动表 |
| `t_seckill_goods` | 秒杀商品关联表 |
| `t_seckill_record` | 秒杀记录表 |
| `t_order` | 订单表 |
| `t_pay_record` | 支付记录表 |
| `t_admin` | 管理员表 |
| `t_admin_operation_log` | 管理员操作日志表 |

### B. Redis Key 设计规范

| Key 格式 | 说明 | TTL |
|----------|------|-----|
| `user:info:{userId}` | 用户信息缓存 | 30 分钟 |
| `token:blacklist:{token}` | Token 黑名单 | 与 Token 剩余有效期一致 |
| `login:fail:{account}` | 登录失败次数 | 30 分钟 |
| `category:tree` | 分类树缓存 | 1 小时 |
| `goods:list:{params}` | 商品列表缓存 | 5 分钟 |
| `goods:detail:{goodsId}` | 商品详情缓存 | 30 分钟 |
| `seckill:activity:list` | 秒杀活动列表 | 1 分钟 |
| `seckill:stock:{activityId}:{goodsId}` | 秒杀库存 | 活动结束后 24 小时 |
| `seckill:done:{activityId}:{goodsId}` | 已秒杀用户集合 | 活动结束后 24 小时 |
| `seckill:path:{userId}:{activityId}:{goodsId}` | 秒杀地址 | 5 分钟 |
| `seckill:result:{recordId}` | 秒杀结果 | 24 小时 |
| `seckill:lock:{userId}:{activityId}:{goodsId}` | 秒杀分布式锁 | 10 秒 |
| `pay:status:{orderNo}` | 支付状态缓存 | 5 分钟 |
| `pay:callback:lock:{orderNo}` | 支付回调锁 | 30 秒 |
| `admin:dashboard` | 仪表盘数据 | 5 分钟 |

### C. RabbitMQ 队列设计

| 队列名称 | 说明 | 消费者处理逻辑 |
|----------|------|----------------|
| `seckill.order.queue` | 秒杀下单队列 | 异步创建订单、扣减数据库库存 |
| `order.pay.success.queue` | 支付成功队列 | 更新订单状态、发送支付成功通知 |
| `order.timeout.queue` | 订单超时队列（延迟队列） | 检查并取消超时未支付订单，释放库存 |
| `mail.send.queue` | 邮件发送队列 | 异步发送注册欢迎邮件、密码变更通知等 |

### D. 接口限流策略汇总

| 接口 | 限流规则 |
|------|----------|
| 用户注册 | 同一 IP 每分钟最多 5 次 |
| 用户登录 | 同一 IP 每分钟最多 10 次，同一账号每分钟最多 5 次 |
| 管理员登录 | 同一 IP 每分钟最多 5 次 |
| 获取秒杀地址 | 同一用户每秒最多 1 次 |
| 执行秒杀 | 全局 QPS 5000，单用户 QPS 1 |
| 查询秒杀结果 | 同一用户每秒最多 3 次 |
| 支付回调 | 无限流（需验签） |
| 其他接口 | 同一 IP 每分钟最多 60 次 |

---

> **文档维护说明：** 本文档由后端开发团队维护，接口变更时需同步更新。如有疑问请联系后端负责人。

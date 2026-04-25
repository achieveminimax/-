# seckill-common 模块

## 模块概述

`seckill-common` 是电商秒杀系统的**公共模块**，为所有业务模块提供通用的工具类、常量定义、统一响应格式和异常处理机制。该模块不依赖任何业务模块，是系统中最基础的模块。

### 核心职责

- 提供统一的 API 响应格式
- 定义全局常量和枚举
- 封装通用工具类
- 定义业务异常体系
- 提供上下文管理工具

---

## 包结构说明

```
seckill-common/
├── constant/           # 常量定义
│   └── RedisKeyConstant.java    # Redis Key 常量
├── dto/               # 数据传输对象
│   └── PageRequest.java         # 分页请求
├── entity/            # 基础实体
│   └── BaseEntity.java          # 基础实体类
├── enums/             # 枚举定义
│   ├── OrderStatusEnum.java     # 订单状态枚举
│   ├── PayStatusEnum.java       # 支付状态枚举
│   ├── PayTypeEnum.java         # 支付类型枚举
│   ├── ResponseCodeEnum.java    # 响应码枚举
│   ├── SeckillRecordStatusEnum.java  # 秒杀记录状态枚举
│   └── SeckillStatusEnum.java   # 秒杀状态枚举
├── exception/         # 异常定义
│   └── BusinessException.java   # 业务异常
├── result/            # 统一响应
│   ├── PageResult.java          # 分页结果
│   └── Result.java              # 统一响应体
└── utils/             # 工具类
    ├── AdminContext.java        # 管理员上下文
    ├── JsonUtils.java           # JSON 工具
    ├── JwtUtils.java            # JWT 工具
    ├── SnowflakeIdWorker.java   # 雪花算法 ID 生成器
    └── UserContext.java         # 用户上下文
```

---

## 核心组件详解

### 1. 统一响应 (Result)

`Result<T>` 是系统统一的 API 响应格式，所有接口都返回此类型。

```java
@Data
public class Result<T> implements Serializable {
    private Integer code;       // 状态码
    private String message;     // 响应消息
    private T data;            // 响应数据
    private Long timestamp;    // 时间戳
}
```

#### 使用示例

```java
// 成功响应（无数据）
return Result.success();

// 成功响应（带数据）
return Result.success(userInfo);

// 成功响应（自定义消息）
return Result.success("登录成功", token);

// 创建成功响应
return Result.created(newOrder);

// 错误响应
return Result.error("用户名或密码错误");

// 使用枚举的错误响应
return Result.error(ResponseCodeEnum.USER_NOT_FOUND);
```

#### 响应码规范

| 状态码范围 | 含义 |
|-----------|------|
| 200 | 成功 |
| 201 | 创建成功 |
| 400 | 请求参数错误 |
| 401 | 未登录或 Token 过期 |
| 403 | 无权限访问 |
| 404 | 资源不存在 |
| 429 | 请求过于频繁 |
| 500 | 服务器内部错误 |
| 40001-49999 | 业务错误 |
| 40401-40499 | 资源不存在错误 |
| 40901-40999 | 资源冲突错误 |
| 42901-42999 | 限流错误 |
| 50001-50099 | 服务器错误 |

---

### 2. 分页封装 (PageResult & PageRequest)

#### PageRequest - 分页请求

```java
@Data
public class PageRequest {
    @Min(1)
    private Long current = 1L;   // 当前页
    
    @Min(1)
    @Max(100)
    private Long size = 10L;     // 每页大小
}
```

#### PageResult - 分页结果

```java
@Data
public class PageResult<T> implements Serializable {
    private List<T> records;     // 数据列表
    private Long total;          // 总记录数
    private Long current;        // 当前页
    private Long size;           // 每页大小
    private Long pages;          // 总页数
}
```

#### 使用示例

```java
// Controller 接收分页参数
@GetMapping("/list")
public Result<PageResult<Goods>> list(@Valid PageRequest pageRequest) {
    PageResult<Goods> result = goodsService.page(pageRequest);
    return Result.success(result);
}

// Service 构建分页结果
public PageResult<Goods> page(PageRequest request) {
    Page<Goods> page = new Page<>(request.getCurrent(), request.getSize());
    goodsMapper.selectPage(page, null);
    
    return PageResult.<Goods>builder()
            .records(page.getRecords())
            .total(page.getTotal())
            .current(page.getCurrent())
            .size(page.getSize())
            .pages(page.getPages())
            .build();
}
```

---

### 3. 业务异常 (BusinessException)

`BusinessException` 是系统统一的业务异常类，用于处理业务逻辑错误。

```java
@Getter
public class BusinessException extends RuntimeException {
    private final Integer code;     // 错误码
    private final String message;   // 错误消息
}
```

#### 使用示例

```java
// 简单错误消息
throw new BusinessException("库存不足");

// 指定错误码
throw new BusinessException(40008, "商品库存不足");

// 使用枚举
throw new BusinessException(ResponseCodeEnum.STOCK_NOT_ENOUGH);

// 自定义消息
throw new BusinessException(ResponseCodeEnum.STOCK_NOT_ENOUGH, "该商品已售罄");
```

#### 全局异常处理建议

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        return Result.error(e.getCode(), e.getMessage());
    }
}
```

---

### 4. JWT 工具类 (JwtUtils)

`JwtUtils` 提供 Token 的生成、解析和验证功能，支持用户和管理员两种主体类型。

#### Token 类型

| Token 类型 | 有效期 | 用途 |
|-----------|--------|------|
| Access Token | 30 分钟 | 接口访问凭证 |
| Refresh Token | 7 天 | 刷新 Access Token |

#### 核心方法

```java
// 生成用户访问 Token
String accessToken = JwtUtils.generateAccessToken(userId);

// 生成用户刷新 Token
String refreshToken = JwtUtils.generateRefreshToken(userId);

// 生成管理员 Token
String adminToken = JwtUtils.generateAdminAccessToken(adminId, role);

// 解析 Token
Claims claims = JwtUtils.parseToken(token);

// 获取用户ID
Long userId = JwtUtils.getUserIdFromToken(token);

// 验证 Token
boolean valid = JwtUtils.validateToken(token);

// 检查是否过期
boolean expired = JwtUtils.isTokenExpired(token);

// 获取剩余时间
Long remainingTime = JwtUtils.getRemainingTime(token);
```

#### Token 结构

```json
{
  "userId": 10001,
  "type": "access",
  "subjectType": "user",
  "role": "USER",
  "sub": "10001",
  "iat": 1704067200,
  "exp": 1704069000
}
```

---

### 5. 雪花算法 ID 生成器 (SnowflakeIdWorker)

`SnowflakeIdWorker` 实现了 Twitter 雪花算法，用于生成全局唯一的分布式 ID。

#### ID 结构

```
0 - 0000000000 0000000000 0000000000 0000000000 0 - 00000 - 00000 - 000000000000
1位标识位 + 41位时间戳 + 5位数据中心ID + 5位机器ID + 12位序列号
```

#### 核心方法

```java
// 获取单例实例
SnowflakeIdWorker idWorker = SnowflakeIdWorker.getInstance();

// 生成唯一ID
long id = idWorker.nextId();

// 生成订单号（带日期前缀）
String orderNo = idWorker.generateOrderNo();  // SEK20240115000000000001

// 生成支付流水号
String payNo = idWorker.generatePayNo();      // PAY20240115000000000001
```

#### 特性

- **全局唯一**：基于时间戳 + 数据中心ID + 机器ID + 序列号
- **趋势递增**：按时间顺序生成，有利于数据库索引
- **高性能**：单机每秒可生成 409.6 万个 ID
- **自动配置**：基于 IP 和进程 ID 自动生成数据中心和机器 ID

---

### 6. 上下文工具类 (UserContext & AdminContext)

使用 ThreadLocal 存储当前登录用户信息，实现请求级别的数据共享。

#### UserContext - 用户上下文

```java
// 设置当前用户ID（在拦截器中调用）
UserContext.setCurrentUserId(userId);

// 获取当前用户ID（在业务代码中使用）
Long userId = UserContext.getCurrentUserId();

// 清除上下文（在请求结束时调用）
UserContext.clear();
```

#### AdminContext - 管理员上下文

```java
// 设置当前管理员ID
AdminContext.setCurrentAdminId(adminId);

// 获取当前管理员ID
Long adminId = AdminContext.getCurrentAdminId();

// 清除上下文
AdminContext.clear();
```

#### 使用场景

```java
@Service
public class OrderService {
    
    public void createOrder(CreateOrderRequest request) {
        // 从上下文中获取当前用户ID
        Long userId = UserContext.getCurrentUserId();
        
        Order order = new Order();
        order.setUserId(userId);
        order.setOrderNo(SnowflakeIdWorker.getInstance().generateOrderNo());
        // ...
    }
}
```

---

### 7. Redis Key 常量 (RedisKeyConstant)

统一管理所有 Redis Key，避免硬编码和 Key 冲突。

#### Key 命名规范

```
seckill:{module}:{type}:{identifier}
```

#### 常用 Key 列表

| Key 常量 | Key 格式 | 用途 |
|---------|---------|------|
| `USER_TOKEN` | `seckill:user:token:{token}` | 用户 Token |
| `USER_INFO` | `seckill:user:info:{userId}` | 用户信息缓存 |
| `GOODS_INFO` | `seckill:goods:info:{goodsId}` | 商品信息缓存 |
| `GOODS_STOCK` | `seckill:goods:stock:{goodsId}` | 商品库存 |
| `SECKILL_STOCK` | `seckill:stock:{activityId}:{goodsId}` | 秒杀库存 |
| `SECKILL_RECORD` | `seckill:record:{activityId}:{goodsId}:{userId}` | 秒杀记录（防重复）|
| `SECKILL_PATH` | `seckill:path:{userId}:{goodsId}` | 秒杀地址（隐藏真实地址）|
| `SECKILL_VERIFY` | `seckill:verify:{userId}:{goodsId}` | 秒杀验证码 |
| `RATE_LIMIT` | `seckill:rate:limit:{uri}:{userId}` | 接口限流 |
| `LOCK` | `seckill:lock:{resource}` | 分布式锁 |

#### 使用示例

```java
// 拼接 Key
String key = RedisKeyConstant.join(RedisKeyConstant.USER_INFO, userId);
// 结果: seckill:user:info:10001

// 设置缓存
redisTemplate.opsForValue().set(
    RedisKeyConstant.USER_INFO + userId, 
    userInfo, 
    Duration.ofMinutes(30)
);
```

---

### 8. JSON 工具类 (JsonUtils)

基于 Fastjson2 封装的 JSON 处理工具。

```java
// 对象转 JSON 字符串
String json = JsonUtils.toJson(user);

// JSON 字符串转对象
User user = JsonUtils.parseObject(json, User.class);

// JSON 字符串转列表
List<User> users = JsonUtils.parseArray(jsonArray, User.class);

// 美化输出
String prettyJson = JsonUtils.toPrettyJson(user);
```

---

## 枚举详解

### ResponseCodeEnum - 响应码枚举

| 枚举值 | 码值 | 说明 |
|-------|------|------|
| SUCCESS | 200 | 操作成功 |
| UNAUTHORIZED | 401 | 未登录或 Token 过期 |
| STOCK_NOT_ENOUGH | 40008 | 商品库存不足 |
| SECKILL_NOT_START | 40009 | 秒杀活动未开始 |
| REPEAT_SECKILL | 40011 | 重复秒杀 |
| USER_NOT_FOUND | 40401 | 用户不存在 |
| RATE_LIMIT | 42901 | 请求过于频繁 |

### 业务状态枚举

- **OrderStatusEnum**: 订单状态（待支付、已支付、已发货、已完成、已取消）
- **PayStatusEnum**: 支付状态（未支付、支付中、已支付、支付失败）
- **PayTypeEnum**: 支付类型（支付宝、微信支付、余额支付）
- **SeckillStatusEnum**: 秒杀活动状态（未开始、进行中、已结束）

---

## 依赖说明

```xml
<dependencies>
    <!-- Spring Boot Web -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    
    <!-- Spring Boot Validation -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
    
    <!-- Spring Boot Data Redis -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>
    
    <!-- JWT -->
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-api</artifactId>
    </dependency>
    
    <!-- Fastjson2 -->
    <dependency>
        <groupId>com.alibaba.fastjson2</groupId>
        <artifactId>fastjson2</artifactId>
    </dependency>
    
    <!-- Hutool -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
    </dependency>
    
    <!-- Lombok -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
    </dependency>
    
    <!-- Knife4j -->
    <dependency>
        <groupId>com.github.xiaoymin</groupId>
        <artifactId>knife4j-openapi3-jakarta-spring-boot-starter</artifactId>
    </dependency>
</dependencies>
```

---

## 使用建议

### 1. 异常处理

- 业务逻辑错误使用 `BusinessException`
- 系统错误使用标准异常或自定义异常
- 在 Controller 层统一捕获并转换为 `Result` 响应

### 2. 上下文使用

- 在拦截器中设置上下文
- 在请求结束时清除上下文（防止内存泄漏）
- 业务代码中优先使用上下文获取用户信息

### 3. ID 生成

- 订单号使用 `generateOrderNo()`
- 支付流水号使用 `generatePayNo()`
- 其他业务 ID 使用 `nextId()`

### 4. Redis Key 管理

- 所有 Key 必须在 `RedisKeyConstant` 中定义
- 使用 `join()` 方法拼接 Key
- 注意设置合理的过期时间

---

## 相关文档

- [父模块文档](../README.md)
- [基础设施模块](../seckill-infrastructure/README.md)

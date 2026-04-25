package com.seckill.order.service;

import com.seckill.order.dto.CreateSeckillOrderCommand;
import com.seckill.order.dto.CreateSeckillOrderResult;

public interface OrderCreateService {

    CreateSeckillOrderResult createSeckillOrder(CreateSeckillOrderCommand command);
}

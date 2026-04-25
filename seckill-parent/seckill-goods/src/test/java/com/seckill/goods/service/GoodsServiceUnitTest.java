package com.seckill.goods.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.seckill.common.enums.ResponseCodeEnum;
import com.seckill.common.exception.BusinessException;
import com.seckill.common.result.PageResult;
import com.seckill.goods.dto.AdminGoodsListResponse;
import com.seckill.goods.dto.AdminGoodsSaveRequest;
import com.seckill.goods.dto.GoodsDetailResponse;
import com.seckill.goods.dto.GoodsListResponse;
import com.seckill.goods.entity.Category;
import com.seckill.goods.entity.Goods;
import com.seckill.goods.entity.SeckillActivity;
import com.seckill.goods.entity.SeckillGoods;
import com.seckill.goods.mapper.CategoryMapper;
import com.seckill.goods.mapper.GoodsMapper;
import com.seckill.goods.mapper.SeckillActivityMapper;
import com.seckill.goods.mapper.SeckillGoodsMapper;
import com.seckill.goods.service.impl.GoodsServiceImpl;
import com.seckill.infrastructure.utils.RedisUtils;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GoodsService 单元测试")
class GoodsServiceUnitTest {

    @BeforeAll
    static void initMybatisPlusLambdaCache() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, Goods.class);
        TableInfoHelper.initTableInfo(assistant, Category.class);
        TableInfoHelper.initTableInfo(assistant, SeckillActivity.class);
        TableInfoHelper.initTableInfo(assistant, SeckillGoods.class);
    }

    @Mock
    private GoodsMapper goodsMapper;

    @Mock
    private CategoryMapper categoryMapper;

    @Mock
    private SeckillActivityMapper seckillActivityMapper;

    @Mock
    private SeckillGoodsMapper seckillGoodsMapper;

    @Mock
    private RedisUtils redisUtils;

    private GoodsServiceImpl goodsService;

    @BeforeEach
    void setUp() {
        goodsService = new GoodsServiceImpl(goodsMapper, categoryMapper, seckillActivityMapper, seckillGoodsMapper, redisUtils);
    }

    @Nested
    @DisplayName("getPublicGoodsList 测试")
    class GetPublicGoodsListTests {

        @Test
        @DisplayName("缓存命中直接返回")
        void getPublicGoodsList_CacheHit() {
            PageResult<GoodsListResponse> cached = PageResult.build(Collections.emptyList(), 0L, 10L, 1L);
            when(redisUtils.get("goods:list:all:all:default:1:10")).thenReturn(cached);

            PageResult<GoodsListResponse> result = goodsService.getPublicGoodsList(null, null, null, 1L, 10L);

            assertNotNull(result);
            verify(goodsMapper, never()).selectPage(any(Page.class), any());
        }

        @Test
        @DisplayName("无筛选条件查询成功 - 有商品数据返回")
        void getPublicGoodsList_WithData() {
            when(redisUtils.get("goods:list:all:all:default:1:10")).thenReturn(null);
            Goods g = goods(1L);
            Page<Goods> page = new Page<>(1, 10);
            page.setRecords(List.of(g));
            page.setTotal(1);
            when(goodsMapper.selectPage(any(Page.class), any())).thenReturn(page);

            PageResult<GoodsListResponse> result = goodsService.getPublicGoodsList(null, null, null, 1L, 10L);

            assertEquals(1L, result.getTotal());
            GoodsListResponse first = result.getRecords().getFirst();
            assertEquals(1L, first.getGoodsId());
            assertEquals("iPhone 16 Pro Max", first.getGoodsName());
            assertEquals(0, first.getIsSeckill());
            verify(redisUtils).set(any(), any(), anyLong(), eq(TimeUnit.SECONDS));
        }

        @Test
        @DisplayName("按分类筛选 - 一级分类包含子分类商品")
        void getPublicGoodsList_WithTopLevelCategory() {
            when(redisUtils.get("goods:list:1:all:default:1:10")).thenReturn(null);
            Category parent = category(1L, "手机数码");
            parent.setParentId(0L);
            when(categoryMapper.selectById(1L)).thenReturn(parent);
            when(categoryMapper.selectList(any())).thenReturn(List.of(category(11L, "手机")));
            Page<Goods> page = new Page<>(1, 10);
            page.setRecords(List.of(goods(1L)));
            page.setTotal(1);
            when(goodsMapper.selectPage(any(Page.class), any())).thenReturn(page);

            PageResult<GoodsListResponse> result = goodsService.getPublicGoodsList(1L, null, null, 1L, 10L);

            assertEquals(1L, result.getTotal());
        }

        @Test
        @DisplayName("按分类筛选")
        void getPublicGoodsList_WithCategory() {
            when(redisUtils.get("goods:list:11:all:default:1:10")).thenReturn(null);
            when(categoryMapper.selectById(11L)).thenReturn(category(11L, "手机"));
            Page<Goods> page = new Page<>(1, 10);
            page.setRecords(Collections.emptyList());
            page.setTotal(0);
            when(goodsMapper.selectPage(any(Page.class), any())).thenReturn(page);

            PageResult<GoodsListResponse> result = goodsService.getPublicGoodsList(11L, null, null, 1L, 10L);

            assertNotNull(result);
        }

        @Test
        @DisplayName("按关键词搜索")
        void getPublicGoodsList_WithKeyword() {
            when(redisUtils.get("goods:list:all:iPhone:default:1:10")).thenReturn(null);
            Page<Goods> page = new Page<>(1, 10);
            page.setRecords(Collections.emptyList());
            page.setTotal(0);
            when(goodsMapper.selectPage(any(Page.class), any())).thenReturn(page);

            PageResult<GoodsListResponse> result = goodsService.getPublicGoodsList(null, "iPhone", null, 1L, 10L);

            assertNotNull(result);
        }

        @Test
        @DisplayName("按价格排序")
        void getPublicGoodsList_WithSort() {
            when(redisUtils.get("goods:list:all:all:price_asc:1:10")).thenReturn(null);
            Page<Goods> page = new Page<>(1, 10);
            page.setRecords(Collections.emptyList());
            page.setTotal(0);
            when(goodsMapper.selectPage(any(Page.class), any())).thenReturn(page);

            PageResult<GoodsListResponse> result = goodsService.getPublicGoodsList(null, null, "price_asc", 1L, 10L);

            assertNotNull(result);
        }

        @Test
        @DisplayName("空结果不缓存")
        void getPublicGoodsList_EmptyResult() {
            when(redisUtils.get("goods:list:all:all:default:1:10")).thenReturn(null);
            Page<Goods> page = new Page<>(1, 10);
            page.setRecords(Collections.emptyList());
            page.setTotal(0);
            when(goodsMapper.selectPage(any(Page.class), any())).thenReturn(page);

            PageResult<GoodsListResponse> result = goodsService.getPublicGoodsList(null, null, null, 1L, 10L);

            assertEquals(0L, result.getTotal());
        }

        @Test
        @DisplayName("分类不存在时抛出异常")
        void getPublicGoodsList_CategoryNotFound() {
            when(redisUtils.get("goods:list:999:all:default:1:10")).thenReturn(null);
            when(categoryMapper.selectById(999L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> goodsService.getPublicGoodsList(999L, null, null, 1L, 10L));
            assertEquals(ResponseCodeEnum.CATEGORY_NOT_FOUND.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("带秒杀标记返回")
        void getPublicGoodsList_WithSeckillMark() {
            when(redisUtils.get("goods:list:all:all:default:1:10")).thenReturn(null);
            Goods g = goods(1L);
            g.setCategoryId(11L);
            Page<Goods> page = new Page<>(1, 10);
            page.setRecords(List.of(g));
            page.setTotal(1);
            when(goodsMapper.selectPage(any(Page.class), any())).thenReturn(page);

            PageResult<GoodsListResponse> result = goodsService.getPublicGoodsList(null, null, null, 1L, 10L);

            assertNotNull(result);
            assertEquals(1L, result.getTotal());
        }
    }

    @Nested
    @DisplayName("getPublicGoodsDetail 测试")
    class GetPublicGoodsDetailTests {

        @Test
        @DisplayName("缓存命中直接返回")
        void getPublicGoodsDetail_CacheHit() {
            GoodsDetailResponse cached = new GoodsDetailResponse();
            cached.setGoodsId(1L);
            when(redisUtils.get("goods:detail:1")).thenReturn(cached);

            GoodsDetailResponse result = goodsService.getPublicGoodsDetail(1L);

            assertNotNull(result);
            assertEquals(1L, result.getGoodsId());
            verify(goodsMapper, never()).selectById(anyLong());
        }

        @Test
        @DisplayName("无秒杀信息的商品详情")
        void getPublicGoodsDetail_NoSeckill() {
            when(redisUtils.get("goods:detail:1")).thenReturn(null);
            when(redisUtils.hasKey("goods:null:1")).thenReturn(false);
            when(goodsMapper.selectById(1L)).thenReturn(goods(1L));

            GoodsDetailResponse result = goodsService.getPublicGoodsDetail(1L);

            assertNotNull(result);
            assertEquals(1L, result.getGoodsId());
            assertEquals(0, result.getIsSeckill());
            verify(redisUtils).set(eq("goods:detail:1"), eq(result), anyLong(), eq(TimeUnit.SECONDS));
        }

        @Test
        @DisplayName("空缓存命中抛出异常（防缓存穿透）")
        void getPublicGoodsDetail_NullCacheHit() {
            when(redisUtils.get("goods:detail:999")).thenReturn(null);
            when(redisUtils.hasKey("goods:null:999")).thenReturn(true);

            BusinessException ex = assertThrows(BusinessException.class, () -> goodsService.getPublicGoodsDetail(999L));
            assertEquals(ResponseCodeEnum.GOODS_NOT_FOUND.getCode(), ex.getCode());
            verify(goodsMapper, never()).selectById(anyLong());
        }

        @Test
        @DisplayName("已下架商品抛出异常")
        void getPublicGoodsDetail_Offline() {
            when(redisUtils.get("goods:detail:1")).thenReturn(null);
            when(redisUtils.hasKey("goods:null:1")).thenReturn(false);
            Goods offlineGoods = goods(1L);
            offlineGoods.setStatus(0);
            when(goodsMapper.selectById(1L)).thenReturn(offlineGoods);

            BusinessException ex = assertThrows(BusinessException.class, () -> goodsService.getPublicGoodsDetail(1L));
            assertEquals(ResponseCodeEnum.GOODS_NOT_FOUND.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("商品不存在抛出异常")
        void getPublicGoodsDetail_NotFound() {
            when(redisUtils.get("goods:detail:999")).thenReturn(null);
            when(redisUtils.hasKey("goods:null:999")).thenReturn(false);
            when(goodsMapper.selectById(999L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class, () -> goodsService.getPublicGoodsDetail(999L));
            assertEquals(ResponseCodeEnum.GOODS_NOT_FOUND.getCode(), ex.getCode());
            verify(redisUtils).set(eq("goods:null:999"), any(), anyLong(), eq(TimeUnit.SECONDS));
        }

        @Test
        @DisplayName("带秒杀信息返回详情")
        void getPublicGoodsDetail_WithSeckillInfo() {
            when(redisUtils.get("goods:detail:1")).thenReturn(null);
            when(redisUtils.hasKey("goods:null:1")).thenReturn(false);
            when(goodsMapper.selectById(1L)).thenReturn(goods(1L));
            SeckillGoods sg = seckillGoods(1L, 1L);
            sg.setSeckillPrice(new BigDecimal("7999.00"));
            when(seckillGoodsMapper.selectList(any())).thenReturn(List.of(sg));
            SeckillActivity activity = new SeckillActivity();
            activity.setId(1L);
            activity.setStartTime(LocalDateTime.now().minusMinutes(5));
            activity.setEndTime(LocalDateTime.now().plusMinutes(30));
            activity.setStatus(1);
            when(seckillActivityMapper.selectBatchIds(List.of(1L))).thenReturn(List.of(activity));

            GoodsDetailResponse result = goodsService.getPublicGoodsDetail(1L);

            assertNotNull(result);
            assertEquals(1, result.getIsSeckill());
            assertNotNull(result.getSeckillPrice());
        }
    }

    @Nested
    @DisplayName("getAdminGoodsList 测试")
    class GetAdminGoodsListTests {

        @Test
        @DisplayName("管理端商品列表查询成功 - 有商品数据")
        void getAdminGoodsList_Success() {
            Goods g = goods(1L);
            Page<Goods> page = new Page<>(1, 10);
            page.setRecords(List.of(g));
            page.setTotal(1);
            when(goodsMapper.selectPage(any(Page.class), any())).thenReturn(page);
            when(categoryMapper.selectBatchIds(any())).thenReturn(List.of(category(11L, "手机")));

            PageResult<AdminGoodsListResponse> result = goodsService.getAdminGoodsList(null, null, null, 1L, 10L);

            assertNotNull(result);
            assertEquals(1L, result.getTotal());
            assertEquals("iPhone 16 Pro Max", result.getRecords().getFirst().getGoodsName());
        }

        @Test
        @DisplayName("按状态筛选")
        void getAdminGoodsList_StatusFilter() {
            Page<Goods> page = new Page<>(1, 10);
            page.setRecords(Collections.emptyList());
            page.setTotal(0);
            when(goodsMapper.selectPage(any(Page.class), any())).thenReturn(page);

            PageResult<AdminGoodsListResponse> result = goodsService.getAdminGoodsList(null, null, 1, 1L, 10L);

            assertNotNull(result);
        }

        @Test
        @DisplayName("空结果返回")
        void getAdminGoodsList_EmptyResult() {
            Page<Goods> page = new Page<>(1, 10);
            page.setRecords(Collections.emptyList());
            page.setTotal(0);
            when(goodsMapper.selectPage(any(Page.class), any())).thenReturn(page);

            PageResult<AdminGoodsListResponse> result = goodsService.getAdminGoodsList(null, null, null, 1L, 10L);

            assertEquals(0L, result.getTotal());
        }
    }

    @Nested
    @DisplayName("createGoods 测试")
    class CreateGoodsTests {

        @Test
        @DisplayName("创建商品成功 - 默认下架状态")
        void createGoods_Success_DefaultOffline() {
            AdminGoodsSaveRequest request = new AdminGoodsSaveRequest();
            request.setGoodsName("测试商品");
            request.setCategoryId(11L);
            request.setPrice(new BigDecimal("99.00"));
            request.setStock(10);
            request.setGoodsImages(List.of("https://example.com/1.jpg"));

            when(categoryMapper.selectById(11L)).thenReturn(category(11L, "手机"));
            when(goodsMapper.insert(any(Goods.class))).thenAnswer(invocation -> {
                Goods goods = invocation.getArgument(0);
                goods.setId(100L);
                return 1;
            });

            Long goodsId = goodsService.createGoods(request);

            assertEquals(100L, goodsId);
            verify(goodsMapper).insert(any(Goods.class));
        }

        @Test
        @DisplayName("分类不存在")
        void createGoods_CategoryNotFound() {
            AdminGoodsSaveRequest request = new AdminGoodsSaveRequest();
            request.setGoodsName("测试商品");
            request.setCategoryId(999L);
            request.setPrice(new BigDecimal("99.00"));
            request.setStock(10);

            when(categoryMapper.selectById(999L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class, () -> goodsService.createGoods(request));
            assertEquals(ResponseCodeEnum.CATEGORY_NOT_FOUND.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("非二级分类不允许创建")
        void createGoods_CategoryNotLeaf() {
            AdminGoodsSaveRequest request = new AdminGoodsSaveRequest();
            request.setGoodsName("测试商品");
            request.setCategoryId(1L);
            request.setPrice(new BigDecimal("99.00"));
            request.setStock(10);

            Category topCategory = category(1L, "手机数码");
            topCategory.setParentId(0L);
            when(categoryMapper.selectById(1L)).thenReturn(topCategory);

            BusinessException ex = assertThrows(BusinessException.class, () -> goodsService.createGoods(request));
            assertEquals(ResponseCodeEnum.PARAM_ERROR.getCode(), ex.getCode());
            assertTrue(ex.getMessage().contains("二级分类"));
        }

        @Test
        @DisplayName("创建商品成功 - 指定上架状态")
        void createGoods_Success_WithStatus() {
            AdminGoodsSaveRequest request = new AdminGoodsSaveRequest();
            request.setGoodsName("测试商品");
            request.setCategoryId(11L);
            request.setPrice(new BigDecimal("99.00"));
            request.setStock(10);
            request.setStatus(1);
            request.setGoodsImages(List.of("https://example.com/1.jpg"));

            when(categoryMapper.selectById(11L)).thenReturn(category(11L, "手机"));
            when(goodsMapper.insert(any(Goods.class))).thenAnswer(invocation -> {
                Goods goods = invocation.getArgument(0);
                goods.setId(100L);
                return 1;
            });

            Long goodsId = goodsService.createGoods(request);

            assertEquals(100L, goodsId);
        }
    }

    @Nested
    @DisplayName("updateGoods 测试")
    class UpdateGoodsTests {

        @Test
        @DisplayName("更新商品成功并清理缓存")
        void updateGoods_Success() {
            AdminGoodsSaveRequest request = new AdminGoodsSaveRequest();
            request.setGoodsName("更新商品");
            request.setCategoryId(11L);
            request.setPrice(new BigDecimal("199.00"));
            request.setStock(20);
            request.setGoodsImages(List.of("https://example.com/new.jpg"));
            request.setDescription("描述");
            request.setDetail("详情");

            when(goodsMapper.selectById(1L)).thenReturn(goods(1L));
            when(categoryMapper.selectById(11L)).thenReturn(category(11L, "手机"));
            when(seckillGoodsMapper.selectList(any())).thenReturn(Collections.emptyList());
            when(goodsMapper.updateById(any(Goods.class))).thenReturn(1);

            goodsService.updateGoods(1L, request);

            verify(goodsMapper).updateById(any(Goods.class));
            verify(redisUtils).pipelineDelete(any());
            verify(redisUtils).deleteByPattern("goods:list:*");
            verify(redisUtils).deleteByPattern("seckill:activity:list:*");
        }

        @Test
        @DisplayName("商品不存在")
        void updateGoods_NotFound() {
            AdminGoodsSaveRequest request = new AdminGoodsSaveRequest();
            request.setGoodsName("更新商品");
            request.setCategoryId(11L);
            request.setPrice(new BigDecimal("199.00"));
            request.setStock(20);

            when(goodsMapper.selectById(999L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class, () -> goodsService.updateGoods(999L, request));
            assertEquals(ResponseCodeEnum.GOODS_NOT_FOUND.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("非二级分类")
        void updateGoods_CategoryNotLeaf() {
            AdminGoodsSaveRequest request = new AdminGoodsSaveRequest();
            request.setGoodsName("更新商品");
            request.setCategoryId(1L);
            request.setPrice(new BigDecimal("199.00"));
            request.setStock(20);

            when(goodsMapper.selectById(1L)).thenReturn(goods(1L));
            Category topCategory = category(1L, "手机数码");
            topCategory.setParentId(0L);
            when(categoryMapper.selectById(1L)).thenReturn(topCategory);

            BusinessException ex = assertThrows(BusinessException.class, () -> goodsService.updateGoods(1L, request));
            assertEquals(ResponseCodeEnum.PARAM_ERROR.getCode(), ex.getCode());
        }
    }

    @Nested
    @DisplayName("deleteGoods 测试")
    class DeleteGoodsTests {

        @Test
        @DisplayName("删除商品成功并清理缓存")
        void deleteGoods_Success() {
            when(goodsMapper.selectById(1L)).thenReturn(goods(1L));
            when(seckillGoodsMapper.selectList(any())).thenReturn(Collections.emptyList());
            when(goodsMapper.deleteById(1L)).thenReturn(1);

            goodsService.deleteGoods(1L);

            verify(goodsMapper).deleteById(1L);
            verify(redisUtils).pipelineDelete(any());
        }

        @Test
        @DisplayName("参与秒杀不允许删除")
        void deleteGoods_Fail_HasOngoingActivity() {
            when(goodsMapper.selectById(1L)).thenReturn(goods(1L));
            when(seckillGoodsMapper.selectList(any())).thenReturn(List.of(seckillGoods(1L, 1L)));
            when(seckillActivityMapper.selectCount(any())).thenReturn(1L);

            BusinessException ex = assertThrows(BusinessException.class, () -> goodsService.deleteGoods(1L));
            assertEquals(ResponseCodeEnum.BAD_REQUEST.getCode(), ex.getCode());
            assertTrue(ex.getMessage().contains("秒杀活动"));
            verify(goodsMapper, never()).deleteById(anyLong());
        }

        @Test
        @DisplayName("商品不存在")
        void deleteGoods_NotFound() {
            when(goodsMapper.selectById(999L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class, () -> goodsService.deleteGoods(999L));
            assertEquals(ResponseCodeEnum.GOODS_NOT_FOUND.getCode(), ex.getCode());
        }
    }

    @Nested
    @DisplayName("updateGoodsStatus 测试")
    class UpdateGoodsStatusTests {

        @Test
        @DisplayName("上架成功并清理缓存")
        void updateGoodsStatus_Success_StatusUp() {
            when(goodsMapper.selectById(1L)).thenReturn(goods(1L));
            when(seckillGoodsMapper.selectList(any())).thenReturn(Collections.emptyList());
            when(goodsMapper.updateById(any(Goods.class))).thenReturn(1);

            goodsService.updateGoodsStatus(1L, 1);

            verify(goodsMapper).updateById(any(Goods.class));
            verify(redisUtils).pipelineDelete(any());
        }

        @Test
        @DisplayName("下架失败 - 正在参与秒杀活动")
        void updateGoodsStatus_Fail_WhenHasOngoingActivity() {
            when(goodsMapper.selectById(1L)).thenReturn(goods(1L));
            when(seckillGoodsMapper.selectList(any())).thenReturn(List.of(seckillGoods(1L, 1L)));
            when(seckillActivityMapper.selectCount(any())).thenReturn(1L);

            BusinessException ex = assertThrows(BusinessException.class, () -> goodsService.updateGoodsStatus(1L, 0));
            assertEquals(ResponseCodeEnum.BAD_REQUEST.getCode(), ex.getCode());
            assertTrue(ex.getMessage().contains("秒杀活动"));
        }
    }

    private Goods goods(Long id) {
        Goods goods = new Goods();
        goods.setId(id);
        goods.setName("iPhone 16 Pro Max");
        goods.setCategoryId(11L);
        goods.setPrice(new BigDecimal("9999.00"));
        goods.setStock(100);
        goods.setStatus(1);
        goods.setCoverImage("https://example.com/iphone.jpg");
        goods.setImages("https://example.com/1.jpg");
        return goods;
    }

    private Category category(Long id, String name) {
        Category category = new Category();
        category.setId(id);
        category.setParentId(1L);
        category.setName(name);
        category.setStatus(1);
        category.setSort(1);
        return category;
    }

    private SeckillGoods seckillGoods(Long activityId, Long goodsId) {
        SeckillGoods seckillGoods = new SeckillGoods();
        seckillGoods.setActivityId(activityId);
        seckillGoods.setGoodsId(goodsId);
        seckillGoods.setSeckillPrice(new BigDecimal("7999.00"));
        seckillGoods.setSeckillStock(50);
        seckillGoods.setLimitPerUser(1);
        return seckillGoods;
    }
}

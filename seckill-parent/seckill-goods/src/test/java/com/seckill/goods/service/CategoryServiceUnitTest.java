package com.seckill.goods.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.seckill.common.enums.ResponseCodeEnum;
import com.seckill.common.exception.BusinessException;
import com.seckill.goods.dto.CategoryRequest;
import com.seckill.goods.dto.CategoryTreeResponse;
import com.seckill.goods.entity.Category;
import com.seckill.goods.entity.Goods;
import com.seckill.goods.mapper.CategoryMapper;
import com.seckill.goods.mapper.GoodsMapper;
import com.seckill.goods.service.impl.CategoryServiceImpl;
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
@DisplayName("CategoryService 单元测试")
class CategoryServiceUnitTest {

    @BeforeAll
    static void initMybatisPlusLambdaCache() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, Category.class);
        TableInfoHelper.initTableInfo(assistant, Goods.class);
    }

    @Mock
    private CategoryMapper categoryMapper;

    @Mock
    private GoodsMapper goodsMapper;

    @Mock
    private RedisUtils redisUtils;

    private CategoryServiceImpl categoryService;

    @BeforeEach
    void setUp() {
        categoryService = new CategoryServiceImpl(categoryMapper, goodsMapper, redisUtils);
    }

    @Nested
    @DisplayName("getCategoryTree 测试")
    class GetCategoryTreeTests {

        @Test
        @DisplayName("分类树查询成功 - 组装两级结构并写入缓存")
        void getCategoryTree_Success_BuildTreeAndCache() {
            when(redisUtils.get("category:tree")).thenReturn(null);
            when(categoryMapper.selectList(any())).thenReturn(List.of(
                    category(1L, 0L, "手机数码"),
                    category(11L, 1L, "手机"),
                    category(12L, 1L, "平板电脑")
            ));

            List<CategoryTreeResponse> result = categoryService.getCategoryTree();

            assertEquals(1, result.size());
            assertEquals("手机数码", result.getFirst().getCategoryName());
            assertEquals(2, result.getFirst().getChildren().size());
            verify(redisUtils).set(eq("category:tree"), eq(result), anyLong(), eq(TimeUnit.SECONDS));
        }

        @Test
        @DisplayName("缓存命中直接返回")
        void getCategoryTree_Success_CacheHit() {
            CategoryTreeResponse root = new CategoryTreeResponse();
            root.setCategoryId(1L);
            root.setCategoryName("手机数码");
            when(redisUtils.get("category:tree")).thenReturn(List.of(root));

            List<CategoryTreeResponse> result = categoryService.getCategoryTree();

            assertEquals(1, result.size());
            assertEquals("手机数码", result.getFirst().getCategoryName());
            verify(categoryMapper, never()).selectList(any());
        }

        @Test
        @DisplayName("无分类返回空列表")
        void getCategoryTree_EmptyList() {
            when(redisUtils.get("category:tree")).thenReturn(null);
            when(categoryMapper.selectList(any())).thenReturn(Collections.emptyList());

            List<CategoryTreeResponse> result = categoryService.getCategoryTree();

            assertNotNull(result);
            assertEquals(0, result.size());
        }
    }

    @Nested
    @DisplayName("createCategory 测试")
    class CreateCategoryTests {

        @Test
        @DisplayName("创建一级分类成功")
        void createCategory_Success_TopLevel() {
            CategoryRequest request = new CategoryRequest();
            request.setParentId(0L);
            request.setName("手机数码");
            request.setIcon("https://example.com/icon.png");
            request.setSort(1);
            request.setStatus(1);

            when(categoryMapper.insert(any(Category.class))).thenAnswer(invocation -> {
                Category cat = invocation.getArgument(0);
                cat.setId(1L);
                return 1;
            });

            Long id = categoryService.createCategory(request);

            assertEquals(1L, id);
            verify(categoryMapper).insert(any(Category.class));
            verify(redisUtils).delete("category:tree");
        }

        @Test
        @DisplayName("创建二级分类成功")
        void createCategory_Success_SubCategory() {
            CategoryRequest request = new CategoryRequest();
            request.setParentId(1L);
            request.setName("手机");
            request.setStatus(1);

            Category parent = category(1L, 0L, "手机数码");
            parent.setStatus(1);
            when(categoryMapper.selectById(1L)).thenReturn(parent);
            when(categoryMapper.insert(any(Category.class))).thenAnswer(invocation -> {
                Category cat = invocation.getArgument(0);
                cat.setId(11L);
                return 1;
            });

            Long id = categoryService.createCategory(request);

            assertEquals(11L, id);
        }

        @Test
        @DisplayName("父分类非一级 - 不支持三级")
        void createCategory_Fail_ParentNotTopLevel() {
            CategoryRequest request = new CategoryRequest();
            request.setParentId(11L);
            request.setName("智能手机");

            Category parent = category(11L, 1L, "手机");
            parent.setStatus(1);
            when(categoryMapper.selectById(11L)).thenReturn(parent);

            BusinessException ex = assertThrows(BusinessException.class, () -> categoryService.createCategory(request));
            assertEquals(ResponseCodeEnum.PARAM_ERROR.getCode(), ex.getCode());
            assertTrue(ex.getMessage().contains("两级分类"));
        }

        @Test
        @DisplayName("父分类已禁用")
        void createCategory_Fail_ParentDisabled() {
            CategoryRequest request = new CategoryRequest();
            request.setParentId(1L);
            request.setName("手机");

            Category parent = category(1L, 0L, "手机数码");
            parent.setStatus(0);
            when(categoryMapper.selectById(1L)).thenReturn(parent);

            BusinessException ex = assertThrows(BusinessException.class, () -> categoryService.createCategory(request));
            assertEquals(ResponseCodeEnum.PARAM_ERROR.getCode(), ex.getCode());
            assertTrue(ex.getMessage().contains("已禁用"));
        }

        @Test
        @DisplayName("父分类不存在")
        void createCategory_Fail_ParentNotFound() {
            CategoryRequest request = new CategoryRequest();
            request.setParentId(999L);
            request.setName("手机");

            when(categoryMapper.selectById(999L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class, () -> categoryService.createCategory(request));
            assertEquals(ResponseCodeEnum.CATEGORY_NOT_FOUND.getCode(), ex.getCode());
        }
    }

    @Nested
    @DisplayName("updateCategory 测试")
    class UpdateCategoryTests {

        @Test
        @DisplayName("更新分类成功")
        void updateCategory_Success() {
            CategoryRequest request = new CategoryRequest();
            request.setParentId(1L);
            request.setName("手机更新");
            request.setIcon("https://example.com/new-icon.png");
            request.setSort(2);
            request.setStatus(1);

            Category existing = category(11L, 1L, "手机");
            when(categoryMapper.selectById(11L)).thenReturn(existing);
            Category parent = category(1L, 0L, "手机数码");
            parent.setStatus(1);
            when(categoryMapper.selectById(1L)).thenReturn(parent);
            when(categoryMapper.updateById(any(Category.class))).thenReturn(1);
            when(goodsMapper.selectList(any())).thenReturn(Collections.emptyList());

            categoryService.updateCategory(11L, request);

            verify(categoryMapper).updateById(any(Category.class));
        }

        @Test
        @DisplayName("不能将自己设为父分类")
        void updateCategory_Fail_SelfReference() {
            CategoryRequest request = new CategoryRequest();
            request.setParentId(11L);
            request.setName("手机");

            Category existing = category(11L, 1L, "手机");
            when(categoryMapper.selectById(11L)).thenReturn(existing);

            BusinessException ex = assertThrows(BusinessException.class, () -> categoryService.updateCategory(11L, request));
            assertEquals(ResponseCodeEnum.PARAM_ERROR.getCode(), ex.getCode());
            assertTrue(ex.getMessage().contains("自己"));
        }

        @Test
        @DisplayName("分类不存在")
        void updateCategory_Fail_NotFound() {
            CategoryRequest request = new CategoryRequest();
            request.setName("手机");

            when(categoryMapper.selectById(999L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class, () -> categoryService.updateCategory(999L, request));
            assertEquals(ResponseCodeEnum.CATEGORY_NOT_FOUND.getCode(), ex.getCode());
        }
    }

    @Nested
    @DisplayName("deleteCategory 测试")
    class DeleteCategoryTests {

        @Test
        @DisplayName("删除分类失败 - 存在子分类")
        void deleteCategory_Fail_WhenHasChildren() {
            when(categoryMapper.selectById(1L)).thenReturn(category(1L, 0L, "手机数码"));
            when(categoryMapper.selectCount(any())).thenReturn(1L);

            BusinessException exception = assertThrows(BusinessException.class, () -> categoryService.deleteCategory(1L));

            assertEquals(ResponseCodeEnum.BAD_REQUEST.getCode(), exception.getCode());
            assertTrue(exception.getMessage().contains("子分类"));
            verify(goodsMapper, never()).selectCount(any());
        }

        @Test
        @DisplayName("删除分类失败 - 存在关联商品")
        void deleteCategory_Fail_WhenHasGoods() {
            when(categoryMapper.selectById(11L)).thenReturn(category(11L, 1L, "手机"));
            when(categoryMapper.selectCount(any())).thenReturn(0L);
            when(goodsMapper.selectCount(any())).thenReturn(2L);

            BusinessException exception = assertThrows(BusinessException.class, () -> categoryService.deleteCategory(11L));

            assertEquals(ResponseCodeEnum.BAD_REQUEST.getCode(), exception.getCode());
            assertTrue(exception.getMessage().contains("商品"));
        }

        @Test
        @DisplayName("删除成功")
        void deleteCategory_Success() {
            when(categoryMapper.selectById(11L)).thenReturn(category(11L, 1L, "手机"));
            when(categoryMapper.selectCount(any())).thenReturn(0L);
            when(goodsMapper.selectCount(any())).thenReturn(0L);
            when(categoryMapper.deleteById(11L)).thenReturn(1);

            categoryService.deleteCategory(11L);

            verify(categoryMapper).deleteById(11L);
        }
    }

    private Category category(Long id, Long parentId, String name) {
        Category category = new Category();
        category.setId(id);
        category.setParentId(parentId);
        category.setName(name);
        category.setStatus(1);
        category.setSort(1);
        return category;
    }
}

package com.atguigu.gulimall.product.dao;

import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品三级分类
 * 
 * @author dcq
 * @email 1193882675@qq.com
 * @date 2026-01-13 14:56:01
 */
@Mapper
public interface CategoryDao extends BaseMapper<CategoryEntity> {
	
}

package com.atguigu.gulimall.product.dao;

import com.atguigu.common.entity.product.SkuSaleAttrValueEntity;
import com.atguigu.common.vo.product.SkuItemSaleAttrVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * sku销售属性&值
 * 
 * @author dcq
 * @email 1193882675@qq.com
 * @date 2026-01-13 14:56:01
 */
@Mapper
public interface SkuSaleAttrValueDao extends BaseMapper<SkuSaleAttrValueEntity> {

    /**
     * 获取spu下的所有销售属性组合
     */
    List<SkuItemSaleAttrVO> getSaleAttrBySpuId(@Param("spuId") Long spuId);

    /**
     * 根据skuId查询销售属性值
     * @param skuId
     * @return attrName:attrValue
     */
    List<String> getSkuSaleAttrValuesAsStringList(@Param("skuId") Long skuId);

}

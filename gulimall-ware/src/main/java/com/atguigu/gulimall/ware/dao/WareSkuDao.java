package com.atguigu.gulimall.ware.dao;

import com.atguigu.common.entity.ware.WareSkuEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Set;

/**
 * 商品库存
 * 
 * @author dcq
 * @email 1193882675@qq.com
 * @date 2026-01-13 19:52:12
 */
@Mapper
public interface WareSkuDao extends BaseMapper<WareSkuEntity> {

    void addStock(@Param("skuId") Long skuId, @Param("wareId") Long wareId, @Param("skuNum") Integer skuNum);

    Long getSkusStock(Long skuId);

    /**
     * 查询商品库存充足的仓库
     * @param skuIds 商品项ID集合
     * @return
     */
    List<WareSkuEntity> selectListHasSkuStock(@Param("skuIds") Set<Long> skuIds);

    /**
     * 锁定库存
     * @param skuId 商品项ID
     * @param wareId 仓库ID
     * @param count 待锁定库存数
     * @return 1成功  0失败
     */
    Long lockSkuStock(@Param("skuId") Long skuId, @Param("wareId") Long wareId, @Param("count") Integer count);


}

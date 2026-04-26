package com.atguigu.gulimall.ware.service;

import com.atguigu.common.to.ware.SkuHasStockTO;
import com.atguigu.common.to.ware.WareSkuLockTO;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.gulimall.ware.entity.WareSkuEntity;

import java.util.List;
import java.util.Map;

/**
 * 商品库存
 *
 * @author dcq
 * @email 1193882675@qq.com
 * @date 2026-01-13 19:52:12
 */
public interface WareSkuService extends IService<WareSkuEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void addStock(Long skuId, Long wareId, Integer skuNum);

    List<SkuHasStockTO> getSkusHasStock(List<Long> skuIds);

    Boolean orderLockStock(WareSkuLockTO lockTO);
}


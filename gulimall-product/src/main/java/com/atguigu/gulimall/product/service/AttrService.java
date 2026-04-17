package com.atguigu.gulimall.product.service;

import com.atguigu.common.vo.product.Attr;
import com.atguigu.common.vo.product.AttrGroupRelationVO;
import com.atguigu.common.vo.product.AttrRespVO;
import com.atguigu.common.vo.product.AttrVO;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.entity.product.AttrEntity;

import java.util.List;
import java.util.Map;

/**
 * 商品属性
 *
 * @author dcq
 * @email 1193882675@qq.com
 * @date 2026-01-13 14:56:01
 */
public interface AttrService extends IService<AttrEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void saveAttr(AttrVO attr);

    PageUtils queryBaseAttrPage(Map<String, Object> params, Long catelogId, String type);

    AttrRespVO getAttrInfo(Long attrId);

    void updateAttr(AttrVO attr);

    List<AttrEntity> getRelationAttr(Long attrgroupId);

    void deleteRelation(AttrGroupRelationVO[] vos);

    PageUtils getNoRelationAttr(Map<String, Object> params, Long attrgroupId);

    List<Long> selectSearchAttrIds(List<Long> attrIds);

    List<AttrEntity> getBatchIds(List<Long> attrIds);

}


package com.atguigu.gulimall.product.dao;

import com.atguigu.common.entity.product.AttrGroupEntity;
import com.atguigu.common.vo.product.SpuItemAttrGroupVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 属性分组
 * 
 * @author dcq
 * @email 1193882675@qq.com
 * @date 2026-01-13 14:56:01
 */
@Mapper
public interface AttrGroupDao extends BaseMapper<AttrGroupEntity> {

    /**
     * 查询当前spu对应的所有属性的分组信息以及当前分组下的所有属性对应的值
     * @param spuId
     * @param catalogId
     * @return
     */
    List<SpuItemAttrGroupVO> getAttrGroupWithAttrsBySpuId(@Param("spuId") Long spuId, @Param("catalogId") Long catalogId);
}

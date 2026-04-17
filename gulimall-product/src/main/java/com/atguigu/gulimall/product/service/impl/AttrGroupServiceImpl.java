package com.atguigu.gulimall.product.service.impl;

import com.atguigu.common.entity.product.AttrEntity;
import com.atguigu.common.vo.product.Attr;
import com.atguigu.common.vo.product.SpuItemAttrGroupVO;
import com.atguigu.gulimall.product.service.AttrService;
import com.atguigu.common.vo.product.AttrGroupWithAttrsVO;
import com.atguigu.common.vo.product.AttrVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimall.product.dao.AttrGroupDao;
import com.atguigu.common.entity.product.AttrGroupEntity;
import com.atguigu.gulimall.product.service.AttrGroupService;
import org.springframework.util.StringUtils;


@Service("attrGroupService")
public class AttrGroupServiceImpl extends ServiceImpl<AttrGroupDao, AttrGroupEntity> implements AttrGroupService {

    @Autowired
    AttrService attrService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<AttrGroupEntity> page = this.page(
                new Query<AttrGroupEntity>().getPage(params),
                new QueryWrapper<AttrGroupEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public PageUtils queryPage(Map<String, Object> params, Long catelogId) {
        String key = (String) params.get("key");
        QueryWrapper<AttrGroupEntity> wrapper = new QueryWrapper<AttrGroupEntity>();
        if (!StringUtils.isEmpty(key)) {
            wrapper.and((obj) -> {
                obj.eq("attr_group_id", key).or().like("attr_group_name", key);
            });
        }

        if (catelogId == 0) {
            IPage<AttrGroupEntity> page = this.page(
                    new Query<AttrGroupEntity>().getPage(params),
                    wrapper);
            return new PageUtils(page);
        } else {
            wrapper.eq("catelog_id", catelogId);
            IPage<AttrGroupEntity> page = this.page(new Query<AttrGroupEntity>().getPage(params), wrapper);
            return new PageUtils(page);
        }
    }

    @Override
    public List<AttrGroupWithAttrsVO> getAttrGroupWithAttrs(Long catelogId) {
        // 1.查询分组
        List<AttrGroupEntity> attrGroupEntities = this.list(new QueryWrapper<AttrGroupEntity>().eq("catelog_id", catelogId));

        // 2.根据分组查询关联属性
        List<AttrGroupWithAttrsVO> collect = attrGroupEntities.stream().map(group -> {
            AttrGroupWithAttrsVO attrsVo = new AttrGroupWithAttrsVO();
            BeanUtils.copyProperties(group, attrsVo);
            List<AttrEntity> attrs = attrService.getRelationAttr(attrsVo.getAttrGroupId());
            attrsVo.setAttrs(attrs);
            return attrsVo;
        }).collect(Collectors.toList());
//        List<AttrGroupWithAttrsVO> results = groupEntities.stream().map(group -> {
//            AttrGroupWithAttrsVO vo = new AttrGroupWithAttrsVO();
//            BeanUtils.copyProperties(group, vo);
//            List<AttrVO> attrs = attrService.getRelationAttr(vo.getAttrGroupId()).stream().map(attrEntity -> {
//                AttrVO AttrVO = new AttrVO();
//                BeanUtils.copyProperties(attrEntity, AttrVO);
//                return AttrVO;
//            }).collect(Collectors.toList());
//
//            vo.setAttrs(attrs);
//            return vo;
//        }).collect(Collectors.toList());
        return collect;
    }

    /**
     * 查询当前spu对应的所有属性的分组信息以及当前分组下的所有属性对应的值
     */
    public List<SpuItemAttrGroupVO> getAttrGroupWithAttrsBySpuId(Long spuId, Long catalogId) {
        // 1.通过spuId查询所有属性值（pms_product_attr_value）
        // 2.通过attrId关联所有属性分组（pms_attr_attrgroup_relation）
        // 3.通过attrGroupId + catalogId关联属性分组名称（pms_attr_group）
        return baseMapper.getAttrGroupWithAttrsBySpuId(spuId, catalogId);
    }

}
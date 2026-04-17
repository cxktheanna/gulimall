package com.atguigu.gulimall.product.service.impl;

import com.atguigu.common.entity.product.AttrEntity;
import com.atguigu.common.entity.product.ProductAttrValueEntity;
import com.atguigu.common.vo.product.BaseAttrs;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimall.product.dao.SpuInfoDescDao;
import com.atguigu.common.entity.product.SpuInfoDescEntity;
import com.atguigu.gulimall.product.service.SpuInfoDescService;
import org.springframework.util.CollectionUtils;


@Service("spuInfoDescService")
public class SpuInfoDescServiceImpl extends ServiceImpl<SpuInfoDescDao, SpuInfoDescEntity> implements SpuInfoDescService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SpuInfoDescEntity> page = this.page(
                new Query<SpuInfoDescEntity>().getPage(params),
                new QueryWrapper<SpuInfoDescEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public void saveSpuInfoDesc(SpuInfoDescEntity descEntity) {
        this.baseMapper.insert(descEntity);
    }

    /**
     * 新增描述图片
     */
    @Override
    public void saveSpuInfoDesc(Long spuId, List<String> decript) {
        SpuInfoDescEntity spuInfoDesc = new SpuInfoDescEntity();
        spuInfoDesc.setSpuId(spuId);
        spuInfoDesc.setDecript(String.join(",", decript));
        this.baseMapper.insert(spuInfoDesc);
    }

}
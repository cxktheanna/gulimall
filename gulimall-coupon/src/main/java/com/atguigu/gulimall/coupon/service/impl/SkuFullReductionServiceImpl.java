package com.atguigu.gulimall.coupon.service.impl;

import com.atguigu.common.to.MemberPrice;
import com.atguigu.common.to.SkuReductionTo;
import com.atguigu.gulimall.coupon.entity.MemberPriceEntity;
import com.atguigu.gulimall.coupon.entity.SkuLadderEntity;
import com.atguigu.gulimall.coupon.service.MemberPriceService;
import com.atguigu.gulimall.coupon.service.SkuLadderService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimall.coupon.dao.SkuFullReductionDao;
import com.atguigu.gulimall.coupon.entity.SkuFullReductionEntity;
import com.atguigu.gulimall.coupon.service.SkuFullReductionService;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;


@Service("skuFullReductionService")
public class SkuFullReductionServiceImpl extends ServiceImpl<SkuFullReductionDao, SkuFullReductionEntity> implements SkuFullReductionService {

    @Autowired
    SkuLadderService skuLadderService;
    @Autowired
    MemberPriceService memberPriceService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SkuFullReductionEntity> page = this.page(
                new Query<SkuFullReductionEntity>().getPage(params),
                new QueryWrapper<SkuFullReductionEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public void saveSkuReduction(SkuReductionTo reductionTo) {
        // 1.sku的打折（买几件打几折）sms_sku_ladder【剔除满减信息为0的】
        if (reductionTo.getFullCount() > 0) {
            SkuLadderEntity skuLadderEntity = new SkuLadderEntity();
            skuLadderEntity.setSkuId(reductionTo.getSkuId());
            skuLadderEntity.setFullCount(reductionTo.getFullCount());
            skuLadderEntity.setDiscount(reductionTo.getDiscount());
            skuLadderEntity.setAddOther(reductionTo.getCountStatus());

            skuLadderService.save(skuLadderEntity);
        }

        // 2.满减信息（满多少减多少）sms_sku_full_reduction【剔除满减信息为0的】
        if (reductionTo.getFullPrice().compareTo(BigDecimal.ZERO) == 1) {
            SkuFullReductionEntity reductionEntity = new SkuFullReductionEntity();
            BeanUtils.copyProperties(reductionTo, reductionEntity);
//            reductionEntity.setAddOther(reductionTo.getPriceStatus());
            this.save(reductionEntity);
        }

        // 3.会员价格：sms_member_price【剔除会员价格为0的数据】
        List<MemberPrice> memberPrice = reductionTo.getMemberPrice();

//        if (!CollectionUtils.isEmpty(memberPrice)) {

        List<MemberPriceEntity> collect = memberPrice.stream()
                .filter(item -> item.getPrice() != null
                        && item.getPrice().compareTo(BigDecimal.ZERO) > 0)
                .map(item -> {
                    MemberPriceEntity priceEntity = new MemberPriceEntity();
                    priceEntity.setSkuId(reductionTo.getSkuId());
                    priceEntity.setMemberLevelId(item.getId());
                    priceEntity.setMemberLevelName(item.getName());
                    priceEntity.setMemberPrice(item.getPrice());
                    priceEntity.setAddOther(1);
                    return priceEntity;
                }).filter(item -> {
                    return item.getMemberPrice().compareTo(new BigDecimal("0")) == 1;
                }).collect(Collectors.toList());

        memberPriceService.saveBatch(collect);
//        }

    }

}
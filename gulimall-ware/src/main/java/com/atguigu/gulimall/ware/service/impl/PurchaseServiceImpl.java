package com.atguigu.gulimall.ware.service.impl;

import com.atguigu.common.constant.ware.PurchaseConstant;
import com.atguigu.gulimall.ware.entity.PurchaseDetailEntity;
import com.atguigu.gulimall.ware.service.PurchaseDetailService;
import com.atguigu.gulimall.ware.vo.MergeVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimall.ware.dao.PurchaseDao;
import com.atguigu.gulimall.ware.entity.PurchaseEntity;
import com.atguigu.gulimall.ware.service.PurchaseService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;


@Service("purchaseService")
public class PurchaseServiceImpl extends ServiceImpl<PurchaseDao, PurchaseEntity> implements PurchaseService {

    @Autowired
    PurchaseDetailService purchaseDetailService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<PurchaseEntity> page = this.page(
                new Query<PurchaseEntity>().getPage(params),
                new QueryWrapper<PurchaseEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public PageUtils queryPageUnreceive(Map<String, Object> params) {
        IPage<PurchaseEntity> page = this.page(
                new Query<PurchaseEntity>().getPage(params),
                new QueryWrapper<PurchaseEntity>().eq("status", PurchaseConstant.PurchaseStatusEnum.CREATED.getCode()).
                        or().eq("status", PurchaseConstant.PurchaseStatusEnum.ASSIGNED.getCode())
        );

        return new PageUtils(page);
    }

    @Transactional
    @Override
    public void mergePurchase(MergeVo mergeVo) {
        Long purchaseId = mergeVo.getPurchaseId();
        if (StringUtils.isEmpty(purchaseId)) {
            // 新增采购单
            PurchaseEntity purchase = new PurchaseEntity();
            purchase.setStatus(PurchaseConstant.PurchaseStatusEnum.CREATED.getCode());
            Date now = new Date();
            purchase.setCreateTime(now);
            purchase.setUpdateTime(now);
            save(purchase);// 保存
            purchaseId = purchase.getId();
        } else {
            // 采购单已存在
            // TODO 校验采购单状态 新建或已分配

        }
        // 整合采购需求
        List<Long> items = mergeVo.getItems();
        Long finalPurchaseId = purchaseId;
        List<PurchaseDetailEntity> collect = items.stream().map(id -> {
            PurchaseDetailEntity detail = new PurchaseDetailEntity();
            detail.setId(id);
            detail.setPurchaseId(finalPurchaseId);
            detail.setStatus(PurchaseConstant.PurchaseDetailStatusEnum.ASSIGNED.getCode());// 分配至采购单
            return detail;
        }).collect(Collectors.toList());
        purchaseDetailService.updateBatchById(collect);

        // 修改更新时间
        PurchaseEntity purchase = new PurchaseEntity();
        purchase.setId(purchaseId);
        purchase.setUpdateTime(new Date());
        updateById(purchase);
    }

}
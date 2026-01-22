package com.atguigu.gulimall.ware.service.impl;

import com.atguigu.common.constant.ware.PurchaseConstant;
import com.atguigu.gulimall.ware.entity.PurchaseDetailEntity;
import com.atguigu.gulimall.ware.service.PurchaseDetailService;
import com.atguigu.gulimall.ware.service.WareSkuService;
import com.atguigu.gulimall.ware.vo.MergeVo;
import com.atguigu.gulimall.ware.vo.PurchaseDoneVo;
import com.atguigu.gulimall.ware.vo.PurchaseItemDoneVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
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
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;


@Service("purchaseService")
public class PurchaseServiceImpl extends ServiceImpl<PurchaseDao, PurchaseEntity> implements PurchaseService {

    @Autowired
    PurchaseDetailService detailService;
    @Autowired
    WareSkuService wareSkuService;


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
        detailService.updateBatchById(collect);

        // 修改更新时间
        PurchaseEntity purchase = new PurchaseEntity();
        purchase.setId(purchaseId);
        purchase.setUpdateTime(new Date());
        updateById(purchase);
    }

    @Override
    public void received(List<Long> ids) {
        // 校验：新建或已分配状态(未领取)
        List<PurchaseEntity> collect = baseMapper.selectBatchIds(ids).stream(
        ).filter(purchase -> PurchaseConstant.PurchaseStatusEnum.CREATED.getCode() == purchase.getStatus() ||
                PurchaseConstant.PurchaseStatusEnum.ASSIGNED.getCode() == purchase.getStatus()
        ).map(purchase -> {
            // 修改采购单状态，已领取
            purchase.setStatus(PurchaseConstant.PurchaseStatusEnum.RECEIVE.getCode());
            return purchase;
        }).collect(Collectors.toList());
        this.updateBatchById(collect);

        // 修改采购需求的状态
        // 查询采购需求
        List<PurchaseDetailEntity> details = new ArrayList<>();
        collect.forEach(purchase -> {
            List<PurchaseDetailEntity> entities = detailService.listDetailByPurchaseId(purchase.getId());
            details.addAll(entities);
        });
        List<PurchaseDetailEntity> detailEntities = details.stream().map(detail -> {
            PurchaseDetailEntity entity = new PurchaseDetailEntity();
            entity.setId(detail.getId());
            entity.setStatus(PurchaseConstant.PurchaseDetailStatusEnum.BUYING.getCode());
            return entity;
        }).collect(Collectors.toList());
        detailService.updateBatchById(detailEntities);
    }

    @Transactional
    @Override
    public void done(PurchaseDoneVo doneVo) {

        Long id = doneVo.getId();

        //2、改变采购项的状态
        Boolean flag = true;
        List<PurchaseItemDoneVo> items = doneVo.getItems();

        List<PurchaseDetailEntity> updates = new ArrayList<>();
        for (PurchaseItemDoneVo item : items) {
            PurchaseDetailEntity detailEntity = new PurchaseDetailEntity();
            if (item.getStatus() == PurchaseConstant.PurchaseDetailStatusEnum.HASERROR.getCode()) {
                flag = false;
                detailEntity.setStatus(item.getStatus());
            } else {
                detailEntity.setStatus(PurchaseConstant.PurchaseDetailStatusEnum.FINISH.getCode());
                ////3、将成功采购的进行入库
                PurchaseDetailEntity entity = detailService.getById(item.getItemId());
                wareSkuService.addStock(entity.getSkuId(), entity.getWareId(), entity.getSkuNum());

            }
            detailEntity.setId(item.getItemId());
            updates.add(detailEntity);
        }

        detailService.updateBatchById(updates);

        //1、改变采购单状态
        PurchaseEntity purchaseEntity = new PurchaseEntity();
        purchaseEntity.setId(id);
        purchaseEntity.setStatus(flag ? PurchaseConstant.PurchaseStatusEnum.FINISH.getCode() : PurchaseConstant.PurchaseStatusEnum.HASERROR.getCode());
        purchaseEntity.setUpdateTime(new Date());
        this.updateById(purchaseEntity);
    }

}
package com.atguigu.gulimall.ware.service.impl;

import com.atguigu.common.constant.ware.WareOrderTaskConstant;
import com.atguigu.common.exception.NoStockException;
import com.atguigu.common.to.mq.StockDetailTO;
import com.atguigu.common.to.mq.StockLockedTO;
import com.atguigu.common.to.ware.SkuHasStockTO;
import com.atguigu.common.to.ware.WareSkuLockTO;
import com.atguigu.common.utils.R;
import com.atguigu.common.vo.ware.OrderItemVO;
import com.atguigu.common.entity.ware.WareOrderTaskDetailEntity;
import com.atguigu.common.entity.ware.WareOrderTaskEntity;
import com.atguigu.gulimall.ware.feign.ProductFeignService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimall.ware.dao.WareSkuDao;
import com.atguigu.common.entity.ware.WareSkuEntity;
import com.atguigu.gulimall.ware.service.WareSkuService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;


@Service("wareSkuService")
public class WareSkuServiceImpl extends ServiceImpl<WareSkuDao, WareSkuEntity> implements WareSkuService {

    @Autowired
    WareSkuDao wareSkuDao;

    @Autowired
    WareOrderTaskServiceImpl orderTaskService;

    @Autowired
    WareOrderTaskDetailServiceImpl orderTaskDetailService;

    @Autowired
    ProductFeignService productFeignService;

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        QueryWrapper<WareSkuEntity> queryWrapper = new QueryWrapper<>();
        String skuId = (String) params.get("skuId");
        if (!StringUtils.isEmpty(skuId)) {
            queryWrapper.eq("sku_id", skuId);
        }

        String wareId = (String) params.get("wareId");
        if (!StringUtils.isEmpty(wareId)) {
            queryWrapper.eq("ware_id", wareId);
        }

        IPage<WareSkuEntity> page = this.page(
                new Query<WareSkuEntity>().getPage(params),
                queryWrapper
        );

        return new PageUtils(page);
    }

    @Override
    public void addStock(Long skuId, Long wareId, Integer skuNum) {
        //1、判断如果还没有这个库存记录新增
        List<WareSkuEntity> entities = wareSkuDao.selectList(new QueryWrapper<WareSkuEntity>().eq("sku_id", skuId).eq("ware_id", wareId));
        if (entities == null || entities.size() == 0) {
            WareSkuEntity skuEntity = new WareSkuEntity();
            skuEntity.setSkuId(skuId);
            skuEntity.setStock(skuNum);
            skuEntity.setWareId(wareId);
            skuEntity.setStockLocked(0);
            //TODO 远程查询sku的名字，如果失败，整个事务无需回滚
            //1、自己catch异常
            //TODO 还可以用什么办法让异常出现以后不回滚？高级
            try {
                R info = productFeignService.info(skuId);
                Map<String, Object> data = (Map<String, Object>) info.get("skuInfo");

                if (info.getCode() == 0) {
                    skuEntity.setSkuName((String) data.get("skuName"));
                }
            } catch (Exception e) {

            }


            wareSkuDao.insert(skuEntity);
        } else {
            wareSkuDao.addStock(skuId, wareId, skuNum);
        }
    }

    /**
     * 查询sku是否有库存
     */
    @Override
    public List<SkuHasStockTO> getSkusHasStock(List<Long> skuIds) {
        return skuIds.stream().map(skuId -> {
            SkuHasStockTO stock = new SkuHasStockTO();
            // 查询当前sku总库存量
            Long count = baseMapper.getSkusStock(skuId);
            stock.setSkuId(skuId);
            stock.setHasStock(count == null ? false : count > 0);
            return stock;
        }).collect(Collectors.toList());
    }

    @Transactional
    @Override
    public Boolean orderLockStock(WareSkuLockTO lockTO) {
        // 按照收货地址找到就近仓库，锁定库存（暂未实现）
        // 采用方案：获取每项商品在哪些仓库有库存，轮询尝试锁定，任一商品锁定失败回滚

        // 1.往库存工作单存储当前锁定（本地事务表）
        WareOrderTaskEntity taskEntity = new WareOrderTaskEntity();
        taskEntity.setOrderSn(lockTO.getOrderSn());
        orderTaskService.save(taskEntity);

        // 2.封装待锁定库存项Map
        Map<Long, OrderItemVO> lockItemMap = lockTO.getLocks().stream().collect(Collectors.toMap(key -> key.getSkuId(), val -> val));
        // 3.查询（库存 - 库存锁定 >= 待锁定库存数）的仓库
        List<WareSkuEntity> wareEntities = baseMapper.selectListHasSkuStock(lockItemMap.keySet()).stream().filter(entity -> entity.getStock() - entity.getStockLocked() >= lockItemMap.get(entity.getSkuId()).getCount()).collect(Collectors.toList());
        // 判断是否查询到仓库
        if (CollectionUtils.isEmpty(wareEntities)) {
            // 匹配失败，所有商品项没有库存
            Set<Long> skuIds = lockItemMap.keySet();
            throw new NoStockException(skuIds);
        }
        // 将查询出的仓库数据封装成Map，key:skuId  val:wareId
        Map<Long, List<WareSkuEntity>> wareMap = wareEntities.stream().collect(Collectors.groupingBy(key -> key.getSkuId()));
        // 4.判断是否为每一个商品项至少匹配了一个仓库
        List<WareOrderTaskDetailEntity> taskDetails = new ArrayList<>();// 库存锁定工作单详情
        Map<Long, StockLockedTO> lockedMessageMap = new HashMap<>();// 库存锁定工作单消息
        if (wareMap.size() < lockTO.getLocks().size()) {
            // 匹配失败，部分商品没有库存
            Set<Long> skuIds = lockItemMap.keySet();
            skuIds.removeAll(wareMap.keySet());// 求商品项差集
            throw new NoStockException(skuIds);
        } else {
            // 所有商品都存在有库存的仓库
            // 5.锁定库存
            for (Map.Entry<Long, List<WareSkuEntity>> entry : wareMap.entrySet()) {
                Boolean skuStocked = false;
                Long skuId = entry.getKey();// 商品
                OrderItemVO item = lockItemMap.get(skuId);
                Integer count = item.getCount();// 待锁定个数
                List<WareSkuEntity> hasStockWares = entry.getValue();// 有足够库存的仓库
                for (WareSkuEntity ware : hasStockWares) {
                    Long num = baseMapper.lockSkuStock(skuId, ware.getWareId(), count);
                    if (num == 1) {
                        // 锁定成功，跳出循环
                        skuStocked = true;
                        // 创建库存锁定工作单详情（每一件商品锁定详情）
                        WareOrderTaskDetailEntity taskDetail = new WareOrderTaskDetailEntity(null, skuId,
                                item.getTitle(), count, taskEntity.getId(), ware.getWareId(),
                                WareOrderTaskConstant.LockStatusEnum.LOCKED.getCode());
                        taskDetails.add(taskDetail);
                        // 创建库存锁定工作单消息（每一件商品一条消息）
                        StockDetailTO detailMessage = new StockDetailTO();
                        BeanUtils.copyProperties(taskDetail, detailMessage);
                        StockLockedTO lockedMessage = new StockLockedTO(taskEntity.getId(), detailMessage);
                        lockedMessageMap.put(skuId, lockedMessage);
                        break;
                    }
                }
                if (!skuStocked) {
                    // 匹配失败，当前商品所有仓库都未锁定成功
                    throw new NoStockException(skuId);
                }
            }
        }

        // 6.往库存工作单详情存储当前锁定（本地事务表）
        orderTaskDetailService.saveBatch(taskDetails);

        // 7.发送消息
        for (WareOrderTaskDetailEntity taskDetail : taskDetails) {
            StockLockedTO message = lockedMessageMap.get(taskDetail.getSkuId());
            message.getDetail().setId(taskDetail.getId());// 存储库存详情ID
            rabbitTemplate.convertAndSend("stock-event-exchange", "stock.locked", message);
        }
        return true;
    }



}
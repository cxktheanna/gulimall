package com.atguigu.gulimall.ware.vo;

import lombok.Data;

@Data
public class PurchaseItemDoneVo {
    private Long itemId;// 采购需求ID

    private Integer status;// 采购状态

    private String reason;// 原因
}

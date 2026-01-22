package com.atguigu.gulimall.ware.vo;

import lombok.Data;

import java.util.List;

@Data
public class MergeVo {
    private Long purchaseId;// 采购单ID
    private List<Long> items;// 采购需求ID
}

package org.atguigu.gulimall.cart.service;

import com.atguigu.common.vo.cart.CartItemVO;
import com.atguigu.common.vo.cart.CartVO;

import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * 购物车
 * @Author: wanzenghui
 * @Date: 2021/12/4 23:53
 */
public interface CartService {

    /**
     * 将商品添加至购物车
     * @param skuId
     * @param num
     * @return
     */
    CartItemVO addToCart(Long skuId, Integer num) throws ExecutionException, InterruptedException;

    /**
     * 获取购物车某个购物项
     * @param skuId
     * @return
     */
    CartItemVO getCartItem(Long skuId);

    /**
     * 获取购物车里面的信息
     * @return
     */
    CartVO getCart() throws ExecutionException, InterruptedException;

    /**
     * 清空购物车的数据
     * @param cartKey
     */
    public void clearCartInfo(String cartKey);

    /**
     * 勾选购物项
     * @param skuId
     * @param check
     */
    void checkItem(Long skuId, Integer check);

    /**
     * 改变商品数量
     * @param skuId
     * @param num
     */
    void changeItemCount(Long skuId, Integer num);


    /**
     * 删除购物项
     * @param skuId
     */
    void deleteIdCartInfo(Integer skuId);

    List<CartItemVO> getUserCartItems();
}
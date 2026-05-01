package org.atguigu.gulimall.seckill.scheduled;

import com.atguigu.common.constant.seckill.SeckillConstant;
import lombok.extern.slf4j.Slf4j;
import org.atguigu.gulimall.seckill.service.SeckillService;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 定时任务
 * @Description:
 * @Created: with IntelliJ IDEA.
 * @author: wanzenghui
 * @createTime: 2020-07-09 19:22
 */
@Slf4j
@Service
public class SeckillScheduled {

    @Autowired
    SeckillService seckillService;
    @Autowired
    RedissonClient redissonClient;

    /**
     * 秒杀商品定时上架，保证幂等性问题
     *  每天晚上3点，上架最近三天需要秒杀的商品
     *  当天00:00:00 - 23:59:59
     *  明天00:00:00 - 23:59:59
     *  后天00:00:00 - 23:59:59
     */
    @Scheduled(cron = "*/10 * * * * ? ")
    //@Scheduled(cron = "0 0 3 * * ? ")
    public void uploadSeckillSkuLatest3Days() {
        log.info("====== 定时任务开始执行 ======");

        RLock lock = redissonClient.getLock(SeckillConstant.UPLOAD_LOCK);
        try {
            lock.lock(10, TimeUnit.SECONDS);
            log.info("====== 分布式锁加锁成功 ======");

            // ++++++++++++++++++++++ 这里加一句
            log.info("====== 开始调用秒杀上架业务方法 ======");

            seckillService.uploadSeckillSkuLatest3Days();

            // ++++++++++++++++++++++ 这里加一句
            log.info("====== 秒杀商品上架完成 ======");

        } catch (Exception e) {
            // ++++++++++++++++++++++ 这里改一下
            log.error("====== 上架方法执行失败！！！======", e);
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }
}

package com.atguigu.gulimall.product.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class MyRedissonConfig {


    @Bean(destroyMethod = "shutdown")
    public RedissonClient redisson() throws IOException {
        String host = "10.234.115.63";
        String port = "6379";
        // 1.创建配置
        Config config = new Config();
        config.useSingleServer().setAddress("redis://" + host + ":" + port);// 单节点模式
//        config.useSingleServer().setAddress("rediss://" + host + ":" + port);// 使用安全连接
//        config.useClusterServers().addNodeAddress("127.0.0.1:7004", "127.0.0.1:7001");// 集群模式
        // 2.创建redisson客户端实例
        RedissonClient redissonClient = Redisson.create(config);
        return redissonClient;
    }


}

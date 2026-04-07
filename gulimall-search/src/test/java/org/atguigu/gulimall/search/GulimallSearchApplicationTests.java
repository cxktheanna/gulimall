package org.atguigu.gulimall.search;

import com.alibaba.fastjson.JSON;
import lombok.Data;
import org.atguigu.gulimall.search.config.GulimallElasticSearchConfig;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;

@RunWith(SpringRunner.class)
@SpringBootTest
class GulimallSearchApplicationTests {

    @Autowired
    private RestHighLevelClient client;


    /**
     * 保存/更新索引
     */
    @Test
    void indexData() throws IOException {
        // 1、构建创建或更新请求，指定索引users
        IndexRequest indexRequest = new IndexRequest( "users");
        // 2、设置id
        indexRequest.id("1");// 数据的id
        // 方式一：直接设置数据项
        //users.source("userName", "zhangsan", "gender", "M", "age", "18");
        User user = new User("lisi", "M", 22);
        // 3、绑定数据与请求
        // 方式二：设置json串格式数据
        indexRequest.source(JSON.toJSONString(user), XContentType.JSON);
        // 4、执行：同步
        IndexResponse index = client.index(indexRequest, GulimallElasticSearchConfig.COMMON_OPTIONS);
        // 5、提取响应数据
        System.out.println(index);
    }

    @Data
    class User{
        private String userName;
        private String gender;
        private Integer age;

        public User(String userName, String gender, Integer age) {
            this.userName = userName;
            this.gender = gender;
            this.age = age;
        }
    }


    @Test
    public void contextLoads() {
        System.out.println(client);
    }

}

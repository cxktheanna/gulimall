package com.atguigu.gulimall.thirdparty;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSSClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

@SpringBootTest
class GulimallThirdPartyApplicationTests {

    @Test
    void contextLoads() {
    }

    @Autowired
    OSSClient ossClient;

    @Test
    public void testUpload() throws FileNotFoundException, ClientException {
        // Endpoint以华东1（杭州）为例，其它Region请按实际情况填写。

        InputStream inputStream = new FileInputStream("C:\\Users\\咳咳\\Desktop\\aa45c383e4f3ff222a73f70eaae98f2f1265652806.jpg");

        ossClient.putObject("gulimall-hermit", "test3.jpg", inputStream);

        ossClient.shutdown();

        System.out.println("上传完成...");

    }
}

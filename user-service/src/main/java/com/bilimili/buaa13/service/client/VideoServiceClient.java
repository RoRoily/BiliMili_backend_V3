package com.bilimili.buaa13.service.client;

import com.bilimili.buaa13.entity.Video;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

//服务名称和url
//用于接收video微服务模块传输的对象
//自动将调用路由到'video-service'
@FeignClient(name = "videoService", url = "http://videoService:9090")
public interface VideoServiceClient {

    //从videoService中寻找提供的服务
    @GetMapping("/video/{vid}")
    Video getUserBtId(@PathVariable("vid") Integer vid);
}

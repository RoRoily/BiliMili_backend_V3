package com.bilimili.buaa13.service.client;

import com.bilimili.buaa13.entity.Video;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "videoService", url = "http://videoService:9090")
public interface VideoArticleClient {
    @GetMapping("/video/get")
    Video getVideo(@RequestParam("vid") Integer vid);
}

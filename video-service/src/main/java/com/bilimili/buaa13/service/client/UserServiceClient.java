package com.bilimili.buaa13.service.client;

import com.bilimili.buaa13.entity.User;
import com.bilimili.buaa13.entity.UserVideo;
import com.bilimili.buaa13.entity.Video;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

//服务名称和url
//用于接收video微服务模块传输的对象
//自动将调用路由到'video-service'
@FeignClient(name = "userService", url = "http://userService:9090")
public interface UserServiceClient {

    //从userService中寻找提供的服务
    @GetMapping("/user/{uid}")
    User getUserBtId(@PathVariable("uid") Integer uid);

    @PostMapping("/user/updateUserVideo")
    ResponseEntity<String> updateUserVideo(@RequestBody UserVideo userVideo);
}

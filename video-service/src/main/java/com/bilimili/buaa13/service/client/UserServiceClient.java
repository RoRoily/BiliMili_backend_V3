package com.bilimili.buaa13.service.client;

import com.bilimili.buaa13.entity.UserVideo;
import com.bilimili.buaa13.entity.dto.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import java.util.*;

//服务名称和url
//用于接收video微服务模块传输的对象
//自动将调用路由到'video-service'
@FeignClient(name = "userService", url = "http://userService:9090")
public interface UserServiceClient {

    //从userService中寻找提供的服务
    @GetMapping("/user/{uid}")
    UserDTO getUserById(@PathVariable("uid") Integer uid);

    @PostMapping("/user/updateUserVideo")
    ResponseEntity<String> updateUserVideo(@RequestBody UserVideo userVideo);

    @PostMapping("/user/currentUser/getId")
    Integer getCurrentUserId();

    @PostMapping("/user/currentUser/isAdmin")
    Boolean currentIsAdmin();

    //直接调用mapper中的方法即可
    @GetMapping("/user/getVidsByFid/{fid}")
    List<Integer> getVidsByFid(@PathVariable("fid") Integer fid);

    @GetMapping("/user/getTimeByFid/{fid}")
    List<Date> getTimeByFid(@PathVariable("fid") Integer fid);
}

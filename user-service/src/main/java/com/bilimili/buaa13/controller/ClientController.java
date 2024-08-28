package com.bilimili.buaa13.controller;



import com.bilimili.buaa13.entity.UserVideo;
import com.bilimili.buaa13.entity.dto.UserDTO;
import com.bilimili.buaa13.service.user.UserService;
import com.bilimili.buaa13.service.utils.CurrentUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Map;


/**
 * 来自user-service模块，向video-service提供被需求的模块
 *
 * **/
@Service
@RestController
@RequestMapping("/user")
public class ClientController{


    @Autowired
    private UserService userService;

    @Autowired
    private CurrentUser currentUser;
    // 获取用户详情

    @GetMapping("/{uid}")
    public UserDTO getUserById(@PathVariable("uid") Integer uid) {
        // 假设 UserDTO 是从 User 实体转换而来的
        return userService.getUserByUId(uid);
    }

    // 更新用户的视频信息
    @PostMapping("/updateUserVideo")
    public ResponseEntity<String> updateUserVideo(@RequestBody UserVideo userVideo) {
        // 执行相应的更新逻辑
        // 如保存到数据库，更新用户观看记录等
        // 此处假设是更新操作
        userRepository.updateUserVideo(userVideo);
        return ResponseEntity.ok("User video updated successfully");
    }

    // 获取当前用户的ID
    @PostMapping("/currentUser/getId")
    public Integer getCurrentUserId() {
        // 假设此处通过某种方式获取当前用户
        return currentUser.getUserId();
    }

    // 判断当前用户是否为管理员
    @PostMapping("/currentUser/isAdmin")
    public Boolean currentIsAdmin() {
        // 假设此处通过用户ID或角色判断是否是管理员
        return currentUser.isAdmin();
    }

    // 更新收藏的视频
    @PostMapping("/updateFavoriteVideo")
    public ResponseEntity<Void> updateFavoriteVideo(@RequestBody List<Map<String, Object>> result, @RequestParam("fid") Integer fid) {
        // 执行相应的更新收藏视频逻辑
        userRepository.updateFavoriteVideos(fid, result);
        return ResponseEntity.ok().build();
    }

    // 处理评论
    @PostMapping("/handle_comment")
    public void handleComment(@RequestParam("uid") Integer uid,
                              @RequestParam("toUid") Integer toUid,
                              @RequestParam("id") Integer id) {
        // 执行评论处理逻辑
        userRepository.handleComment(uid, toUid, id);
    }

    // 通过收藏ID获取视频ID列表
    @GetMapping("/getVidsByFid/{fid}")
    public List<Integer> getVidsByFid(@PathVariable("fid") Integer fid) {
        return userRepository.getVideoIdsByFavoriteId(fid);
    }

    // 通过收藏ID获取时间列表
    @GetMapping("/getTimeByFid/{fid}")
    public List<Date> getTimeByFid(@PathVariable("fid") Integer fid) {
        return userRepository.getTimesByFavoriteId(fid);
    }

    // 获取当前登录用户ID的模拟方法（实际可以通过Spring Security或Token获取）
    private Integer getCurrentAuthenticatedUserId() {
        // 模拟返回某个用户ID，实际情况会有所不同
        return 123;  // 示例用户ID
    }
}

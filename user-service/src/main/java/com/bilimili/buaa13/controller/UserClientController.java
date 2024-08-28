package com.bilimili.buaa13.controller;

import com.bilimili.buaa13.entity.dto.UserDTO;
import com.bilimili.buaa13.service.user.UserService;
import com.bilimili.buaa13.service.utils.CurrentUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("/user")
public class UserClientController {

    @Autowired
    private CurrentUser currentUser;
    @Autowired
    private UserService userService;
    //从userService中寻找提供的服务
    @GetMapping("/{uid}")
    public UserDTO getUserById(@PathVariable("uid") Integer uid){
        return userService.getUserByUId(uid);
    }

    @PostMapping("/currentUser/getId")
    public Integer getCurrentUserId(){
        return currentUser.getUserId();
    }

    @PostMapping("/currentUser/isAdmin")
    public Boolean currentIsAdmin(){
        return currentUser.isAdmin();
    }
}

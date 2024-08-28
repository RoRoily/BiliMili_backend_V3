package com.bilimili.buaa13.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.bilimili.buaa13.entity.*;
import com.bilimili.buaa13.entity.dto.UserDTO;
import com.bilimili.buaa13.im.IMServer;
import com.bilimili.buaa13.mapper.FavoriteMapper;
import com.bilimili.buaa13.mapper.FavoriteVideoMapper;
import com.bilimili.buaa13.service.favorite.FavoriteVideoService;
import com.bilimili.buaa13.service.message.MessageUnreadService;
import com.bilimili.buaa13.service.user.UserService;
import com.bilimili.buaa13.service.utils.CurrentUser;
import com.bilimili.buaa13.tools.RedisTool;
import io.netty.channel.Channel;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController("/user")
public class UserClientController {

    @Autowired
    private CurrentUser currentUser;
    @Autowired
    private UserService userService;
    @Autowired
    private SqlSessionFactory sqlSessionFactory;

    @Autowired
    private FavoriteVideoMapper favoriteVideoMapper;
    @Autowired
    private RedisTool redisTool;
    @Autowired
    private IMServer imServer;
    @Autowired
    private MessageUnreadService messageUnreadService;
    @Autowired
    private FavoriteMapper favoriteMapper;
    @Autowired
    private FavoriteVideoService favoriteVideoService;

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


    @PostMapping("/updateFavoriteVideo")
    public ResponseEntity<Void> updateFavoriteVideo(@RequestBody List<Map<String, Object>> result, @RequestParam("fid") Integer fid) {
        try (SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.BATCH)) {
            result.stream().parallel().forEach(map -> {
                Video video = (Video) map.get("video");
                QueryWrapper<FavoriteVideo> queryWrapper = new QueryWrapper<>();
                queryWrapper.eq("vid", video.getVid()).eq("fid", fid);
                map.put("info", favoriteVideoMapper.selectOne(queryWrapper));
            });
            sqlSession.commit();
        }
        return ResponseEntity.ok().build();
    }

    @PostMapping("/handle_comment")
    public void handleComment(@RequestParam("uid") Integer uid,
                              @RequestParam("toUid") Integer toUid,
                              @RequestParam("id") Integer id){

        if(!toUid.equals(uid)) {
            //1注释Redis
            redisTool.storeZSet("reply_zset:" + toUid, id);
            messageUnreadService.addOneUnread(toUid, "reply");

            // 通知未读消息
            Map<String, Object> map = new HashMap<>();
            map.put("type", "接收");
            Set<Channel> commentChannel = IMServer.userChannel.get(toUid);
            if (commentChannel != null) {
                commentChannel.stream().parallel().forEach(channel -> channel.writeAndFlush(IMResponse.message("reply", map)));
            }
        }
    }

    @PostMapping("/set/favorite")
    ResponseResult setFavorite(@RequestParam("fid") Integer fid ,
                               @RequestParam("vid") String vids){
        ResponseResult responseResult = new ResponseResult();
        List<Integer> videoList = new ArrayList<>();
        String[] videos = vids.split(",");
        for (String s : videos) {
            try {
                videoList.add(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                // 处理可能的转换异常
                System.err.println("Invalid number format: " + s);
            }
        }
        Integer uid = fid;
        QueryWrapper<Favorite> favoriteQueryWrapper = new QueryWrapper<>();
        favoriteQueryWrapper.eq("fid", uid).eq("type", 1);
        Favorite favorite = favoriteMapper.selectOne(favoriteQueryWrapper);
        if(favorite == null){
            responseResult.setCode(404);
            responseResult.setMessage("Favorite not found");
            return responseResult;
        }
        Set<Integer>addSet = new HashSet<>();
        List<Integer> collectedVid = favoriteVideoMapper.getVidByFid(fid);
        addSet.add(fid);
        int flag = 0;
        for(Integer vid:videoList){
            if(collectedVid.contains(vid)){
                flag++;
            }
            else favoriteVideoService.addToFav(uid,vid,addSet);
        }
        if(flag >= videoList.size()){
            responseResult.setData(true);
        }
        else {
            responseResult.setData(false);
        }
        return responseResult;
    }
}

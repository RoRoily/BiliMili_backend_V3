package com.bilimili.buaa13.service.impl.user;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.bilimili.buaa13.entity.Follow;
import com.bilimili.buaa13.entity.UserRecord;
import com.bilimili.buaa13.entity.UserRecordString;
import com.bilimili.buaa13.mapper.FollowMapper;
import com.bilimili.buaa13.mapper.UserRecordStringMapper;
import com.bilimili.buaa13.service.record.UserRecordService;
import com.bilimili.buaa13.service.user.FollowService;
import com.bilimili.buaa13.tools.RedisTool;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Service
public class FollowServiceImpl implements FollowService {
    @Autowired
    private FollowMapper followMapper;
    @Autowired
    private RedisTool redisTool;
    @Autowired
    @Qualifier("taskExecutor")
    private Executor taskExecutor;//同步或异步执行器

    @Autowired
    private UserRecordStringMapper userRecordStringMapper;
    @Autowired
    private UserRecordService userRecordService;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 根据是否用户本人获取全部可见的关注列表
     * @param uid   用户ID
     * @param isOwner  是否用户本人
     * @return  关注列表
     */
    @Override
    public List<Integer> getUidFollow(Integer uid, boolean isOwner){
        String key = "fans:" + uid;   // uid用户的关注列表，所以uid用户是粉丝
        Set<Object>following = redisTemplate.opsForZSet().range(key, 0, -1);
        List<Integer> list = null;
        if (following != null) {
            list = following.stream().map(obj -> Integer.parseInt(obj.toString())).collect(Collectors.toList());
        }
        else return Collections.emptyList();
        if (!list.isEmpty())   {
            if (!isOwner) {
                List<Integer> list1 = new ArrayList<>();
                for (Integer follow : list) {
                    if(true/** 以后实现拉黑用**/){
                        list1.add(follow);
                    }
                }
                return list1;
            }
            return list;
        }
        list = followMapper.getUidFollowByUid(uid);

        List<Integer> finalList = list;
        if(list!=null&& !list.isEmpty()){
            CompletableFuture.runAsync(() -> {
                redisTool.setExObjectValue(key, finalList);
            }, taskExecutor);
            return list;
        }
        return Collections.emptyList();
    }
    /**
     * 根据是否用户本人获取全部可见的粉丝列表
     * @param uid   用户ID
     * @param isOwner  是否用户本人
     * @return  关注列表
     */
    @Override
    public List<Integer> getUidFans(Integer uid, boolean isOwner){
        List<Integer>list = followMapper.getUidFansByUid(uid);
        if(list!=null&& !list.isEmpty()){
            return list;
        }
        return Collections.emptyList();
    }
    @Override
    public List<Integer> getUidFans(Integer uid){
        QueryWrapper<Follow> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("uid_follow", uid);
        List<Follow> followList = followMapper.selectList(queryWrapper);
        // 创建一个 integer 的 list，用于存储查询结果中的 id 值
        List<Integer> idList = new ArrayList<>();
        // 遍历查询结果集，将 id 值添加到 list 中
        for (Follow follow : followList) {
            idList.add(follow.getUidFans());
        }
        // 返回结果
        return idList;
    }

    /**
     * 关注用户
     * @param uidFollow   关注者ID
     * @param uidFans   粉丝ID
     * 关注者id对应的用户，有一个粉丝ID
     * 粉丝id对应的用户，有一个关注ID
     */
    @Override
    public void addFollow(Integer uidFollow, Integer uidFans) throws JsonProcessingException {
        if(uidFollow.equals(uidFans)){return;}
        Follow newFollow = new Follow(uidFollow,uidFans,1);
        QueryWrapper<Follow> followQueryWrapper = new QueryWrapper<>();
        followQueryWrapper.eq("uid_follow", uidFollow).eq("uid_fans", uidFans);
        Follow follow = followMapper.selectOne(followQueryWrapper);
        if(follow!=null) return;
        followMapper.insert(newFollow);
        String key = "follow:" + uidFollow;
        redisTool.storeZSet(key,uidFans);
        String key2 = "fans:" + uidFans;
        redisTool.storeZSet(key2,uidFollow);
        String key3 = "userRecord:" + uidFollow;
        UserRecord userRecord = null;
        Set<Object> userRecordSet = redisTemplate.opsForZSet().range(key3, 0, 0);
        if(userRecordSet!=null&& !userRecordSet.isEmpty()){
            userRecord = (UserRecord) userRecordSet.iterator().next();
            redisTool.deleteZSetMember(key3,userRecord);//注意这里
        }
        else{
            QueryWrapper<UserRecordString> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("uid",uidFollow);
            UserRecordString userRecordString = userRecordStringMapper.selectOne(queryWrapper);
            if(userRecordString!=null){
                userRecord = userRecordService.findUserRecordByString(userRecordString);
            }
        }
        if (userRecord != null) {
            userRecord.setFansNew(userRecord.getFansNew()+1);
            redisTool.storeZSet(key3,userRecord);
            UserRecordString userRecordString = userRecordService.saveUserRecordToString(userRecord);
            userRecordService.saveUserRecordStringToDatabase(userRecordString);
        }

    }
    /**
     * 取关用户
     * @param uidFollow   关注者ID
     * @param uidFans   被关注者ID
     */
    @Override
    public void delFollow(Integer uidFollow, Integer uidFans) throws JsonProcessingException {
        QueryWrapper<Follow> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("uid_follow", uidFollow).eq("uid_fans", uidFans);
        Follow follow = followMapper.selectOne(queryWrapper);
        if(follow==null) return;
        else followMapper.delete(queryWrapper);
        UserRecord userRecord = null;
        QueryWrapper<UserRecordString> queryWrapper1 = new QueryWrapper<>();
        queryWrapper.eq("uid",uidFollow);
        UserRecordString userRecordString = userRecordStringMapper.selectList(queryWrapper1).get(0);
        if(userRecordString!=null){
            userRecord = userRecordService.findUserRecordByString(userRecordString);
        }
        if (userRecord != null) {
            userRecord.setFansNew(userRecord.getFansNew()-1);
            //redisUtil.storeZSet(key3,userRecord);
            userRecordString = userRecordService.saveUserRecordToString(userRecord);
            userRecordService.saveUserRecordStringToDatabase(userRecordString);
        }
    }
    /**
     * 更新其他人是否可以查看关注列表
     * @param uid   自己的ID
     * @param visible   能否查看,1可以，0不可以
     */
    @Override
    public void updateVisible(Integer uid, Integer visible){
        UpdateWrapper<Follow> followUpdateWrapper = new UpdateWrapper<>();
        followUpdateWrapper.eq("uid", uid);
        followUpdateWrapper.set("visible", visible);
    }

    /**
     * 检查该用户是否被关注
     * @param uidFollow   关注者ID
     * @param uidFans   粉丝ID
     * 关注者id对应的用户，有一个粉丝ID
     * 粉丝id对应的用户，有一个关注ID
     */
    @Override
    public boolean isHisFans(Integer uidFollow,Integer uidFans){
        QueryWrapper<Follow> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("uidFollow", uidFollow).eq("uidFans", uidFans);
        Follow follow = followMapper.selectOne(queryWrapper);
        return follow != null;
    }
}


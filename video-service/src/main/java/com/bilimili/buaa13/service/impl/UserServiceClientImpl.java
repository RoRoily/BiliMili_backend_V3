package com.bilimili.buaa13.service.impl;

@Service
public class VideoServiceImpl implements VideoService {

    @Autowired
    private UserServiceFeignClient userServiceFeignClient;

    @Override
    public UserVideo updatePlay(Integer uid, Integer vid) {
        QueryWrapper<UserVideo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("uid", uid).eq("vid", vid);
        UserVideo userVideo = userVideoMapper.selectOne(queryWrapper);
        if (userVideo == null) {
            // 记录不存在，创建新记录
            userVideo = new UserVideo(null, uid, vid, 1, 0, 0, 0, 0, new Date(), null, null);
            userVideoMapper.insert(userVideo);
        } else if (System.currentTimeMillis() - userVideo.getPlayTime().getTime() <= 30000) {
            // 如果最近30秒内播放过则不更新记录，直接返回
            return userVideo;
        } else {
            userVideo.setPlay(userVideo.getPlay() + 1);
            userVideo.setPlayTime(new Date());
            userVideoMapper.updateById(userVideo);
        }

        // 异步线程更新video表和redis，同时通知user-service
        CompletableFuture.runAsync(() -> {
            redisTool.storeZSet("user_video_history:" + uid, vid);   // 添加到/更新观看历史记录
            videoStatusService.updateVideoStatus(vid, "play", true, 1);

            // 通知 user-service 更新用户视频记录
            userServiceFeignClient.updateUserVideo(userVideo);
        }, taskExecutor);

        return userVideo;
    }
}


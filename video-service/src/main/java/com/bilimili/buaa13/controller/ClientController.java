package com.bilimili.buaa13.controller;


import com.bilimili.buaa13.entity.ResponseResult;
import com.bilimili.buaa13.entity.Video;
import com.bilimili.buaa13.entity.VideoStatus;
import com.bilimili.buaa13.mapper.VideoMapper;
import com.bilimili.buaa13.service.video.VideoStatusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

/**
 * 来自video-service模块，向user-service提供被需求服务
 *
 *
 * **/
@Service
@RestController
@RequestMapping("/video")
public class ClientController {

    @Autowired
    private VideoMapper videoMapper;

    @Autowired
    private VideoStatusService videoStatusService;


    // 通过视频ID获取视频详情
    @GetMapping("/{vid}")
    public ResponseResult getVideoById(@PathVariable("vid") Integer vid) {
        Video video = videoMapper.selectById(vid);
        if (video != null) {
            return new ResponseResult(200, "OK", video);
        } else {
            return new ResponseResult(404, "Video not found", null);
        }
    }

    // 通过视频ID获取视频状态
    @GetMapping("/videoStatus/{vid}")
    public VideoStatus getVideoStatusById(@PathVariable("vid") Integer vid) {
        return videoStatusService.getStatusByVideoId(vid);
    }

    // 更新视频状态
    @PostMapping("/updateStatus")
    public ResponseResult updateVideoStatus(@RequestParam("vid") Integer vid,
                                            @RequestParam("statusType") String statusType,
                                            @RequestParam("increment") Boolean increment,
                                            @RequestParam("count") Integer count) {

        try {
            videoStatusService.updateVideoStatus(vid, statusType, increment, count);
            return new ResponseResult(200, "update video status success", null);
        } catch (Exception e) {
            return new ResponseResult(500, "update video status failed", null);
        }
    }

    // 更新视频的点赞或差评数
    @PostMapping("/updateGoodAndBad")
    public ResponseResult updateGoodAndBad(@RequestParam("vid") Integer vid,
                                           @RequestParam("addGood") Boolean addGood) {
        try {
            videoStatusService.updateGoodAndBad(vid, addGood);
            return new ResponseResult(200, "update video status success", null);
        } catch (Exception e) {
            return new ResponseResult(500, "update video status failed", null);
        }
    }
}

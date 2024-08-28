package com.bilimili.buaa13.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.bilimili.buaa13.entity.Video;
import com.bilimili.buaa13.mapper.VideoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController("/video")
public class ArticleClientController {
    @Autowired
    private VideoMapper videoMapper;
    @GetMapping("/video/get")
    public Video getVideo(@RequestParam("vid") Integer vid){
        QueryWrapper<Video> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("vid", vid).ne("status", 3);
        //Viode = articleMapper.selectOne(queryWrapper);
        return videoMapper.selectOne(queryWrapper);
    }
}

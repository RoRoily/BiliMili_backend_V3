package com.bilimili.buaa13.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.bilimili.buaa13.entity.Article;
import com.bilimili.buaa13.entity.ResponseResult;
import com.bilimili.buaa13.entity.Video;
import com.bilimili.buaa13.entity.dto.ArticleUploadDTO;
import com.bilimili.buaa13.mapper.ArticleMapper;
import com.bilimili.buaa13.service.article.ArticleUploadService;
import com.bilimili.buaa13.service.client.UserArticleClient;
import com.bilimili.buaa13.service.client.VideoArticleClient;
import com.bilimili.buaa13.tools.OssTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@RestController
public class ArticleUploadController {
    @Autowired
    private ArticleUploadService articleUploadService;
    @Autowired
    private OssTool ossTool;

    @Autowired
    private UserArticleClient userArticleClient;
    @Autowired
    private ArticleMapper articleMapper;
    @Autowired
    private VideoArticleClient videoArticleClient;



    @PostMapping("/image/add")
    @Transactional
    public ResponseResult addImage(
            @RequestParam("image") MultipartFile image

    ){
        try {
            String url = ossTool.uploadImage(image,"articleArtwork");
            System.out.println(url);
            return new ResponseResult(200,"图片上传成功",url);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseResult(500, "封面上传失败", null);
        }

    }

    /**
     * 添加文章投稿
     * @param title 投稿标题
     * @param cover  文章封面
     * @param title 关联的视频内容
     * @param content  文章内容的markdown文件
     * @return  响应对象
     */
    @PostMapping("/article/add/all")
    public ResponseResult addAllArticle(
            @RequestParam("cover") MultipartFile cover,
            @RequestParam("title") String title,
            @RequestParam("vid") String vid,
            @RequestParam("content") MultipartFile content
    ) {
        ArticleUploadDTO articleUploadDTO = new ArticleUploadDTO(null, content);
        try {
            List<Integer> videoList = new ArrayList<>();
            String[] videos = vid.split(",");
            for (String s : videos) {
                try {
                    videoList.add(Integer.parseInt(s));
                } catch (NumberFormatException e) {
                    // 处理可能的转换异常
                    System.err.println("Invalid number format: " + s);
                    return new ResponseResult(200,"关联视频的格式错误",null);
                }
            }
            for(Integer vid1:videoList){
                Video video = videoArticleClient.getVideo(vid1);
                if(video == null){
                    return new ResponseResult(200,"关联视频列表中包含不存在的视频",null);
                }
            }

            //return articleUploadService.addArticle(articleUploadDTO);
            String url = ossTool.uploadArticle(content);
            String url2 = ossTool.uploadImage(cover,"articleCover");
            Article article = new Article();
            article.setTitle(title);
            article.setVid(vid);
            article.setContentUrl(url);
            article.setCoverUrl(url2);
            article.setStatus(0);
            article.setUid(userArticleClient.getCurrentUserId()); // 假设 currentUser 对象可以获取当前用户的 ID
            articleMapper.insert(article);
            return new ResponseResult(200,"文章上传成功",article.getAid().toString());
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseResult(500, "封面上传失败", null);
        }
    }
}

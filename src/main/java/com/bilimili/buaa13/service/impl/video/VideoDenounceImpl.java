package com.bilimili.buaa13.service.impl.video;
import com.bilimili.buaa13.service.critique.CritiqueService;
import org.springframework.stereotype.Service;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bilimili.buaa13.entity.*;
import com.bilimili.buaa13.im.IMServer;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.bilimili.buaa13.mapper.ArticleMapper;
import com.bilimili.buaa13.mapper.CritiqueMapper;
import com.bilimili.buaa13.utils.RedisUtil;

import java.util.*;
import java.util.stream.Stream;
import java.util.stream.Collectors;
import java.util.concurrent.Executor;


import com.bilimili.buaa13.service.*;
import io.netty.channel.Channel;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.bilimili.buaa13.service.video.VideoDenounceService;

public class VideoDenounceImpl implements VideoDenounceService {


    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private CritiqueMapper critiqueMapper;

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private ArticleStatsService articleStatsService;

    @Autowired
    private UserService userService;

    @Autowired
    private MsgUnreadService msgUnreadService;

    @Autowired
    @Qualifier("taskExecutor")
    private Executor taskExecutor;


    @Override
    @Transactional
    public ResponseResult deleteCritique(Integer criId, Integer postId, boolean isAdmin) {
        // 初始化响应对象
        ResponseResult responseResult = new ResponseResult();

        // 查询评论是否存在且未被删除
        Critique critique = critiqueMapper.selectOne(new QueryWrapper<Critique>()
                .eq("criId", criId)
                .eq("is_deleted", 0));

        if (critique == null) {
            // 评论不存在
            return new ResponseResult();
        }

        // 获取文章信息
        Article article = articleMapper.selectById(critique.getAid());

        // 判断删除权限：管理员、本评论的发布者、文章作者
        if (isAdmin || critique.getPostId().equals(postId) || article.getUid().equals(postId)) {
            // 标记该评论为已删除
            critiqueMapper.update(null, new UpdateWrapper<Critique>()
                    .eq("criId", criId)
                    .set("is_deleted", 1));

            // 更新文章统计数据
            //articleStatsService.updateArticleStats(critique.getAid(), "critique", false, 1);

            // 递归删除所有子评论
            deleteChildCritiques(criId, postId);
                                      // 删除成功
            //return new ResponseResult(200, "删除成功!");
            return new ResponseResult();
        } else {
            // 无权删除
            //return new ResponseResult(403, "你无权删除该条评论");
            return new ResponseResult();
        }
    }

    private void deleteChildCritiques(Integer rootCriId, Integer postId) {
        List<Critique> childCritiques = getChildCritiquesByRootId(rootCriId, 0L, -1L);

        if (childCritiques != null && !childCritiques.isEmpty()) {
            for (Critique child : childCritiques) {
                critiqueMapper.update(null, new UpdateWrapper<Critique>()
                        .eq("criId", child.getCriId())
                        .set("is_deleted", 1));
                deleteChildCritiques(child.getCriId(), postId);
            }
        }
    }

    private List<Critique> getChildCritiquesByRootId(Integer rootCriId, long l, long l1) {
        return List.of();
    }

}

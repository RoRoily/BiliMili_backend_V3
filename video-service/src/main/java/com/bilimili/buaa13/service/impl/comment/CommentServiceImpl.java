package com.bilimili.buaa13.service.impl.comment;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bilimili.buaa13.entity.Comment;
import com.bilimili.buaa13.entity.CommentTree;
import com.bilimili.buaa13.entity.ResponseResult;
import com.bilimili.buaa13.entity.Video;
import com.bilimili.buaa13.mapper.CommentMapper;
import com.bilimili.buaa13.mapper.VideoMapper;
import com.bilimili.buaa13.service.client.UserServiceClient;
import com.bilimili.buaa13.service.comment.CommentService;
import com.bilimili.buaa13.service.video.VideoStatusService;
import com.bilimili.buaa13.tools.RedisTool;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.Executor;
import java.util.stream.Stream;


@Slf4j
@Service
public class CommentServiceImpl implements CommentService {
    @Autowired
    private RedisTool redisTool;

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private VideoMapper videoMapper;

    @Autowired
    private VideoStatusService videoStatusService;

    @Autowired
    private UserServiceClient userServiceClient;

    @Autowired
    @Qualifier("taskExecutor")
    private Executor taskExecutor;

    @Autowired
    private RedisTemplate<String,Object> redisTemplate;

    /**
     * 获取评论树列表
     * @param vid   对应视频ID
     * @param offset 分页偏移量（已经获取到的评论树的数量）
     * @param sortType  排序类型 1 按热度排序 2 按时间排序
     * @return  评论树列表
     * 2024.08.05
     */
    @Override
    public List<CommentTree> getCommentTreeByVid(Integer vid, Long offset, Integer sortType) {
        // 查询父级评论
        List<Comment> commentsRoot = getRootCommentsByVid(vid, offset, sortType);
        List<CommentTree> commentTrees = new ArrayList<>();
        for (Comment comment : commentsRoot) {
            CommentTree commentTree = buildCommentTree(comment,0L,2L);
            commentTrees.add(commentTree);
        }
        //System.out.println("getCommentTreeByVid+: "+commentTrees);
        return commentTrees;
    }

    /**
     * 构建评论树
     * @param comment 根评论
     * @param start 子评论开始偏移量
     * @param end  子评论结束偏移量
     * @return  单棵评论树
     * 2024.08.03
     */
    private CommentTree buildCommentTree(Comment comment, Long start, Long end) {
        //先将子节点设为null，下面再递归构建
        CommentTree commentTree = new CommentTree(
                comment.getId(),
                comment.getVid(),
                comment.getRootId(),
                comment.getParentId(),
                comment.getContent(),
                userServiceClient.getUserById(comment.getUid()),
                userServiceClient.getUserById(comment.getToUserId()),
                comment.getLove(),
                comment.getBad(),
                null,
                comment.getCreateTime(),
                0L
        );

        // 递归查询构建子评论树
        Integer Id = comment.getId();
        QueryWrapper<Comment> commentQueryWrapper = new QueryWrapper<>();
        commentQueryWrapper.eq("parent_id", Id).ne("is_deleted", 1);
        long count = commentMapper.selectCount(commentQueryWrapper);
        commentTree.setCount(count);
        List<Comment> comments;
        if(end.equals(-1L)){
            end = Long.MAX_VALUE;
        }
        long limit = end - start +1;
        long currentPage = start / limit + 1;
        Page<Comment> page = new Page<>(currentPage, limit);
        Page<Comment> commentPage = commentMapper.selectPage(page, commentQueryWrapper);
        //查询回复的评论数组
        comments = commentPage.getRecords();
        List<Comment> sonComments = getChildCommentsByRootId(comment.getId(), start, end);
        if (comment.getRootId() == 0 || (comments!=null && !comments.isEmpty()) || (sonComments !=null && !sonComments.isEmpty())) {
            List<CommentTree> sonTreeList = new ArrayList<>();
            if(sonComments ==null || sonComments.isEmpty()){ sonComments = comments;}
            for(Comment sonComment : sonComments) {
                CommentTree sonNode = buildCommentTree(sonComment, start, end);
                sonTreeList.add(sonNode);
            }
            commentTree.setReplies(sonTreeList);
        }
        return commentTree;
    }

    /**
     * 发送评论，字数不得大于2000或为空
     * @param vid   视频id
     * @param uid   发布者uid
     * @param rootId    楼层id（根评论id）
     * @param parentId  被回复的评论id
     * @param toUserId  被回复用户uid
     * @param content   评论内容
     * @return  true 发送成功 false 发送失败
     * 2024.08.05
     */
    @Override
    @Transactional
    public CommentTree sendComment(Integer vid, Integer uid, Integer rootId, Integer parentId, Integer toUserId, String content) {
        if (content == null || content.isEmpty() || content.length() > 2000) return null;
        //System.out.println("sendComment: "+uid);
        Comment comment = new Comment(
                null,
                vid,
                uid,
                rootId,
                parentId,
                toUserId,
                content,
                0,
                0,
                new Date(),
                null,
                null
        );
        System.out.println("sendComment:  comment: "+comment);
        commentMapper.insert(comment);
        // 更新视频评论 + 1
        videoStatusService.updateVideoStatus(comment.getVid(), "comment", true, 1);

        CommentTree commentTree = buildCommentTree(comment, 0L, -1L);

        try {
            //1注释Redis
            // 如果不是根级评论，则加入 redis 对应的 storeZSet 中
            if (!rootId.equals(0)) {
                redisTool.storeZSet("comment_reply:" + rootId, comment.getId());
            } else {
                redisTool.storeZSet("comment_video:"+ vid, comment.getId());
            }
            // 表示被回复的用户收到的回复评论的 Id 有序集合
            userServiceClient.handleComment(comment.getUid(),comment.getToUserId(),comment.getId());
        } catch (Exception e) {
            log.error("评论出错了\n{}", e.getMessage());
        }

        return commentTree;
    }

    /**
     * 删除评论
     * @param Id    评论id
     * @param uid   当前用户id
     * @param isAdmin   是否是管理员
     * @return  响应对象
     */
    @Override
    @Transactional
    public ResponseResult deleteComment(Integer Id, Integer uid, boolean isAdmin) {
        ResponseResult responseResult = new ResponseResult();
        QueryWrapper<Comment> commentQueryWrapper = new QueryWrapper<>();
        commentQueryWrapper.eq("Id", Id).ne("is_deleted", 1);
        Comment comment = commentMapper.selectOne(commentQueryWrapper);
        if (comment == null) {
            responseResult.setCode(404);
            responseResult.setMessage("评论不存在");
            return responseResult;
        }

        // 限制评论只能由本人或管理员或作者删除
        Video video = videoMapper.selectById(comment.getVid());
        if (comment.getUid().equals(uid) || isAdmin || video.getUid().equals(uid)) {
            // 删除评论
            UpdateWrapper<Comment> deleteCommentWrapper = new UpdateWrapper<>();
            deleteCommentWrapper.eq("Id", comment.getId()).set("is_deleted", 1);
            commentMapper.update(null, deleteCommentWrapper);
            videoStatusService.updateVideoStatus(comment.getVid(), "comment", false, 1);
            //并行流递归删除子评论
            List<Comment> childComments = getChildCommentsByRootId(Id,0L,-1L);
            if (childComments == null || childComments.isEmpty()) {
                responseResult.setMessage("已删除完毕");
                return responseResult;
            }
            List<ResponseResult> responseResults = childComments.stream().parallel().flatMap(
                    childComment ->{
                        ResponseResult childResponse = new ResponseResult();
                        childResponse = deleteComment(childComment.getId(),uid,true);
                        return Stream.of(childResponse);
                    }
            ).toList();

            //1注释Redis
            /*
             如果该评论是根节点评论，则删掉其所有回复。
             如果不是根节点评论，则将他所在的 comment_reply(storeZSet) 中的 comment_id 删掉
             */
            if (comment.getRootId()==0) {
                // 查询总共要减少多少评论数
                int count = Math.toIntExact(redisTool.getZSetNumber("comment_reply:" + comment.getId()));
                videoStatusService.updateVideoStatus(comment.getVid(), "comment", false, count + 1);
                redisTool.deleteZSetMember("comment_video:" + comment.getVid(), comment.getId());
                redisTool.deleteValue("comment_reply:" + comment.getId());
            } else {
                videoStatusService.updateVideoStatus(comment.getVid(), "comment", false, 1);
                redisTool.deleteZSetMember("comment_reply:" + comment.getRootId(), comment.getId());
            }

            responseResult.setCode(200);
            responseResult.setMessage("删除成功!");
        } else {
            responseResult.setCode(403);
            responseResult.setMessage("你无权删除该条评论");
        }
        return responseResult;
    }

    /**
     * @param rootId 根级节点的评论 Id, 即楼层 Id
     * @return 1. 根据 redis 查找出回复该评论的子评论 Id 列表
     * 2. 根据 Id 多线程查询出所有评论的详细信息
     * 2024.08.03
     */
    @Override
    public List<Comment> getChildCommentsByRootId(Integer rootId, Long start, Long end) {
        //1注释Redis
        Set<Object> replyIds = redisTemplate.opsForZSet().range("comment_reply:" + rootId, start, end);
        if (replyIds == null || replyIds.isEmpty()) return Collections.emptyList();
        QueryWrapper<Comment> wrapper = new QueryWrapper<>();
        wrapper.in("Id", replyIds).ne("is_deleted", 1);
        return commentMapper.selectList(wrapper);
    }

    /**
     * 根据视频 vid 获取根评论列表，一次查 10 条
     * @param vid 视频 Id
     * @param offset 偏移量，已经获取到的根评论数量
     * @param sortType 1:按热度排序 2:按时间排序
     * @return List<Comment>
     * 2024.08.05
     */
    @Override
    public List<Comment> getRootCommentsByVid(Integer vid, Long offset, Integer sortType) {
        //1注释Redis
        Set<Object> rootIdsSet;
        if (sortType == 1) {
            // 按热度排序就不能用时间分数查偏移量了，要全部查出来，后续在MySQL筛选
            rootIdsSet = redisTool.reverseRange("comment_video:" + vid, 0L, -1L);
        } else {
            rootIdsSet = redisTool.reverseRange("comment_video:" + vid, offset, offset + 9L);
        }

        if (rootIdsSet == null || rootIdsSet.isEmpty()) return Collections.emptyList();

        QueryWrapper<Comment> commentQueryWrapper = new QueryWrapper<>();
        commentQueryWrapper.in("Id", rootIdsSet).ne("is_deleted", 1);
        if (sortType == 1) { // 热度
            commentQueryWrapper.orderByDesc("(love - bad)").last("LIMIT 10 OFFSET " + offset);
        } else if(sortType == 2){ // 时间
            commentQueryWrapper.orderByDesc("create_time");
        }
        //System.out.println("getRootCommentsByVid "+commentMapper.selectList(commentQueryWrapper));
        return commentMapper.selectList(commentQueryWrapper);
    }

    /**
     * 获取更多回复评论
     * 由于获取的是回复，所以根据当前评论节点为基准，构建评论树
     * @param Id 根评论id
     * @return  包含全部回复评论的评论树
     * 2024.08.03
     */
    @Override
    public CommentTree getMoreCommentsById(Integer Id) {
        Comment comment = commentMapper.selectById(Id);
        return buildCommentTree(comment, 0L, -1L);
    }

    /**
     * 同时相对更新点赞和点踩
     * 用于原本点踩了，现在直接点赞，一次改完。
     * @param Id    评论id
     * @param addUpVote   true 点赞 false 点踩
     * 2024.08.03
     */
    @Override
    public void updateLikeAndDisLike(Integer Id, boolean addUpVote) {
        UpdateWrapper<Comment> updateWrapper = new UpdateWrapper<>();
        if (addUpVote) {
            updateWrapper.setSql(
                    "love = love + 1, bad = CASE WHEN " +
                    "bad - 1 < 0 " +
                    "THEN 0 " +
                    "ELSE bad - 1 END"
            );
        } else {
            updateWrapper.setSql(
                    "bad = bad + 1, love = CASE WHEN " +
                    "love - 1 < 0 " +
                    "THEN 0 " +
                    "ELSE love - 1 END"
            );
        }

        commentMapper.update(null, updateWrapper);
    }

    /**
     * 单独更新点赞或点踩
     * @param Id    评论id
     * @param column    "love" 点赞 "bad" 点踩
     * @param increase  true 增加 false 减少
     * @param count     更改数量
     * 2024.08.03
     */
    @Override
    public void updateComment(Integer Id, String column, boolean increase, Integer count) {
        UpdateWrapper<Comment> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", Id);
        if (increase) {
            updateWrapper.setSql(column + " = " + column + " + " + count);
        } else {
            // 减少对应值，减少后值必须大于等于0，需要检验
            updateWrapper.setSql(column + " = CASE WHEN " + column + " - " + count + " < 0 THEN 0 ELSE " + column + " - " + count + " END");
        }
        commentMapper.update(null, updateWrapper);
    }

    @Override
    public boolean reportComment(Integer id, Integer userId, String reason) {
        return false;
    }
}
package com.bilimili.buaa13.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class Critique {
    @TableId(type = IdType.AUTO)
    private Integer criId;
    private Integer aid;
    private Integer rootCid;
    private Integer parentCid;//父节点id，给这个id的评论发送
    private Integer postId;
    private Integer acceptId;
    private String content;
    private Integer upVote;
    private Integer downVote;
    private Date createTime;
    private Integer isTop;
    private Integer isDeleted;
}
package com.bilimili.buaa13.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bilimili.buaa13.entity.MessageUnread;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MessageUnreadMapper extends BaseMapper<MessageUnread> {
    int updateByIdWithVersion(MessageUnread messageUnread);
}

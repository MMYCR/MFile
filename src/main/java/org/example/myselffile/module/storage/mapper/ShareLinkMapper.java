package org.example.myselffile.module.storage.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.myselffile.module.storage.model.entity.ShareLink;

@Mapper
public interface ShareLinkMapper extends BaseMapper<ShareLink> {
}
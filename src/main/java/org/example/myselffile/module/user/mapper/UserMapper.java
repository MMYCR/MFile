package org.example.myselffile.module.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.myselffile.module.user.model.eneity.User;

@Mapper
public interface UserMapper extends BaseMapper<User> {
    int NumberOfSameName (@Param("username") String username);

}
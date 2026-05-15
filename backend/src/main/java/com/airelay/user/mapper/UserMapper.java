package com.airelay.user.mapper;

import com.airelay.user.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select("SELECT * FROM user WHERE id = #{id} AND deleted = 0 FOR UPDATE")
    User selectForUpdate(Long id);
}

package org.example.myselffile.module.user.service;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import jakarta.annotation.Resource;
import org.example.myselffile.core.exception.BizException;
import org.example.myselffile.core.exception.ErrorCode;
import org.example.myselffile.module.user.mapper.UserMapper;
import org.example.myselffile.module.user.model.UserIds;
import org.example.myselffile.module.user.model.eneity.User;
import org.example.myselffile.module.user.model.request.UpdateUserPasswordRequest;
import org.example.myselffile.utils.PasswordUtils;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Objects;

@Service
public class UserService {
    @Resource
    private UserMapper userMapper;

    /**
     * 根据用户名查询用户ID
     * @param username 用户名
     * @return 用户ID，如果不存在则返回null
     */
    public Long getIdByName(String username) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .select(User::getId) // 只查 ID 字段，优化性能
                .eq(User::getUsername, username));
        return user != null ? user.getId() : null;
    }

    /**
     * 根据用户名查询当前用户名出现次数
     * @param username 用户名
     * @return 出现次数,不存在则返回0
     */
    public int numberOfSameName(String username) {
        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
        // 如果 count 为 null（极少见），返回 0，否则转 int
        return count == null ? 0 : count.intValue();
    }

    /**
     * 通过ID更新用户名
     * @param id 用户id
     * @param username 新用户名
     * @return 更新的行数
     */
    public int updateNameById(long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        return userMapper.updateById(user); // MP 自带的更新，只更新非空字段
    }

    /**
     * 根据 ID 获取用户
     * @param id 用户id
     * @return 用户对象
     */
    public User getUserById(Long id) {
        if (id == null) { // 额外安全检查，尽管上游已经检查过
            System.out.println("DEBUG: getUserById received null ID."); // 添加调试日志

            return null;
        }
        System.out.println("DEBUG: getUserById will query for ID: " + id); // 添加调试日志
        return userMapper.selectById(id); // Mybatis-Plus 提供的 getById 方法
    }

    /**
     * 根据用户名获取用户
     * @param name 用户名
     * @return 用户对象
     */
    public User getUserByName(String name) {
        Long userId = this.getIdByName(name); // 直接调用 this 上的方法
        if (userId == null) {
            return null;
        }
        return this.getUserById(userId); // 直接调用 this 上的方法
    }
    /**
     * 删除用户
     * @param id 用户ID
     * @return 是否成功 (返回boolean或者影响的行数)
     */
    public boolean deleteUserById(Long id) {
        // 直接调用 userMapper.deleteById
        int rows = userMapper.deleteById(id);
        return rows > 0;
    }

    /**
     * 注册新用户
     * @param username 用户名
     * @param rawPassword 明文密码
     */
    public void register(String username, String rawPassword) {
        // 1. 检查用户名是否已存在
        Long existId = getIdByName(username);
        if (existId != null) {
            // 使用你定义的错误码: 4003 用户名已存在
            throw new BizException(ErrorCode.BIZ_USERNAME_ALREADY_EXISTS);
        }

        // 2. 创建用户对象
        User user = new User();
        user.setUsername(username);
        // 3. 密码加密 (复用 PasswordUtils)
        user.setPassword(PasswordUtils.encrypt(rawPassword));
        user.setEnable(true);
        // 如果你没有配置 MyBatisPlus 的自动填充(MetaObjectHandler)，这里最好手动设一下时间
        user.setCreatetime(new Date());
        user.setUpdatetime(new Date());

        // 4. 插入数据库
        userMapper.insert(user);
    }

    /**
     * 通过ID更新用户密码
     */
    public void updatePwById(Long id, UpdateUserPasswordRequest request) {
        User user = userMapper.selectById(id);

        // 校验用户是否存在
        if (user == null) {
            throw new BizException(ErrorCode.AUTH_USER_NOT_FOUND);
        }
        if (StringUtils.isNotBlank(user.getPassword()) &&
                !PasswordUtils.verify(user.getPassword(), request.getOldPassword())) {
            // 抛出: 2001 密码错误
            throw new BizException(ErrorCode.AUTH_PASSWORD_INVALID);
        }
        if (!request.getNewPassword().equals(request.getNewPasswordConfig())) {
            // 抛出: 4001 新密码和确认密码不一致
            throw new BizException(ErrorCode.BIZ_PASSWORD_NOT_SAME);
        }
        if (PasswordUtils.verify(user.getPassword(), request.getNewPassword())) {
            // 抛出: 4002 新密码不能与旧密码相同
            throw new BizException(ErrorCode.BIZ_NEW_PASSWORD_CANNOT_BE_SAME_AS_OLD);
        }

        user.setPassword(PasswordUtils.encrypt(request.getNewPassword()));
        user.setUpdatetime(new Date()); // 更新时间
        userMapper.updateById(user);
    }

    /**
     * 获取所有用户
     * @return 所有用户列表
     */
    public List<User> listAllUsers() {
        return userMapper.selectList(null); // selectList(null) 查询所有
    }

    public boolean isAdmin(long id)
    {
        return Objects.equals(UserIds.ADMIN_ID,id);
    }
}

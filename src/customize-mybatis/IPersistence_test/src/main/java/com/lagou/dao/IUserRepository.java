package com.lagou.dao;

import com.lagou.pojo.User;
import com.lagou.pojo.UserQuery;

import java.util.List;

/**
 * @author francis
 * @date 2020/3/6 4:58 下午
 * @description
 */
public interface IUserRepository {

    List<User> queryList();

    User querySingle(UserQuery query);

    int insert(User user);

    int update(User user);

    boolean delete(Integer id);
}

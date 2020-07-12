package com.lagou.dao;

import com.lagou.pojo.Account;

import java.sql.SQLException;

public interface AccountDao {

    public Account getByCardNo(String cardNo) throws SQLException;

    boolean addMoney(String cardNo, Integer money) throws SQLException;
}

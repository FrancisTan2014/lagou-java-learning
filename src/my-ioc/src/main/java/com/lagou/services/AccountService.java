package com.lagou.services;

import com.lagou.pojo.Account;

import java.sql.SQLException;

public interface AccountService {

    public Account getByCardNo(String cardNo) throws SQLException;

    public boolean transfer(String fromCardNo, String toCardNo, Integer money) throws SQLException;

}

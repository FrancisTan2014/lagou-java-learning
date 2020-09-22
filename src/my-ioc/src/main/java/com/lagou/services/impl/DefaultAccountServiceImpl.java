package com.lagou.services.impl;

import com.lagou.dao.AccountDao;
import com.lagou.dao.impl.AccountDaoImpl;
import com.lagou.ioc.Autowired;
import com.lagou.ioc.Service;
import com.lagou.pojo.Account;
import com.lagou.services.AccountService;

import java.sql.SQLException;

@Service
public class DefaultAccountServiceImpl implements AccountService {

    @Autowired
    private AccountDao dao;

    @Override
    public Account getByCardNo(String cardNo) throws SQLException {
        Account byCardNo = dao.getByCardNo(cardNo);
        return byCardNo;
    }

    @Override
    public boolean transfer(String fromCardNo, String toCardNo, Integer money) throws SQLException {
        boolean success = dao.addMoney(fromCardNo, -money);
        if (false == success) {
            return false;
        }

        success = dao.addMoney(toCardNo, money);
        return success;
    }

}

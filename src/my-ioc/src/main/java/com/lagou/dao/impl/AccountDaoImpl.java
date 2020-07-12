package com.lagou.dao.impl;

import com.lagou.dao.AccountDao;
import com.lagou.pojo.Account;
import com.lagou.utils.DruidUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AccountDaoImpl implements AccountDao {

    @Override
    public Account getByCardNo(String cardNo) throws SQLException {
        try (Connection connection = DruidUtils.getInstance().getSqlConnection()) {
            String sql = "SELECT * FROM Account WHERE cardNo=?;";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, cardNo);

            ResultSet resultSet = statement.executeQuery();
            if (false == resultSet.next()) {
                return null;
            }

            String cardNumber = resultSet.getString("cardNo");
            String name = resultSet.getString("name");
            int money = resultSet.getInt("money");

            Account account  = new Account();
            account.setCardNo(cardNumber);
            account.setName(name);
            account.setMoney(money);

            return account;
        }
    }

    @Override
    public boolean addMoney(String cardNo, Integer money) throws SQLException {
        try (Connection connection = DruidUtils.getInstance().getSqlConnection()) {
            String sql = "UPDATE Account SET money=money+? WHERE cardNo=?;";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setInt(1, money);
            statement.setString(2, cardNo);

            int affectedRows = statement.executeUpdate();
            return affectedRows > 0;
        }
    }

}

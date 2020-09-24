package com.lagou.controlers;

import com.alibaba.fastjson.JSON;
import com.lagou.ioc.BeanFactory;
import com.lagou.ioc.impl.AppContextBeanFactory;
import com.lagou.pojo.ApiResponse;
import com.lagou.services.AccountService;
import org.dom4j.DocumentException;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

@WebServlet(name = "transferServlet", urlPatterns = "/transferServlet")
public class TransferServlet extends HttpServlet {

    private AccountService accountService;

    public TransferServlet() throws IOException, DocumentException,
            IllegalAccessException, InstantiationException, ClassNotFoundException {
        try {
            BeanFactory factory = new AppContextBeanFactory();
            accountService = factory.getBean(AccountService.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String fromCardNo = req.getParameter("fromCardNo");
        String toCardNo = req.getParameter("toCardNo");
        Integer money = Integer.parseInt(req.getParameter("money"));

        ApiResponse response = new ApiResponse();
        try {
            boolean success = accountService.transfer(fromCardNo, toCardNo, money);
            response.setStatusCode(success ? 200 : 500);
            response.setMessage(success ? "转账成功" : "转账失败");
        } catch (SQLException e) {
            response.setStatusCode(500);
            response.setMessage(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }

        String json = JSON.toJSONString(response);
        resp.setContentType("application/json;charset=utf-8");
        resp.getWriter().print(json);
    }
}

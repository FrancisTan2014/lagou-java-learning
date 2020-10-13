package com.lagou.edu.spring.mvc.sample;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import java.util.Date;

@Controller
@RequestMapping("/datetime")
public class DatetimeController {

    @RequestMapping("/now")
    public ModelAndView now() {
        Date date = new Date();
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.addObject("date", date);
        String hello = """
                Hello, JAVA 15!
                """;
        modelAndView.addObject("hello", hello);

        return modelAndView;
    }

}

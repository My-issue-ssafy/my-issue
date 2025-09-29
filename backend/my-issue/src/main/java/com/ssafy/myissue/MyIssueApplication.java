package com.ssafy.myissue;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MyIssueApplication {

    public static void main(String[] args) {
        SpringApplication.run(MyIssueApplication.class, args);
    }

}

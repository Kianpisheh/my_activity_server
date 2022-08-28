package com.example.my_activity_server.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.my_activity_server.service.LoginService;

@RestController
@CrossOrigin(origins = "http://localhost:3000")
@RequestMapping(path = "/pass")
public class LoginController {
    private final LoginService loginService;

    @Autowired
    public LoginController(LoginService loginService) {
        this.loginService = loginService;
    }

    @PostMapping
    public Boolean checkPassword(@RequestBody Map<String, String> userData) {
        return loginService.checkPassword(userData);
    }
}

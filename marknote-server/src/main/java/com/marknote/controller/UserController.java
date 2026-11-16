package com.marknote.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.marknote.entity.User;
import com.marknote.service.UserService;
import com.marknote.util.JwtUtil;
import com.marknote.util.Result;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;

    @Resource
    private JwtUtil jwtUtil;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    /**
     * 登录：兼容 bcrypt 与明文（演示账号）两种密码
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody User loginUser) {
        if (loginUser == null || loginUser.getUsername() == null || loginUser.getPassword() == null) {
            return Result.fail("账号密码不能为空");
        }
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, loginUser.getUsername());
        User user = userService.getOne(wrapper);
        if (user == null) {
            return Result.fail("账号不存在");
        }

        String stored = user.getPassword();
        boolean ok;
        if (stored != null && stored.startsWith("$2")) {
            ok = encoder.matches(loginUser.getPassword(), stored);
        } else {
            ok = loginUser.getPassword().equals(stored);
        }
        if (!ok) {
            return Result.fail("密码错误");
        }

        String token = jwtUtil.generateToken(user.getId());
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userId", user.getId());
        data.put("username", user.getUsername());
        data.put("nickname", user.getNickname() == null ? user.getUsername() : user.getNickname());
        return Result.success(data);
    }

    /**
     * 注册
     */
    @PostMapping("/register")
    public Result<Object> register(@RequestBody User regUser) {
        if (regUser.getUsername() == null || regUser.getPassword() == null
                || regUser.getUsername().trim().isEmpty() || regUser.getPassword().trim().isEmpty()) {
            return Result.fail("账号密码不能为空");
        }
        if (userService.lambdaQuery().eq(User::getUsername, regUser.getUsername()).count() > 0) {
            return Result.fail("账号已存在");
        }
        regUser.setPassword(encoder.encode(regUser.getPassword()));
        regUser.setDeleted(0);
        userService.save(regUser);
        return Result.success();
    }

    /**
     * 当前登录用户信息
     */
    @GetMapping("/info")
    public Result<User> info(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        User user = userService.getById(userId);
        if (user != null) {
            user.setPassword(null);
        }
        return Result.success(user);
    }
}

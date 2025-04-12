package com.codewiz.socialmedia.controller;

import com.codewiz.socialmedia.dto.UserResponse;
import com.codewiz.socialmedia.model.*;
import com.codewiz.socialmedia.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/user")
@AllArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/signup")
    public ResponseEntity<User> register(
            @RequestParam String name,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam(required = false) MultipartFile profilePhoto) throws IOException {
        UserDto registerUserDto = new UserDto(name, email, password, profilePhoto);
        User registeredUser = userService.register(registerUserDto);
        return ResponseEntity.ok(registeredUser);
    }


    @PostMapping("/signin")
    public ResponseEntity<LoginResponse> authenticate(@RequestBody LoginDto loginUserDto) {
        var loginResponse = userService.authenticate(loginUserDto);
        return ResponseEntity.ok(loginResponse);
    }


    @GetMapping("/{id}/user-detail")
    public ResponseEntity<UserResponse> getUserById(@PathVariable String id){
        return ResponseEntity.ok().body(userService.getUserById(id));
    }



    @PutMapping("/{id}/update")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable String id,
            @RequestParam String name,
            @RequestParam String email,
            @RequestParam(required = false) MultipartFile profilePhoto
    ) throws IOException {
        UpdateUserDto updateUserDto = new UpdateUserDto(name, email, profilePhoto);
        UserResponse updatedUser = userService.updateUser(id, updateUserDto);
        return ResponseEntity.ok(updatedUser);
    }

}

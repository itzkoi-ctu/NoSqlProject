package com.codewiz.socialmedia.controller;


import com.codewiz.socialmedia.dto.UserResponse;
import com.codewiz.socialmedia.model.*;
import com.codewiz.socialmedia.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

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
            @RequestParam String gender,
            @RequestParam String dateOfBirth,
            @RequestParam(required = false) List<String> otherWebsites,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String stateOrProvince,
            @RequestParam(required = false) String zipCode,
            @RequestParam(required = false) String streetAddress,
            @RequestParam(required = false) MultipartFile profilePhoto) throws IOException {
        // Map các trường vào AddressDto
        AddressDto addressDto = (country != null || city != null || stateOrProvince != null || zipCode != null || streetAddress != null)
                ? new AddressDto(country, city, stateOrProvince, zipCode, streetAddress)
                : null;

        UserDto registerUserDto = new UserDto(name, email, password, profilePhoto, gender, dateOfBirth, otherWebsites, addressDto);
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

    @GetMapping("/search")
    public ResponseEntity<?> searchUsers(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) String dateOfBirth,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String stateOrProvince,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) List<String> otherWebsites,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        UserSearchCriteria criteria = new UserSearchCriteria(
                name, email, gender, dateOfBirth, country, stateOrProvince, city, otherWebsites, page, size
        );
        return ResponseEntity.ok(userService.searchUsers(criteria));
    }

    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(
            @PathVariable String id,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) String dateOfBirth,
            @RequestParam(required = false) List<String> otherWebsites,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String stateOrProvince,
            @RequestParam(required = false) String zipCode,
            @RequestParam(required = false) String streetAddress,
            @RequestParam(required = false) MultipartFile profilePhoto
    ) throws IOException {
        AddressDto addressDto = (country != null || city != null || stateOrProvince != null || zipCode != null || streetAddress != null)
                ? new AddressDto(country, city, stateOrProvince, zipCode, streetAddress)
                : null;

        UserDto userDto = new UserDto(name, email, null, profilePhoto, gender, dateOfBirth, otherWebsites, addressDto);

        User updatedUser = userService.updateUser(id, userDto);
        return ResponseEntity.ok(updatedUser);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable String id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}

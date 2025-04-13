package com.codewiz.socialmedia.model;

import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;

public record UserDto(
        String name,
        String email,
        String password,
        MultipartFile profilePhoto,
        String gender,
        String dateOfBirth,
        List<String> otherWebsites,
        AddressDto address
){
}

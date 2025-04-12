package com.codewiz.socialmedia.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
@AllArgsConstructor
public class UpdateUserDto {
    private String name;
    private String email;
    private MultipartFile profilePhoto;
}

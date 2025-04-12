package com.codewiz.socialmedia.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class UserResponse {
    private String id;
    private String name;
    private String email;
    private String profilePhoto;
}

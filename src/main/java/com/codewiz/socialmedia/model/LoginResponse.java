package com.codewiz.socialmedia.model;

public record LoginResponse(String token,String userId, String name, String email, String profilePhoto) {
}

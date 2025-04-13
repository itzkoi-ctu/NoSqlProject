package com.codewiz.socialmedia.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class UserSearchCriteria{
    private String name;
    private String email;
    private String gender;
    private String country;
    private String stateOrProvince;
    private String city;
    private String dateOfBirth; // định dạng yyyy-MM-dd nếu muốn
    private List<String> otherWebsites;
    private int page;
    private int size;
}

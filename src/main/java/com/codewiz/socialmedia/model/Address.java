package com.codewiz.socialmedia.model;

import lombok.Data;

@Data
public class Address {
    private String country;       // Ví dụ: "Vietnam"
    private String city;          // Ví dụ: "Ho Chi Minh City"
    private String stateOrProvince; // Ví dụ: "District 1" or "California"
    private String zipCode;       // Optional
    private String streetAddress; // Ví dụ: "123 Nguyen Hue street"
}

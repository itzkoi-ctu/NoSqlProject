package com.codewiz.socialmedia.model;

public record AddressDto(
        String country,           // Quốc gia
        String city,              // Thành phố
        String stateOrProvince,   // Tỉnh/bang
        String zipCode,           // Mã bưu điện (tuỳ chọn)
        String streetAddress      // Địa chỉ chi tiết
) {
}

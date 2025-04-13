package com.codewiz.socialmedia.controller;

import com.codewiz.socialmedia.config.AWSConfig;
import com.codewiz.socialmedia.model.AddressDto;
import com.codewiz.socialmedia.model.LoginDto;
import com.codewiz.socialmedia.model.UserDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;

import java.io.File;
import java.util.List;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public class UserControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @Container
    @ServiceConnection
    final static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:4.0.10");

    @Container
    final static LocalStackContainer localStackContainer =
            new LocalStackContainer(DockerImageName.parse("localstack/localstack:3.5.0"))
                    .withServices(LocalStackContainer.Service.S3);


    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("aws.s3.accessKey", localStackContainer::getAccessKey);
        registry.add("aws.s3.secretKey", localStackContainer::getSecretKey);
        registry.add("aws.s3.region", localStackContainer::getRegion);
        registry.add("aws.s3.endpoint", () -> localStackContainer.getEndpointOverride(LocalStackContainer.Service.S3));
    }

    @BeforeAll
    public static void setUp() {
        RestAssured.baseURI = "http://localhost";
        createBucket();
    }

    private static void createBucket() {
        S3Client s3Client = S3Client.builder()
                .endpointOverride(localStackContainer.getEndpointOverride(LocalStackContainer.Service.S3))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(localStackContainer.getAccessKey(), localStackContainer.getSecretKey())))
                .region(Region.of(localStackContainer.getRegion()))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build();
        s3Client.createBucket(CreateBucketRequest.builder().bucket(AWSConfig.BUCKET_NAME).build());
    }

    @Test
    @Order(1)
    public void testRegisterUser() {
        UserDto userDto = new UserDto(
                "John Doe",
                "john.doe@example.com",
                "password123",
                null,                      // Không upload ảnh
                "Male",                    // Giới tính
                "1990-01-01",              // Ngày sinh
                List.of("https://mywebsite.com"), // Websites (có thể để null hoặc empty list)
                new AddressDto(
                        "USA",           // Quốc gia
                        "New York",      // Thành phố
                        "NY",            // Bang
                        "10001",         // Mã bưu điện
                        "5th Avenue"     // Địa chỉ
                )
        );
        File profilePhoto = new File("src/test/resources/profile-photo.jpg");

        String addressJson = null;
        try {
            addressJson = new ObjectMapper().writeValueAsString(userDto.address());
        } catch (JsonProcessingException e) {
            e.printStackTrace();  // Hoặc ném ngoại lệ
        }

        given()
                .port(port)
                .contentType("multipart/form-data")
                .multiPart("name", userDto.name())
                .multiPart("email", userDto.email())
                .multiPart("password", userDto.password())
                .multiPart("gender", userDto.gender())
                .multiPart("dateOfBirth", userDto.dateOfBirth())
                .multiPart("otherWebsites", userDto.otherWebsites())
                .multiPart("profilePhoto", profilePhoto) // Nếu có ảnh
                .multiPart("address", addressJson)
                .when()
                .post("/user/signup")
                .then()
                .statusCode(200)
                .body("name", equalTo(userDto.name()))
                .body("email", equalTo(userDto.email()))
                .body("gender", equalTo(userDto.gender()))  // Kiểm tra giới tính
                .body("dateOfBirth", equalTo(userDto.dateOfBirth())) // Kiểm tra ngày sinh
                .body("address.country", equalTo(userDto.address().country()))  // Kiểm tra địa chỉ
                .body("address.city", equalTo(userDto.address().city()))
                .body("address.stateOrProvince", equalTo(userDto.address().stateOrProvince()))
                .body("address.zipCode", equalTo(userDto.address().zipCode()))
                .body("address.streetAddress", equalTo(userDto.address().streetAddress()));
    }

    @Test
    @Order(2)
    public void testAuthenticateUser() {
        LoginDto loginDto = new LoginDto("john.doe@example.com", "password123");
        given()
                .port(port)
                .contentType("application/json")
                .body(loginDto)
                .when()
                .post("/user/signin")
                .then()
                .statusCode(200)
                .body("token", notNullValue());
    }

}

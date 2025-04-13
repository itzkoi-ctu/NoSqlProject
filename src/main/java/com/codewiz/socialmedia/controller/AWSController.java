//package com.codewiz.socialmedia.controller;
//
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//import software.amazon.awssdk.auth.credentials.AwsCredentials;
//import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
//import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
//
//@RestController
//@RequestMapping("/aws")
//public class AWSController {
//
//    @GetMapping("/check-credentials")
//    public ResponseEntity<String> checkAwsCredentials() {
//        try {
//            AwsCredentialsProvider provider = DefaultCredentialsProvider.create();
//            AwsCredentials credentials = provider.resolveCredentials();
//
//            String accessKey = credentials.accessKeyId();
//            return ResponseEntity.ok("AWS Credentials loaded successfully! Access Key: " + accessKey);
//        } catch (Exception e) {
//            e.printStackTrace();
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to load AWS credentials: " + e.getMessage());
//        }
//    }
//}

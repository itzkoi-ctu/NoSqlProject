package com.codewiz.socialmedia.service;

import com.codewiz.socialmedia.config.AWSConfig;
import com.codewiz.socialmedia.dto.UserResponse;
import com.codewiz.socialmedia.model.*;
import com.codewiz.socialmedia.repository.PostRepository;
import com.codewiz.socialmedia.repository.UserRepository;

import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@AllArgsConstructor
public class UserService {

    private final PostRepository postRepository;

    private final UserRepository userRepository;

    private final S3Client s3Client;

    private final PasswordEncoder passwordEncoder;

    private final AuthenticationManager authenticationManager;

    private final TokenService tokenService;

    private final S3PresignedUrlService s3PresignedUrlService;

    private final MongoTemplate mongoTemplate;

    public User register(UserDto userDto) throws IOException {
        User user = new User();
        user.setName(userDto.name());
        user.setEmail(userDto.email());
        user.setPassword(passwordEncoder.encode(userDto.password()));
        user.setGender(userDto.gender());
        user.setDateOfBirth(userDto.dateOfBirth());
        user.setOtherWebsites(userDto.otherWebsites());
        user.setAddress(convertToAddress(userDto.address()));
        var uuid =  UUID.randomUUID().toString();
        user.setId(uuid);
        user.setRoles(List.of("ROLE_USER"));
        var mediaFile = userDto.profilePhoto();
        if (mediaFile != null && !mediaFile.isEmpty()) {
            String fileName = uuid + "-profile";
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(AWSConfig.BUCKET_NAME)
                    .key(fileName)
                    .build();
            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(mediaFile.getBytes()));
            user.setProfilePhoto(fileName);
        }
        return userRepository.save(user);
    }

    private Address convertToAddress(AddressDto addressDto) {
        if (addressDto == null) return null;
        Address address = new Address();
        address.setCountry(addressDto.country());
        address.setCity(addressDto.city());
        address.setStateOrProvince(addressDto.stateOrProvince());
        address.setZipCode(addressDto.zipCode());
        address.setStreetAddress(addressDto.streetAddress());
        return address;
    }

    public LoginResponse authenticate(LoginDto input) {
        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(input.email(), input.password()));
        var user = (User) authentication.getPrincipal();
        String userId= user.getId();
        String token= tokenService.generateToken(authentication);
        String profilePhotoUrl = user.getProfilePhoto() != null ? s3PresignedUrlService.generatePresignedUrl(user.getProfilePhoto()) : null;
        return new LoginResponse(token, userId,user.getName(), user.getEmail(), profilePhotoUrl);
    }

    public UserResponse getUserById(String userId){
        User user= userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found!"));


        return  UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .profilePhoto(s3PresignedUrlService.generatePresignedUrl(user.getId()+"-profile"))
                .name(user.getName())
                .build();
    }

    public UserResponse updateUser(String userId, UpdateUserDto userDto) throws IOException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setName(userDto.getName());
        user.setEmail(userDto.getEmail());

        MultipartFile mediaFile = userDto.getProfilePhoto();
        if (mediaFile != null && !mediaFile.isEmpty()) {
            String fileName = userId + "-profile";
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(AWSConfig.BUCKET_NAME)
                    .key(fileName)
                    .build();
            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(mediaFile.getBytes()));
            user.setProfilePhoto(fileName);
        }

        User updatedUser = userRepository.save(user);

        return UserResponse.builder()
                .id(updatedUser.getId())
                .name(updatedUser.getName())
                .email(updatedUser.getEmail())
                .profilePhoto(s3PresignedUrlService.generatePresignedUrl(updatedUser.getProfilePhoto()))
                .build();
    }



    public Page<User> searchUsers(UserSearchCriteria criteria) {
        // Xây dựng các điều kiện tìm kiếm từ UserSearchCriteria
        Criteria searchCriteria = new Criteria();

        if (criteria.getName() != null) {
            searchCriteria = searchCriteria.and("name").regex(criteria.getName(), "i");
        }
        if (criteria.getEmail() != null) {
            searchCriteria = searchCriteria.and("email").is(criteria.getEmail());
        }
        if (criteria.getGender() != null) {
            searchCriteria = searchCriteria.and("gender").is(criteria.getGender());
        }
        if (criteria.getCity() != null) {
            searchCriteria = searchCriteria.and("address.city").is(criteria.getCity());
        }
        if (criteria.getStateOrProvince() != null) {
            searchCriteria = searchCriteria.and("address.stateOrProvince").is(criteria.getStateOrProvince());
        }
        if (criteria.getCountry() != null) {
            searchCriteria = searchCriteria.and("address.country").is(criteria.getCountry());
        }
        if (criteria.getDateOfBirth() != null) {
            searchCriteria = searchCriteria.and("dateOfBirth").gte(criteria.getDateOfBirth());
        }
        if (criteria.getOtherWebsites() != null) {
            searchCriteria = searchCriteria.and("otherWebsites").in(criteria.getOtherWebsites());
        }

        // Tạo Query với điều kiện tìm kiếm và phân trang
        Query query = new Query(searchCriteria);

        // Phân trang
        Pageable pageable = PageRequest.of(criteria.getPage(), criteria.getSize(), Sort.by(Sort.Direction.DESC, "updatedAt"));
        query.with(pageable);

        // Thực hiện truy vấn MongoDB
        long totalCount = mongoTemplate.count(query, User.class);
        query.skip((long) pageable.getPageNumber() * pageable.getPageSize()).limit(pageable.getPageSize());
        var users = mongoTemplate.find(query, User.class);

        return new PageImpl<>(users, pageable, totalCount);
    }


    public User updateUser(String id, UserDto userDto) throws IOException {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));

        if (userDto.name() != null) user.setName(userDto.name());
        if (userDto.email() != null) user.setEmail(userDto.email());
        if (userDto.gender() != null) user.setGender(userDto.gender());
        if (userDto.dateOfBirth() != null) user.setDateOfBirth(userDto.dateOfBirth());
        if (userDto.otherWebsites() != null) user.setOtherWebsites(userDto.otherWebsites());
        if (userDto.address() != null) user.setAddress(convertToAddress(userDto.address()));

        MultipartFile profilePhoto = userDto.profilePhoto();
        if (profilePhoto != null && !profilePhoto.isEmpty()) {
            String fileName = id + "-profile"; // Sử dụng id làm tên file
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(AWSConfig.BUCKET_NAME)
                    .key(fileName)
                    .build();
            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(profilePhoto.getBytes()));
            user.setProfilePhoto(fileName);
        }

        return userRepository.save(user);
    }

    public void deleteUser(String id) {
        // Tìm user cần xóa
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));

        // Xóa ảnh profile nếu có
        deleteProfilePhotoFromS3(user);

        // Xóa các ảnh liên quan đến bài viết của user
        deletePostMediaFromS3(user);

        // Xóa tất cả các bài post của user
        postRepository.deleteAllByCreator_Id(id);

        // Xóa user khỏi DB
        userRepository.deleteById(id);
    }

    private void deleteProfilePhotoFromS3(User user) {
        if (user.getProfilePhoto() != null) {
            // Xóa ảnh đại diện trên S3
            s3Client.deleteObject(builder -> builder
                    .bucket(AWSConfig.BUCKET_NAME)
                    .key(user.getProfilePhoto())
                    .build());
            System.out.println("Deleted profile photo: " + user.getProfilePhoto());
        }
    }

    private void deletePostMediaFromS3(User user) {
        // Giả sử các bài post của user chứa ảnh/video cần xóa
        List<Post> posts = postRepository.findByCreator_Id(user.getId());

        for (Post post : posts) {
            if (post.getMediaUrl() != null) {
                // Xóa ảnh/video từ S3
                s3Client.deleteObject(builder -> builder
                        .bucket(AWSConfig.BUCKET_NAME)
                        .key(post.getMediaUrl())
                        .build());
                System.out.println("Deleted media from post: " + post.getMediaUrl());
            }
        }
    }


}

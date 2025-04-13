package com.codewiz.socialmedia.service;

import com.codewiz.socialmedia.config.AWSConfig;
import com.codewiz.socialmedia.model.MediaType;
import com.codewiz.socialmedia.model.Post;
import com.codewiz.socialmedia.model.PostCreator;
import com.codewiz.socialmedia.repository.PostRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@AllArgsConstructor
public class PostService   {
    private final PostRepository postRepository;
    private final S3Client s3Client;
    private final S3PresignedUrlService s3PresignedUrlService;

    public  Post createPost(String title, String text, List<String> tags, MultipartFile mediaFile) throws IOException {
        String fileName = storeFileInS3(mediaFile);
        PostCreator creator = getPostCreator();
        Post post = new Post();
        post.setTitle(title);
        post.setText(text);
        post.setTags(tags);
        post.setLikes(0);
        post.setCreator(creator);
        post.setCreatedAt(java.time.LocalDateTime.now());
        post.setMediaUrl(fileName);
        MediaType mediaType = getMediaType(mediaFile);
        post.setMediaType(mediaType);
        return postRepository.save(post);
    }

    private static PostCreator getPostCreator() {
        var auth = (JwtAuthenticationToken)SecurityContextHolder.getContext().getAuthentication();
        var claims = auth.getToken().getClaims();
        var userId = (String) claims.get("id");
        var userName = (String) claims.get("name");
        PostCreator creator = PostCreator.builder()
                .id(userId)
                .name(userName)
                .build();
        return creator;
    }

        private static MediaType getMediaType(MultipartFile mediaFile) {
            return Objects.requireNonNull(mediaFile.getContentType()).startsWith("video/") ? MediaType.VIDEO :
                    (mediaFile.getContentType().startsWith("image/") ? MediaType.IMAGE : null);
        }

    private String storeFileInS3(MultipartFile mediaFile) throws IOException {
        String fileName = UUID.randomUUID().toString()+" - "+ mediaFile.getOriginalFilename();
        if(mediaFile !=null && !mediaFile.isEmpty()){
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(AWSConfig.BUCKET_NAME)
                    .key(fileName)
                    .build();
            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(mediaFile.getBytes()));
        }else{return null;}
        return fileName;
    }

    public Page<Post> getAllPosts(int page, int size,String searchCriteria) {
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        var postList =
                StringUtils.hasText(searchCriteria)? postRepository.searchByText(searchCriteria,PageRequest.of(page, size, sort))
                        :postRepository.findAll(PageRequest.of(page, size, sort));
        postList.forEach(post -> {
            if(post.getMediaUrl()!=null) {
                post.setPresignedUrl(s3PresignedUrlService.generatePresignedUrl(post.getMediaUrl()));
            }
            post.getCreator().setProfilePhoto(s3PresignedUrlService.generatePresignedUrl(post.getCreator().getId()+"-profile"));
        });
        return postList;
    }
    
    public Post getPostById(String id) {
        var post =  postRepository.findById(id).orElseThrow(() -> new RuntimeException("Post not found"));
        if(post.getMediaUrl()!=null) {
            post.setPresignedUrl(s3PresignedUrlService.generatePresignedUrl(post.getMediaUrl()));
        }
        return post;
    }

    public List<Post> getPostByCreatorId(String creatorId){
        List<Post> postList= postRepository.findByCreator_Id(creatorId);
        postList.forEach(post -> {
            if(post.getMediaUrl()!= null){
                post.setPresignedUrl(s3PresignedUrlService.generatePresignedUrl(post.getMediaUrl()));
            }
            post.getCreator().setProfilePhoto(s3PresignedUrlService.generatePresignedUrl(post.getCreator().getId()+"-profile"));
        });
        return postList;
    }

    public Post updatePost(String id, String title, String text, List<String> tags, MultipartFile mediaFile) throws IOException {
        Post post = getPostById(id);
        if(post.getMediaUrl()!=null){
            s3Client.deleteObject(builder -> builder.bucket(AWSConfig.BUCKET_NAME).key(post.getMediaUrl()));
        }
        String fileName = storeFileInS3(mediaFile);
        post.setTitle(title);
        post.setText(text);
        post.setTags(tags);
        post.setMediaUrl(fileName);
        MediaType mediaType = getMediaType(mediaFile);
        post.setMediaType(mediaType);
        return postRepository.save(post);
    }

    public void deletePost(String id) {
        Post post = getPostById(id);
        if(post.getMediaUrl()!=null){
            s3Client.deleteObject(builder -> builder.bucket(AWSConfig.BUCKET_NAME).key(post.getMediaUrl()));
        }
        postRepository.deleteById(id);
    }

    public String deleteAllByCreatorId(String creatorId) {
        postRepository.deleteAllByCreator_Id(creatorId);
        return "Deleted successfully";
    }


    public String deleteAllPostsByUser(String userId) {
        // Kiểm tra quyền sở hữu trước khi cho phép xóa tất cả bài viết
        var auth = (JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        var claims = auth.getToken().getClaims();
        var currentUserId = (String) claims.get("id");

        // Nếu người dùng hiện tại không phải là chủ sở hữu của bài viết, ném ngoại lệ
        if (!currentUserId.equals(userId)) {
            throw new RuntimeException("You do not have permission to delete posts for this user.");
        }
        List<Post> posts = postRepository.findByCreator_Id(userId);
        for (Post post : posts) {
            if (post.getMediaUrl() != null) {
                try {
                    s3Client.deleteObject(b -> b.bucket(AWSConfig.BUCKET_NAME).key(post.getMediaUrl()));
                } catch (Exception e) {
                    // Log lỗi nếu việc xóa tệp không thành công
                    System.err.println("Error deleting media from S3: " + e.getMessage());
                    // Có thể thêm cơ chế rollback ở đây nếu cần
                }
            }
        }
        postRepository.deleteAllByCreator_Id(userId);
        return "Deleted successfully";
    }

    private void generatePresignedUrlForPost(Post post) {
        if (post.getMediaUrl() != null) {
            post.setPresignedUrl(s3PresignedUrlService.generatePresignedUrl(post.getMediaUrl()));
        }
        if (post.getCreator() != null && post.getCreator().getId() != null) {
            post.getCreator().setProfilePhoto(s3PresignedUrlService.generatePresignedUrl(post.getCreator().getId() + "-profile"));
        }
    }

    public void likePost(String id) {
        postRepository.incrementLikes(id);
    }


    public Page<Post> searchPosts(String searchCriteria, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return postRepository.findByTitleContainingOrTextContainingOrTagsContaining(searchCriteria, searchCriteria, searchCriteria, pageable);
    }

    // Kiểm tra quyền sở hữu bài viết
    public boolean isUserOwnerOfPost(String postId, String userId) {
        Post post = postRepository.findById(postId).orElse(null); // Lấy bài viết từ repository
        return post != null && post.getCreator().getId().equals(userId); // Kiểm tra chủ sở hữu
    }
}

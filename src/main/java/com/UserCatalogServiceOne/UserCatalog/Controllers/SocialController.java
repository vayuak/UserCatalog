package com.UserCatalogServiceOne.UserCatalog.Controllers;

import com.UserCatalogServiceOne.UserCatalog.Models.Community;
import com.UserCatalogServiceOne.UserCatalog.Models.Post;
import com.UserCatalogServiceOne.UserCatalog.Repositories.CommunityRepository;
import com.UserCatalogServiceOne.UserCatalog.Repositories.PostRepository;
import com.UserCatalogServiceOne.UserCatalog.Services.UserServiceImpl;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@RestController
@RequestMapping("/api/social")
@RequiredArgsConstructor
public class SocialController {

    private final UserServiceImpl userService;
    private final PostRepository postRepository;
    private final CommunityRepository communityRepository;

    // 1. Like a Post
    @PostMapping("/post/{postId}/like")
    public ResponseEntity<String> likePost(@PathVariable Long postId, @RequestParam Integer userId) {
        Post post = postRepository.findById(postId).orElseThrow(() -> new RuntimeException("Post not found"));

        if (post.getLikes().contains(userId)) {
            post.getLikes().remove(userId);
            postRepository.save(post);
            return ResponseEntity.ok("Unliked");
        }

        post.getLikes().add(userId);
        postRepository.save(post);
        return ResponseEntity.ok("Liked");
    }

    // 2. Create Community
    @PostMapping("/community/create")
    public ResponseEntity<Community> createCommunity(@RequestBody Community community, @RequestParam Integer creatorId) {
        community.setCreatorId(creatorId);
        community.getMembers().add(creatorId);
        return ResponseEntity.status(HttpStatus.CREATED).body(communityRepository.save(community));
    }

    // 3. Join Community
    @PostMapping("/community/{communityId}/join")
    public ResponseEntity<String> joinCommunity(@PathVariable Long communityId, @RequestParam Integer userId) {
        Community community = communityRepository.findById(communityId).orElseThrow(
                () -> new RuntimeException("Community not found")
        );
        community.getMembers().add(userId);
        communityRepository.save(community);
        return ResponseEntity.ok("Joined " + community.getName());
    }

    // 4. Get Paginated City Feed
    @GetMapping("/feed")
    public ResponseEntity<Page<Post>> getCityFeed(
            @RequestParam String city,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Post> feed;

        if (category != null && !category.isEmpty()) {
            feed = postRepository.findByCityNameAndCategoryOrderByCreatedAtDesc(city, category, pageable);
        } else {
            feed = postRepository.findByCityNameOrderByCreatedAtDesc(city, pageable);
        }

        return ResponseEntity.ok(feed);
    }
}
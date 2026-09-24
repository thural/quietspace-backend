package dev.thural.quietspace.post;

import dev.thural.quietspace.post.dto.PostRequest;
import dev.thural.quietspace.post.dto.PostResponse;
import dev.thural.quietspace.post.dto.RepostRequest;
import dev.thural.quietspace.post.dto.VoteRequest;
import org.springframework.data.domain.Page;

import java.util.Optional;
import java.util.UUID;

public interface PostService {

    Page<PostResponse> getAllPosts(Integer pageNumber, Integer pageSize);

    PostResponse addPost(PostRequest post);

    Optional<PostResponse> getPostById(UUID id);

    PostResponse updatePost(UUID id, PostRequest post);

    void deletePost(UUID id);

    PostResponse patchPost(UUID id, PostRequest post);

    void votePoll(VoteRequest voteRequest);

    String getVotedPollOptionLabel(Poll poll);

    Page<PostResponse> getPostsByUserId(UUID userId, Integer pageNumber, Integer pageSize);

    Page<PostResponse> getAllByQuery(String query, Integer pageNumber, Integer pageSize);

    PostResponse addRepost(RepostRequest repost);

    Page<PostResponse> getSavedPostsByUser(Integer pageNumber, Integer pageSize);

    void savePostForUser(UUID postId);

    void unsavePostForUser(UUID postId);

    Page<PostResponse> getCommentedPostsByUserId(UUID userId, Integer pageNumber, Integer pageSize);
}

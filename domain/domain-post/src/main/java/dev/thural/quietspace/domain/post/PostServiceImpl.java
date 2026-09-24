package dev.thural.quietspace.post;

import dev.thural.quietspace.photo.Photo;
import dev.thural.quietspace.photo.PhotoService;
import dev.thural.quietspace.post.dto.PostRequest;
import dev.thural.quietspace.post.dto.PostResponse;
import dev.thural.quietspace.post.dto.RepostRequest;
import dev.thural.quietspace.post.dto.VoteRequest;
import dev.thural.quietspace.reaction.EntityType;
import dev.thural.quietspace.user.User;
import dev.thural.quietspace.user.UserService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static dev.thural.quietspace.shared.util.PagingProvider.buildPageRequest;

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostSpecifications postSpecifications;
    private final PostRepository postRepository;
    private final PhotoService photoService;
    private final UserService userService;
    private final PostMapper postMapper;

    public final String AUTHOR_MISMATCH_MESSAGE = "post author mismatch with current user";

    @Override
    public Page<PostResponse> getAllPosts(Integer pageNumber, Integer pageSize) {
        PageRequest pageRequest = buildPageRequest(pageNumber, pageSize, null);
        return postRepository.findAll(postSpecifications.visibleToUser(), pageRequest)
                .map(postMapper::postEntityToResponse);
    }

    @Override
    @Transactional
    public PostResponse addPost(PostRequest post) {
        User loggedUser = userService.getSignedUser();
        if (!loggedUser.getId().equals(post.getUserId())) throw new AccessDeniedException(AUTHOR_MISMATCH_MESSAGE);
        Post savedPost = postRepository.save(postMapper.postRequestToEntity(post));
        if (post.getPhotoData() != null) savePostPhoto(post, savedPost);
        return postMapper.postEntityToResponse(savedPost);
    }

    public String getVotedPollOptionLabel(Poll poll) {
        UUID userId = userService.getSignedUser().getId();
        return poll.getOptions().stream()
                .filter(option -> option.getVotes().contains(userId))
                .findAny().map(PollOption::getLabel).orElse("not voted");
    }

    @Override
    public Optional<PostResponse> getPostById(UUID postId) {
        Post post = findPostEntityById(postId);
        return Optional.of(postMapper.postEntityToResponse(post));
    }

    @Override
    @Transactional
    public PostResponse updatePost(UUID postId, PostRequest post) {
        User loggedUser = userService.getSignedUser();
        Post existingPost = findPostEntityById(postId);
        boolean postExistsByLoggedUser = isPostExistsByLoggedUser(existingPost, loggedUser);
        if (!postExistsByLoggedUser) throw new AccessDeniedException(AUTHOR_MISMATCH_MESSAGE);
        if (post.getPhotoData() == null) photoService.deletePhotoByEntityId(existingPost.getId());
        else savePostPhoto(post, existingPost);
        BeanUtils.copyProperties(post, existingPost);
        return postMapper.postEntityToResponse(existingPost);
    }

    @Override
    @Transactional
    public PostResponse patchPost(UUID postId, PostRequest post) {
        User loggedUser = userService.getSignedUser();
        Post existingPost = findPostEntityById(postId);
        boolean postExistsByLoggedUser = isPostExistsByLoggedUser(existingPost, loggedUser);
        if (!postExistsByLoggedUser) throw new AccessDeniedException(AUTHOR_MISMATCH_MESSAGE);
        if (StringUtils.hasText(post.getText())) existingPost.setText(post.getText());
        if (StringUtils.hasText(post.getTitle())) existingPost.setTitle(post.getTitle());
        if (post.getPhotoData() == null) photoService.deletePhotoByEntityId(existingPost.getId());
        else savePostPhoto(post, existingPost);
        return postMapper.postEntityToResponse(existingPost);
    }

    @Override
    @Transactional
    public void votePoll(VoteRequest voteRequest) {
        Post foundPost = postRepository.findById(voteRequest.getPostId()).orElseThrow(EntityNotFoundException::new);
        foundPost.votePoll(voteRequest.getUserId(), voteRequest.getOption());
    }

    @Override
    @Transactional
    public void deletePost(UUID postId) {
        User loggedUser = userService.getSignedUser();
        Post existingPost = findPostEntityById(postId);
        boolean postExistsByLoggedUser = isPostExistsByLoggedUser(existingPost, loggedUser);
        if (!postExistsByLoggedUser) throw new AccessDeniedException(AUTHOR_MISMATCH_MESSAGE);
        postRepository.deleteByRepostId(existingPost.getId().toString());
        postRepository.deleteById(postId);
        photoService.deletePhotoByEntityId(postId);
    }

    @Override
    public Page<PostResponse> getPostsByUserId(UUID userId, Integer pageNumber, Integer pageSize) {
        PageRequest pageRequest = buildPageRequest(pageNumber, pageSize, null);
        if (userId != null) {
            Specification<Post> specification = postSpecifications.visibleToUser()
                    .and((root, query, criteriaBuilder) ->
                            criteriaBuilder.equal(root.get("user").get("id"), userId));
            return postRepository.findAll(specification, pageRequest)
                    .map(postMapper::postEntityToResponse);
        } else {
            return postRepository.findAll(postSpecifications.visibleToUser(), pageRequest)
                    .map(postMapper::postEntityToResponse);
        }
    }

    @Override
    public Page<PostResponse> getAllByQuery(String searchText, Integer pageNumber, Integer pageSize) {
        PageRequest pageRequest = buildPageRequest(pageNumber, pageSize, null);
        Specification<Post> specification = postSpecifications.visibleToUser();
        if (StringUtils.hasText(searchText)) {
            specification = specification.and(postSpecifications.containsText(searchText));
        }
        return postRepository.findAll(specification, pageRequest)
                .map(postMapper::postEntityToResponse);
    }

    @Override
    @Transactional
    public PostResponse addRepost(RepostRequest repost) {
        Post repostEntity = postMapper.repostRequestToEntity(repost);
        return postMapper.postEntityToResponse(postRepository.save(repostEntity));
    }

    @Override
    @Transactional
    public Page<PostResponse> getSavedPostsByUser(Integer pageNumber, Integer pageSize) {
        PageRequest pageRequest = buildPageRequest(pageNumber, pageSize, null);
        UUID userId = userService.getSignedUser().getId();
        Specification<Post> specification = postSpecifications.visibleToUser()
                .and(postSpecifications.savedByUser(userId));
        return postRepository.findAll(specification, pageRequest)
                .map(postMapper::postEntityToResponse);
    }

    @Override
    @Transactional
    public void savePostForUser(UUID postId) {
        Post foundPost = findPostEntityById(postId);
        foundPost.saveBy(userService.getSignedUser());
    }

    @Override
    @Transactional
    public void unsavePostForUser(UUID postId) {
        Post foundPost = findPostEntityById(postId);
        foundPost.unsaveBy(userService.getSignedUser());
    }

    @Override
    public Page<PostResponse> getCommentedPostsByUserId(UUID userId, Integer pageNumber, Integer pageSize) {
        PageRequest pageRequest = buildPageRequest(pageNumber, pageSize, null);
        Specification<Post> specification = postSpecifications.visibleToUser()
                .and(postSpecifications.commentedByUser(userId));
        return postRepository.findAll(specification, pageRequest)
                .map(postMapper::postEntityToResponse);
    }

    private boolean isPostExistsByLoggedUser(Post existingPost, User loggedUser) {
        return existingPost.getUser().equals(loggedUser);
    }

    private Post findPostEntityById(UUID postId) {
        return postRepository.findById(postId).orElseThrow(EntityNotFoundException::new);
    }

    private void savePostPhoto(PostRequest post, Post savedPost) {
        Photo savedPhoto = photoService.persistPhotoEntity(post.getPhotoData(), savedPost.getId(), EntityType.POST);
        savedPost.setPhotoId(savedPhoto.getId());
    }
}

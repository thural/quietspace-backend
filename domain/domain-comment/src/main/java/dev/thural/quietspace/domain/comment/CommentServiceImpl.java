package dev.thural.quietspace.domain.comment;

import dev.thural.quietspace.domain.comment.dto.CommentRequest;
import dev.thural.quietspace.domain.comment.dto.CommentResponse;
import dev.thural.quietspace.domain.comment.port.CommentPostPort;
import dev.thural.quietspace.core.shared.exception.UnauthorizedException;
import dev.thural.quietspace.domain.user.User;
import dev.thural.quietspace.domain.user.UserService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Optional;
import java.util.UUID;

import static dev.thural.quietspace.core.shared.util.PagingProvider.BY_CREATED_DATE_ASC;
import static dev.thural.quietspace.core.shared.util.PagingProvider.buildPageRequest;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentMapper commentMapper;
    private final UserService userService;
    private final CommentRepository commentRepository;
    private final CommentPostPort postPort;

    @Override
    public Page<CommentResponse> getCommentsByPostId(UUID postId, Integer pageNumber, Integer pageSize) {
        PageRequest pageRequest = buildPageRequest(pageNumber, pageSize, BY_CREATED_DATE_ASC);
        return commentRepository.findAllByPostId(postId, pageRequest).map(commentMapper::commentEntityToResponse);
    }

    @Override
    public Page<CommentResponse> getCommentsByUserId(UUID userId, Integer pageNumber, Integer pageSize) {
        if (!userService.getSignedUser().getId().equals(userId))
            throw new UnauthorizedException("denied access to resource");
        PageRequest pageRequest = buildPageRequest(pageNumber, pageSize, null);
        return commentRepository.findAllByUserId(userId, pageRequest).map(commentMapper::commentEntityToResponse);
    }

    @Override
    public Optional<CommentResponse> getLatestCommentByUserIdAndPostId(UUID userId, UUID postId) {
        return commentRepository.findLatestCommentByPostAndUserByUpdateDate(postId, userId)
                .map(commentMapper::commentEntityToResponse);
    }

    @Override
    public CommentResponse createComment(CommentRequest comment) {
        User loggedUser = userService.getSignedUser();
        if (!loggedUser.getId().equals(comment.getUserId()))
            throw new UnauthorizedException("denied access to resource");
        if (!postPort.postExists(comment.getPostId()))
            throw new EntityNotFoundException("post does not exist");
        Comment commentEntity = commentMapper.commentRequestToEntity(comment);
        return commentMapper.commentEntityToResponse(commentRepository.save(commentEntity));
    }

    @Override
    public Optional<CommentResponse> getCommentById(UUID commentId) {
        return commentRepository.findById(commentId).map(commentMapper::commentEntityToResponse);
    }

    @Override
    @Transactional
    public CommentResponse updateComment(UUID commentId, CommentRequest comment) {
        User loggedUser = userService.getSignedUser();
        Comment existingComment = commentRepository.findById(commentId).orElseThrow(EntityNotFoundException::new);
        if (existingComment.getUserId().equals(loggedUser.getId()))
            throw new AccessDeniedException("comment author does not belong to current user");
        existingComment.setText(comment.getText());
        return commentMapper.commentEntityToResponse(existingComment);
    }

    @Override
    @Transactional
    public void deleteComment(UUID commentId) {
        User loggedUser = userService.getSignedUser();
        Comment existingComment = commentRepository.findById(commentId).orElseThrow(EntityNotFoundException::new);
        if (!existingComment.getUserId().equals(loggedUser.getId()))
            throw new AccessDeniedException("comment author does not belong to current user");
        if (existingComment.getParentId() == null)
            commentRepository.deleteAllByParentId(commentId);
        commentRepository.deleteById(commentId);
    }

    @Override
    public Page<CommentResponse> getRepliesByParentId(UUID commentId, Integer pageNumber, Integer pageSize) {
        PageRequest pageRequest = buildPageRequest(pageNumber, pageSize, null);
        return commentRepository.findAllByParentId(commentId, pageRequest).map(commentMapper::commentEntityToResponse);
    }

    @Override
    @Transactional
    public CommentResponse patchComment(UUID commentId, CommentRequest comment) {
        User loggedUser = userService.getSignedUser();
        Comment existingComment = commentRepository.findById(commentId).orElseThrow(EntityNotFoundException::new);
        if (!existingComment.getUserId().equals(loggedUser.getId()))
            throw new AccessDeniedException("comment author does not belong to current user");
        if (StringUtils.hasText(comment.getText())) existingComment.setText(comment.getText());
        return commentMapper.commentEntityToResponse(existingComment);
    }

}

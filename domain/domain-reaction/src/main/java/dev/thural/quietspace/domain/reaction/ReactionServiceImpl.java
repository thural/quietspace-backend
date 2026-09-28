package dev.thural.quietspace.domain.reaction;

import dev.thural.quietspace.core.shared.enums.EntityType;
import dev.thural.quietspace.core.shared.enums.ReactionType;
import dev.thural.quietspace.domain.reaction.dto.ReactionRequest;
import dev.thural.quietspace.domain.reaction.dto.ReactionResponse;
import dev.thural.quietspace.domain.user.User;
import dev.thural.quietspace.domain.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

import static dev.thural.quietspace.core.shared.enums.ReactionType.LIKE;
import static dev.thural.quietspace.core.shared.util.PagingProvider.DEFAULT_SORT_OPTION;
import static dev.thural.quietspace.core.shared.util.PagingProvider.buildPageRequest;

@Service
@RequiredArgsConstructor
public class ReactionServiceImpl implements ReactionService {

    private final ReactionRepository reactionRepository;
    private final ReactionMapper reactionMapper;
    private final UserService userService;

    @Override
    public void handleReaction(ReactionRequest reaction) {
        User user = userService.getSignedUser();
        Reaction foundReaction = reactionRepository.findByContentIdAndUserId(reaction.getContentId(), user.getId()).orElse(null);
        if (foundReaction == null) {
            reactionRepository.save(reactionMapper.reactionRequestToEntity(reaction));
        } else if (reaction.getReactionType().equals(foundReaction.getReactionType())) {
            reactionRepository.deleteById(foundReaction.getId());
        } else {
            foundReaction.setReactionType(reaction.getReactionType());
            reactionRepository.save(foundReaction);
        }
    }

    @Override
    public void addReaction(ReactionRequest reaction) {
        User user = userService.getSignedUser();
        Reaction foundReaction = reactionRepository.findByContentIdAndUserId(reaction.getContentId(), user.getId()).orElse(null);
        if (foundReaction == null) {
            reactionRepository.save(reactionMapper.reactionRequestToEntity(reaction));
        } else {
            foundReaction.setReactionType(reaction.getReactionType());
            reactionRepository.save(foundReaction);
        }
    }

    @Override
    public void removeReaction(UUID reactionId) {
        reactionRepository.deleteById(reactionId);
    }

    @Override
    public Optional<ReactionResponse> getUserReactionByContentId(UUID contentId) {
        User user = userService.getSignedUser();
        Optional<Reaction> userReaction = reactionRepository.findByContentIdAndUserId(contentId, user.getId());
        return userReaction.map(reactionMapper::reactionEntityToResponse);
    }

    @Override
    public Page<ReactionResponse> getReactionsByContentIdAndReactionType(UUID contentId, ReactionType reactionType, Integer pageNumber, Integer pageSize) {
        PageRequest pageRequest = buildPageRequest(pageNumber, pageSize, DEFAULT_SORT_OPTION);
        return reactionRepository.findAllByContentIdAndReactionType(contentId, LIKE, pageRequest)
                .map(reactionMapper::reactionEntityToResponse);
    }

    @Override
    public Integer countByContentIdAndReactionType(UUID contentId, ReactionType reactionType) {
        return reactionRepository.countByContentIdAndReactionType(contentId, reactionType);
    }

    @Override
    public Page<ReactionResponse> getReactionsByContentIdAndContentType(UUID contentId, EntityType type, Integer pageNumber, Integer pageSize) {
        PageRequest pageRequest = buildPageRequest(pageNumber, pageSize, DEFAULT_SORT_OPTION);
        return reactionRepository.findAllByContentIdAndContentType(contentId, type, pageRequest)
                .map(reactionMapper::reactionEntityToResponse);
    }

    @Override
    public Page<ReactionResponse> getReactionsByUserIdAndContentType(UUID userId, EntityType contentType, Integer pageNumber, Integer pageSize) {
        PageRequest pageRequest = buildPageRequest(pageNumber, pageSize, DEFAULT_SORT_OPTION);
        return reactionRepository.findAllByUserIdAndContentType(userId, contentType, pageRequest)
                .map(reactionMapper::reactionEntityToResponse);
    }

}

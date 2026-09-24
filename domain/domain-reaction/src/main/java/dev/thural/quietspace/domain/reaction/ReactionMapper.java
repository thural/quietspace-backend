package dev.thural.quietspace.domain.reaction;

import dev.thural.quietspace.domain.reaction.dto.ReactionRequest;
import dev.thural.quietspace.domain.reaction.dto.ReactionResponse;
import dev.thural.quietspace.domain.user.User;
import dev.thural.quietspace.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ReactionMapper {

    private final UserRepository userRepository;

    public Reaction reactionRequestToEntity(ReactionRequest request) {
        Reaction reaction = new Reaction();
        BeanUtils.copyProperties(request, reaction);
        reaction.setUsername(getUserNameById(request.getUserId()));
        return reaction;
    }

    public ReactionResponse reactionEntityToResponse(Reaction reaction) {
        ReactionResponse response = new ReactionResponse();
        BeanUtils.copyProperties(reaction, response);
        return response;
    }

    String getUserNameById(UUID userId) {
        return userRepository.findById(userId).map(User::getUsername).orElse(null);
    }

}
package dev.thural.quietspace.domain.user.service;

import dev.thural.quietspace.core.shared.exception.UserNotFoundException;
import dev.thural.quietspace.domain.user.service.CommonService;
import dev.thural.quietspace.domain.user.User;
import dev.thural.quietspace.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CommonServiceImpl implements CommonService {

    private final UserRepository userRepository;

    @Override
    public User getSignedUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) throw new UserNotFoundException("no authenticated user");
        String username = authentication.getName();
        return userRepository.findUserByUsername(username).orElseThrow(UserNotFoundException::new);
    }
}

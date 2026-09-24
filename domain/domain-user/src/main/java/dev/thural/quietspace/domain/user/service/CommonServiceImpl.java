package dev.thural.quietspace.shared.service.impl;

import dev.thural.quietspace.shared.exception.UserNotFoundException;
import dev.thural.quietspace.shared.service.CommonService;
import dev.thural.quietspace.user.User;
import dev.thural.quietspace.user.UserRepository;
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

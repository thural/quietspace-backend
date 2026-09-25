package dev.thural.quietspace.domain.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import dev.thural.quietspace.core.shared.entity.BaseEntity;
import dev.thural.quietspace.core.shared.enums.Role;
import dev.thural.quietspace.core.shared.enums.StatusType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.security.Principal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;


@Entity
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class User extends BaseEntity implements UserDetails, Principal {

    @NotBlank
    @Column(length = 32, unique = true)
    private String username;

    @NotBlank
    @Column(length = 32, unique = true)
    private String email;

    @NotBlank
    @JsonIgnore
    private String password;

    @JsonIgnore
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "photo_id", columnDefinition = "varchar(36)")
    private UUID photoId;

    @JsonIgnore
    @Builder.Default
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "user_saved_posts",
            joinColumns = @JoinColumn(name = "user_id")
    )
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "post_id", columnDefinition = "varchar(36)")
    private List<UUID> savedPostIds = new ArrayList<>();

    @JsonIgnore
    @Builder.Default
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    ProfileSettings profileSettings = new ProfileSettings();


    @JsonIgnore
    @ManyToMany
    @JoinTable(
            name = "user_followings",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "followings_id")
    )
    @Builder.Default
    private List<User> followings = new ArrayList<>();

    @JsonIgnore
    @Builder.Default
    @ManyToMany(mappedBy = "followings")
    private List<User> followers = new ArrayList<>();


    @JsonIgnore
    private String firstname;
    @JsonIgnore
    private String lastname;
    @JsonIgnore
    private OffsetDateTime dateOfBirth;
    @JsonIgnore
    private boolean accountLocked;
    @JsonIgnore
    private boolean enabled;
    @JsonIgnore
    private StatusType statusType;


    @NotNull
    private Role role;

    @Override
    @NonNull
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this.role.getAuthorities();
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    @NonNull
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !accountLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public String fullName() {
        return getFirstname() + " " + getLastname();
    }

    @Override
    public String getName() {
        return username;
    }

    public String getFullName() {
        return firstname + " " + lastname;
    }

    // Domain methods for follow/unfollow
    public void follow(User target) {
        if (this.equals(target)) {
            throw new IllegalArgumentException("Cannot follow yourself");
        }
        if (!followings.contains(target)) {
            followings.add(target);
            target.getFollowers().add(this);
        }
    }

    public void unfollow(User target) {
        if (this.equals(target)) {
            throw new IllegalArgumentException("Cannot unfollow yourself");
        }
        if (followings.remove(target)) {
            target.getFollowers().remove(this);
        }
    }

    public void block(User target) {
        if (this.equals(target)) {
            throw new IllegalArgumentException("Cannot block yourself");
        }
        getProfileSettings().blockUser(target);
    }

    public void unblock(User target) {
        if (this.equals(target)) {
            throw new IllegalArgumentException("Cannot unblock yourself");
        }
        getProfileSettings().unblockUser(target);
    }

    // Followers management (called by target user)
    void addFollower(User follower) {
        if (!followers.contains(follower)) {
            followers.add(follower);
        }
    }

    void removeFollower(User follower) {
        if (!followers.remove(follower)) {
            throw new IllegalArgumentException("User is not found in followers");
        }
    }

    @PreRemove
    void onRemove() {
        // TODO: remove photo associated
    }

    @PrePersist
    @PreUpdate
    void ensureProfileSettings() {
        if (profileSettings == null) {
            profileSettings = new ProfileSettings();
        }
        if (profileSettings.getUser() == null) {
            profileSettings.setUser(this);
        }
    }
}
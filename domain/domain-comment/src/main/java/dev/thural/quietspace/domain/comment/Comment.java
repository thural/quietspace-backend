package dev.thural.quietspace.domain.comment;

import com.fasterxml.jackson.annotation.JsonBackReference;
import dev.thural.quietspace.core.shared.entity.BaseEntity;
import dev.thural.quietspace.domain.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Entity
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class Comment extends BaseEntity implements Serializable {

    private UUID parentId;

    @NotBlank
    @Column(length = 999)
    private String text;

    @NotNull
    @ManyToOne
    @JsonBackReference
    private User user;

    @NotNull    @Column(name = "post_id", nullable = false, columnDefinition = "varchar(36)")
    private UUID postId;

}

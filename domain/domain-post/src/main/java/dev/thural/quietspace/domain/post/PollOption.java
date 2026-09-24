package dev.thural.quietspace.post;

import com.fasterxml.jackson.annotation.JsonBackReference;
import dev.thural.quietspace.shared.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class PollOption extends BaseEntity {

    @NotNull
    @ManyToOne
    @JsonBackReference
    private Poll poll;

    @NotBlank
    @Column(length = 999)
    private String label;

    @NotNull
    @Builder.Default
    @ElementCollection
    private Set<UUID> votes = new HashSet<>();

}

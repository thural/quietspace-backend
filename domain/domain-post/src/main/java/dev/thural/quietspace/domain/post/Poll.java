package dev.thural.quietspace.post;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import dev.thural.quietspace.shared.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.OffsetDateTime;
import java.util.List;

@Entity
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class Poll extends BaseEntity {

    private OffsetDateTime dueDate;

    @NotNull
    @OneToOne
    private Post post;

    @NotNull
    @JsonManagedReference
    @OneToMany(mappedBy = "poll", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PollOption> options;

}

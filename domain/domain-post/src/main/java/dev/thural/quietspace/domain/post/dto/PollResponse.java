package dev.thural.quietspace.post.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import dev.thural.quietspace.shared.model.BaseResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PollResponse extends BaseResponse {

    private String votedOption;
    private Integer voteCount;
    private List<OptionResponse> options;

}
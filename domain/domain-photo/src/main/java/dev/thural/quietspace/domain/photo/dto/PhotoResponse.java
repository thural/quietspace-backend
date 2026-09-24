package dev.thural.quietspace.photo.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import dev.thural.quietspace.shared.model.BaseResponse;
import jakarta.persistence.Lob;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PhotoResponse extends BaseResponse {

    private String name;
    private String type;
    @Lob
    private byte[] data;
    
}

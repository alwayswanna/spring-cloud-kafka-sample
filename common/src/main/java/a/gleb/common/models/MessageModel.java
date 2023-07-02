package a.gleb.common.models;


import jakarta.validation.constraints.NotEmpty;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageModel {

    private String login;

    @NotEmpty
    private String userMessage;
}

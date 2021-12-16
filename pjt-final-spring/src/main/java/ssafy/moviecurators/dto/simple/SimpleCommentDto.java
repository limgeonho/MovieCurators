package ssafy.moviecurators.dto.simple;

import lombok.Data;
import ssafy.moviecurators.domain.movies.Comment;

import java.time.OffsetDateTime;

@Data
public class SimpleCommentDto {

    private Long id;
    private String content;
    private String thanks_content;
    private Integer mileage;
    private OffsetDateTime created_at;
    private OffsetDateTime updated_at;

    public SimpleCommentDto(Comment comment) {
        this.id = comment.getId();
        this.content = comment.getContent();
        this.thanks_content = comment.getThanksContent();
        this.mileage = comment.getMileage();
        this.created_at = comment.getCreated();
        this.updated_at = comment.getUpdated();
    }
}

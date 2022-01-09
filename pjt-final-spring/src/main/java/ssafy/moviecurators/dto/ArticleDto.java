package ssafy.moviecurators.dto;

import lombok.Data;
import ssafy.moviecurators.domain.movies.Article;
import ssafy.moviecurators.dto.simple.SimpleCommentDto;
import ssafy.moviecurators.dto.simple.SimpleLikesDto;
import ssafy.moviecurators.dto.simple.SimpleMovieDto;
import ssafy.moviecurators.dto.simple.SimpleUserDto;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 평가 관련 DTO
 */
@Data
public class ArticleDto {

    private Long id;
    private Double rate;
    private String title;
    private String content;
    private Integer points;
    private OffsetDateTime created_at;
    private OffsetDateTime updated_at;

    private SimpleMovieDto movie;
    private SimpleUserDto user;
    private List<SimpleCommentDto> comments;
    private List<SimpleLikesDto> likes;

    public ArticleDto(Article article) {
        this.id = article.getId();
        this.rate = article.getRate();
        this.title = article.getTitle();
        this.content = article.getContent();
        this.points = article.getPoints();
        this.created_at = article.getCreated();
        this.updated_at = article.getUpdated();

        this.movie = new SimpleMovieDto(article.getMovie());
        this.user = new SimpleUserDto(article.getUser());

        /*
        this.comments = article.getComments()
                .stream().map(comment -> new SimpleCommentDto(comment)).collect(Collectors.toList());
        this.likes = article.getLikes()
                .stream().map(likes -> new SimpleLikesDto(likes)).collect(Collectors.toList());
         */
    }
}

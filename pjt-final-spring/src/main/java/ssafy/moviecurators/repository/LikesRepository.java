package ssafy.moviecurators.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ssafy.moviecurators.domain.accounts.User;
import ssafy.moviecurators.domain.movies.Article;
import ssafy.moviecurators.domain.movies.Comment;
import ssafy.moviecurators.domain.movies.Likes;

import java.util.List;

/**
 * 좋아요 관련 Repository, JpaRepository상속
 */
public interface LikesRepository extends JpaRepository<Likes, Long> {

    @Query("select l from Likes l " +
            "join fetch l.article a " +
            "join fetch l.user u " +
            "where a.id = :articleId and u.id = :userId")
    Likes likesDetail(@Param("articleId") Long articleId, @Param("userId") Long userId);

    List<Likes> findTop12ByUserOrderByIdDesc(User byId);
}

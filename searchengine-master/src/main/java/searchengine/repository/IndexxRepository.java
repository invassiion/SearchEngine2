package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.IndexxSearch;

@Repository
public interface IndexxRepository extends JpaRepository<IndexxSearch,Integer> {
    @Query(value = "select * from indexx t where t.page_id = :pageId and t.lemma_id = :lemmaId",nativeQuery = true)
    IndexxSearch indexSearchExist(@Param("pageId")Integer pageId, @Param("lemmaId")Integer lemmaId);

}

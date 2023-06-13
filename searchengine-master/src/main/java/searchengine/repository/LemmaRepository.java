package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.LemmaEntity;

@Repository
public interface LemmaRepository  extends JpaRepository<LemmaEntity,Integer> {
    @Query(value = "select * from lemma t where t.lemma = :lemma for update",nativeQuery = true)
    LemmaEntity lemmaExist(@Param("lemma") String lemma);
}

package searchengine.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.PageEntity;

@Repository
public interface PageRepository extends JpaRepository<PageEntity,Integer> {
    @Query(value = "select * from page t where t.site_id = :siteId and t.path = :path limit 1",nativeQuery = true)
    PageEntity findPageBySiteIdAndPath(@Param("path") String path, @Param("siteId") Integer siteId);
}

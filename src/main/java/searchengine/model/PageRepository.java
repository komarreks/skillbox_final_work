package searchengine.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface PageRepository extends JpaRepository<Page, Integer> {

    @Transactional
    void deleteBySite(Site site);

    @Transactional
    @Query("delete Page p where p.site = ?1")
    void deletePages(Site site);
    Optional<Page> findByPathAndSite(String path, Site site);

    Optional<Page> findByPath(String path);
}

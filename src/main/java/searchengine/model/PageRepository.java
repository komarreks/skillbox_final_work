package searchengine.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface PageRepository extends JpaRepository<Page, Integer> {

    @Transactional
    List<Page> deleteBySite(Site site);
    Optional<Page> findByPathAndSite(String path, Site site);
}

package searchengine.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface SiteRepository extends JpaRepository<Site, Integer> {
    @Query("select s from Site s where s.name = ?1")
    List<Site> findSite(String name);

    @Query("delete from Site s where s.name = ?1")
    public void deleteByName(String name);
}

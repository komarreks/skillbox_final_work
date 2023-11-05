package searchengine.parser;

import org.springframework.beans.factory.annotation.Autowired;
import searchengine.model.Site;
import searchengine.model.SiteRepository;
import searchengine.model.Status;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class SiteParserStarter extends Thread{

    private List<searchengine.config.Site> localSiteList;
    @Autowired
    private SiteRepository siteRepository;

    public SiteParserStarter(List<searchengine.config.Site> localSiteList, SiteRepository siteRepository){
        this.localSiteList = localSiteList;
        this.siteRepository = siteRepository;
    }

    @Override
    public void run() {
        localSiteList.forEach(s -> {
            Optional<Site> localSite = siteRepository.findByName(s.getName());

            if (localSite.isPresent()){
                searchengine.model.Site siteOnSQL = localSite.get();
                siteRepository.deleteById(siteOnSQL.getId());
            }

            searchengine.model.Site site = new searchengine.model.Site();
            site.setStatus(Status.INDEXING);
            site.setStatus_time(LocalDateTime.now());
            site.setUrl(s.getUrl());
            site.setName(s.getName());

            siteRepository.save(site);
        });
    }
}

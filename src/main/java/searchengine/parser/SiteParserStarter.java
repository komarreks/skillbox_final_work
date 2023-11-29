package searchengine.parser;

import org.springframework.beans.factory.annotation.Autowired;
import searchengine.model.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ForkJoinPool;

public class SiteParserStarter extends Thread{
    private List<searchengine.config.Site> localSiteList;
    @Autowired
    private SiteRepository siteRepository;
    @Autowired
    private PageRepository pageRepository;

    public SiteParserStarter(List<searchengine.config.Site> localSiteList, SiteRepository siteRepository, PageRepository pageRepository){
        this.localSiteList = localSiteList;
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
    }

    @Override
    public void run() {

        siteRepository.saveAll(createSiteList());
        List<Site> siteList = siteRepository.findAll();

        siteList.forEach(site -> {
            //new Thread(() -> startSiteIndexing(site)).start();
            startSiteIndexing(site);
        });
    }

    private void startSiteIndexing(Site site){
        pageRepository.deleteBySite(site);

        createPageFromRoot(site);

        String siteUrl = site.getUrl().replace("www.","");

        PagesParser pagesParser = new PagesParser(siteUrl,true ,site, pageRepository, siteRepository);

        TaskPool.addTask(pagesParser);

        ForkJoinPool forkJoinPool = new ForkJoinPool();
        Page page = forkJoinPool.invoke(pagesParser);

        site.setStatus(Status.INDEXED);

        siteRepository.save(site);
    }

    private List<Site> createSiteList(){
        List<Site> siteList = new ArrayList<>();
        localSiteList.forEach(s -> {
            Optional<Site> localSite = siteRepository.findByName(s.getName());

            if (localSite.isPresent()){
                searchengine.model.Site siteOnSQL = localSite.get();
                pageRepository.deleteBySite(siteOnSQL);
                siteRepository.deleteById(siteOnSQL.getId());
            }

            searchengine.model.Site site = new searchengine.model.Site();
            site.setStatus(Status.INDEXING);
            site.setStatus_time(LocalDateTime.now());
            site.setUrl(s.getUrl());
            site.setName(s.getName());

            siteList.add(site);});
        return siteList;
    }

    private void createPageFromRoot(Site site){
        Page page = new Page();
        page.setSite(site);
        page.setPath("/");
        page.setCode(200);
        page.setContent("html");

        pageRepository.save(page);
    }
}



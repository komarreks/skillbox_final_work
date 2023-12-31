package searchengine.parser;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import searchengine.config.DataSourceProperty;
import searchengine.model.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;

public class SiteParserStarter extends Thread{
    private List<searchengine.config.Site> localSiteList;
    @Autowired
    private SiteRepository siteRepository;
    @Autowired
    private PageRepository pageRepository;

    private DataSourceProperty datasource;

    public SiteParserStarter(List<searchengine.config.Site> localSiteList, SiteRepository siteRepository, PageRepository pageRepository, DataSourceProperty datasource){
        this.localSiteList = localSiteList;
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.datasource = datasource;
    }

    @Override
    public void run() {

        List<Site> siteList = createSiteList();

        ExecutorService ex = Executors.newFixedThreadPool(2);

        siteList.forEach(site -> {
            ex.execute(new ParserExecutor(site));
        });
    }

    class ParserExecutor implements Runnable{
        Site site;
        public ParserExecutor(Site site){
            this.site = site;
        }
        @Override
        public void run() {
            startSiteIndexing(site);
        }
    }

    private void startSiteIndexing(Site site){

        siteRepository.save(site);

        JdbcTemplate jdbcTemplate = new JdbcTemplate(datasource.getDataSourse());
        jdbcTemplate.update("delete from pages where pages.site_id = ?", site.getId());

        String siteUrl = site.getUrl().replace("www.","");

        PagesParser pagesParser = new PagesParser(siteUrl,true ,site, pageRepository, siteRepository);

        SiteLinkList siteLinkList = new SiteLinkList(site.getName());
        pagesParser.setSiteLink(siteLinkList);

        ForkJoinPool forkJoinPool = new ForkJoinPool();

        try {
            boolean isDone = forkJoinPool.invoke(pagesParser);
        }catch (Exception ex){
            site.setLast_error(ex.getMessage());
        }

        siteLinkList.clear();
        siteLinkList = null;

        site.setStatus_time(LocalDateTime.now());
        site.setStatus(Status.INDEXED);

        siteRepository.save(site);
    }

    private List<Site> createSiteList(){
        List<Site> siteList = new ArrayList<>();
        localSiteList.forEach(s -> {
            Site siteFromDB = siteRepository.findSite(s.getName());

            if (siteFromDB == null){
                siteFromDB = new searchengine.model.Site();
            }

            siteFromDB.setStatus(Status.INDEXING);
            siteFromDB.setStatus_time(LocalDateTime.now());

            siteList.add(siteFromDB);});
        return siteList;
    }
}



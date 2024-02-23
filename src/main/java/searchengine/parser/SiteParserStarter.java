package searchengine.parser;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import searchengine.config.DataSourceProperty;
import searchengine.model.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
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
    private static boolean isRunning = false;

    public SiteParserStarter(List<searchengine.config.Site> localSiteList, SiteRepository siteRepository, PageRepository pageRepository, DataSourceProperty datasource){
        this.localSiteList = localSiteList;
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.datasource = datasource;
    }

    public static boolean isRunning(){return isRunning;}

    @Override
    public void run() {

        isRunning = true;

        List<Site> siteList = createSiteList();

        ExecutorService ex = Executors.newFixedThreadPool(2);

        siteList.forEach(site -> {
            ex.execute(new ParserExecutor(site));
        });

        while (isRunning){
            if (ex.isTerminated()){
                isRunning = false;
            }
        }
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

        site.setLast_error("");

        PagesParser pagesParser = new PagesParser(siteUrl,true ,site, pageRepository, siteRepository);

        SiteLinkList siteLinkList = new SiteLinkList(site.getName());
        pagesParser.setSiteLink(siteLinkList);

        ForkJoinPool forkJoinPool = new ForkJoinPool();

        try {
            boolean isDone = forkJoinPool.invoke(pagesParser);
            forkJoinPool.shutdown();
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

        List<Site> siteListFromDB = siteRepository.findAll();
        HashMap<String, searchengine.config.Site> hashSite = new HashMap<>();

        localSiteList.forEach(s ->{
            hashSite.put(s.getName(),s);
        });

        siteListFromDB.forEach(s ->{
            if (hashSite.get(s.getName())==null){
                deleteSitePages(s);
                siteRepository.delete(s);
            }
        });

        localSiteList.forEach(s -> {
            List<Site> sitesListFromDB = siteRepository.findSite(s.getName());

            Site siteFromDB = null;

            if (sitesListFromDB.size()>1){
                sitesListFromDB.forEach(site -> {deleteSitePages(site);siteRepository.delete(site);});

            } else if (sitesListFromDB.size() == 1) {
                siteFromDB = sitesListFromDB.get(0);
            }

            if (siteFromDB == null){
                siteFromDB = new searchengine.model.Site();
                siteFromDB.setName(s.getName());
                siteFromDB.setUrl(s.getUrl());
                siteFromDB.setLast_error("");
            }

            siteFromDB.setStatus(Status.INDEXING);
            siteFromDB.setStatus_time(LocalDateTime.now());

            siteList.add(siteFromDB);});
        return siteList;
    }

    private void deleteSitePages(Site s){
        JdbcTemplate jdbcTemplate = new JdbcTemplate(datasource.getDataSourse());
        jdbcTemplate.update("delete from pages where pages.site_id = ?", s.getId());
    }
}



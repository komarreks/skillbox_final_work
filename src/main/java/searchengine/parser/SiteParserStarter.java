package searchengine.parser;

import org.springframework.jdbc.core.JdbcTemplate;
import searchengine.config.DataSourceProperty;
import searchengine.model.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;

public class SiteParserStarter extends Thread{
    private List<searchengine.config.Site> localSiteList;
    //@Autowired
    private SiteRepository siteRepository;
    //@Autowired
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

        siteRepository.saveAll(createSiteList());
        List<Site> siteList = siteRepository.findAll();

        siteList.forEach(site -> {
            new Thread(() -> startSiteIndexing(site)).start();
        });
    }

    private void startSiteIndexing(Site site){
        String siteUrl = site.getUrl().replace("www.","");

        PagesParser pagesParser = new PagesParser(siteUrl,true ,site, pageRepository, siteRepository);

//        TaskPool.addTask(pagesParser);

        ForkJoinPool forkJoinPool = new ForkJoinPool();

        try {
            Page page = forkJoinPool.invoke(pagesParser);
        }catch (Exception ex){
            site.setLast_error(ex.getMessage());
        }

        site.setStatus_time(LocalDateTime.now());
        site.setStatus(Status.INDEXED);

        siteRepository.save(site);
    }

    private List<Site> createSiteList(){
        List<Site> siteList = new ArrayList<>();
        localSiteList.forEach(s -> {
            Site siteFromDB = siteRepository.findSite(s.getName());

            if (siteFromDB != null){
                //siteRepository.delete(siteFromDB);


                JdbcTemplate jdbcTemplate = new JdbcTemplate(datasource.getDataSourse());
                jdbcTemplate.update("delete from pages where pages.site_id = ?", siteFromDB.getId());

                siteRepository.delete(siteFromDB);
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



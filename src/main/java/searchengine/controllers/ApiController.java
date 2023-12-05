package searchengine.controllers;

import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import searchengine.config.DataSourceProperty;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.model.PageRepository;
import searchengine.model.SiteRepository;
import searchengine.parser.SiteParserStarter;
import searchengine.services.StatisticsService;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;
    private final SitesList sitesList;

    private final DataSourceProperty datasource;

    @Autowired
    private SiteRepository siteRepository;
    @Autowired
    private PageRepository pageRepository;

    public ApiController(StatisticsService statisticsService, SitesList sitesList, DataSourceProperty datasource) {
        this.statisticsService = statisticsService;
        this.sitesList = sitesList;
        this.datasource = datasource;
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity startIndexing(){
        @Data
        class Answer{
            private boolean result;
            private String error;

            public Answer(boolean res, String err){
                result = res;
                error = err;
            }
        }

        List<Site> localSiteList = sitesList.getSites();

        SiteParserStarter siteParserStarter = new SiteParserStarter(localSiteList, siteRepository, pageRepository, datasource);

        siteParserStarter.start();

        Answer answer = new Answer(true, "");
        return new ResponseEntity(answer, HttpStatus.OK);
    }
}

package searchengine.controllers;

import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.model.SiteRepository;
import searchengine.model.Status;
import searchengine.services.StatisticsService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;
    private final SitesList sitesList;

    @Autowired
    private SiteRepository siteRepository;

    public ApiController(StatisticsService statisticsService, SitesList sitesList) {
        this.statisticsService = statisticsService;
        this.sitesList = sitesList;
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

        localSiteList.forEach(s -> {
            Optional<searchengine.model.Site> localSite = siteRepository.findByName(s.getName());

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

            int a=0;
        });

        Answer answer = new Answer(true, "");
        return new ResponseEntity(answer, HttpStatus.OK);
    }
}

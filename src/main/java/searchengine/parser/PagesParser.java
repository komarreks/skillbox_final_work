package searchengine.parser;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import searchengine.model.Page;
import searchengine.model.PageRepository;
import searchengine.model.Site;
import searchengine.model.SiteRepository;

import javax.xml.crypto.dsig.spec.XSLTTransformParameterSpec;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;

import static java.lang.Thread.sleep;

public class PagesParser extends RecursiveTask<Page> {
    private Site site;
    private String currentUrl;
    @Autowired
    private PageRepository pageRepository;
    @Autowired
    private SiteRepository siteRepository;
    private static ConcurrentSkipListSet<String> linkList = new ConcurrentSkipListSet<>();

    private boolean firstThread;

    public PagesParser(String currentUrl, boolean firstThread, Site site, PageRepository pageRepository, SiteRepository siteRepository){
        this.currentUrl = currentUrl;
        this.firstThread = firstThread;
        this.site = site;
        this.pageRepository = pageRepository;
        this.siteRepository = siteRepository;

    }

    @Override
    protected Page compute() {

        try {
            sleep(100);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        if(!TaskPool.isIndexing()){return null;}

        String siteUrl = site.getUrl().replace("www.","");
        if (!currentUrl.contains(siteUrl)){return null;}

        String urlWithoutDomain = deleteDomain(currentUrl);

        if (urlWithoutDomain.equals("/")){

            if (!firstThread){return null;}

            Document jsoupDoc = getJsoupDoc(currentUrl);
            List<String> linkList = getLinkList(jsoupDoc);

            List<PagesParser> taskList = new ArrayList<>();

            linkList.forEach(link -> {
                PagesParser pagesParser = new PagesParser(link,false,site,pageRepository,siteRepository);
                taskList.add(pagesParser);
                pagesParser.fork();
            });

            taskList.forEach(ForkJoinTask::join);

            return new Page();
        }else {
            if (!linkList.contains(currentUrl)){
                Page page = new Page();
                page.setSite(site);
                page.setPath(urlWithoutDomain);
                page.setCode(getCodeConnection(currentUrl));

                linkList.add(currentUrl);

                Document jsoupDoc = getJsoupDoc(currentUrl);
                if (jsoupDoc==null){return null;}

                page.setContent("html");

                List<String> linkList = getLinkList(jsoupDoc);

                List<PagesParser> taskList = new ArrayList<>();

                linkList.forEach(link -> {
                    PagesParser pagesParser = new PagesParser(link,false,site,pageRepository,siteRepository);
                    taskList.add(pagesParser);
                    pagesParser.fork();
                });

                List<Page> pageList = new ArrayList<>();

                taskList.forEach(task ->{

                    Page childPage = task.join();

                    if (!(childPage==null)){pageList.add(childPage);}
                });

                synchronized (pageRepository) {
                    if (!pageList.isEmpty()) {
                        pageRepository.saveAll(pageList);
                    }
                }
                return page;
            }
        }

        return null;
    }



    private int getCodeConnection(String rootUrl){
        int code = 200;
        Connection.Response response = null;

        try {
            response = Jsoup.connect(rootUrl).
                    userAgent("Mozilla/5.0 (Windows; U; WindowsNT5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6").
                    referrer("http://www.google.com").
                    ignoreHttpErrors(true).
                    execute();
            code = response.statusCode();

        }catch (Exception ex){
            code = 404;
        }

        return code;
    }

    private Document getJsoupDoc(String rootUrl){
        Document document = null;

        try {
            document = Jsoup.connect(rootUrl).
                    userAgent("Mozilla/5.0 (Windows; U; WindowsNT5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6").
                    referrer("http://www.google.com").
                    ignoreHttpErrors(true).
                    get();

        }catch (Exception ex){

        }

        return document;
    }

    private String deleteDomain(String url){
        int indexDoubleSlash = url.indexOf("//");
        int indexOneSlash = url.indexOf("/",indexDoubleSlash+2);

        if (indexOneSlash<0){return "/";}

        return url.substring(indexOneSlash);
    }

    private List<String> getLinkList(Document jsoupDoc){
        List<String> linkList = new ArrayList<>();

        if (jsoupDoc==null){return linkList;}

        Elements elements = jsoupDoc.select("a");

        for (Element element : elements){
            String absUrl = element.absUrl("href");

            if (!linkList.contains(absUrl) && isValidLink(absUrl)){
                linkList.add(absUrl);
            }
        }

        return linkList;
    }

    private Boolean isValidLink(String url){

        if (url.contains("?") || url.contains("@") || url.contains("#") || url.contains(".pdf")){
            return false;
        }

        return true;
    }
}

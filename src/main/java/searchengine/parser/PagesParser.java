package searchengine.parser;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.model.Page;
import searchengine.model.PageRepository;
import searchengine.model.Site;
import searchengine.model.SiteRepository;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import static java.lang.Thread.sleep;

public class PagesParser extends RecursiveTask<Boolean> {
    private Site site;
    private String currentUrl;
    //@Autowired
    private PageRepository pageRepository;
    //@Autowired
    private SiteRepository siteRepository;

    private boolean firstThread;

    private SiteLinkList siteLinkList;

    public PagesParser(String currentUrl, boolean firstThread, Site site, PageRepository pageRepository, SiteRepository siteRepository){
        this.currentUrl = currentUrl;
        this.firstThread = firstThread;
        this.site = site;
        this.pageRepository = pageRepository;
        this.siteRepository = siteRepository;

    }

    public void setSiteLink(SiteLinkList linkList){
        this.siteLinkList = linkList;
    }

    @Override
    protected Boolean compute() {

//        if(!TaskPool.isIndexing()){return null;}
        if (!isValidLink(currentUrl)){return false;}
        String siteUrl = new String(site.getUrl().replace("www.",""));
        if (!currentUrl.contains(siteUrl)){return false;}

        String urlWithoutDomain = new String(deleteDomain(currentUrl));

        if (urlWithoutDomain.equals("/")){

            if (!firstThread){return false;}

            Document jsoupDoc = getJsoupDoc(currentUrl);
            List<String> linkList = getLinkList(jsoupDoc);

            createPageFromRoot(site, jsoupDoc);

            List<PagesParser> taskList = new ArrayList<>();

            linkList.forEach(link -> {
                PagesParser pagesParser = new PagesParser(link,false,site,pageRepository,siteRepository);
                pagesParser.setSiteLink(siteLinkList);
                taskList.add(pagesParser);
                pagesParser.fork();
            });

            taskList.forEach(ForkJoinTask::join);

            return true;
        }else {
            if (!siteLinkList.linkAddeded(urlWithoutDomain)){

                Document jsoupDoc = getJsoupDoc(currentUrl);
                if (jsoupDoc==null){return false;}

                Page page = new Page();
                page.setSite(site);
                page.setPath(urlWithoutDomain);
                page.setCode(getCodeConnection(currentUrl));
                page.setContent(getTextHtml(jsoupDoc));

                siteLinkList.addLink(urlWithoutDomain);

                synchronized (pageRepository){
                    pageRepository.save(page);
                }

                List<String> linkList = getLinkList(jsoupDoc);

                List<PagesParser> taskList = new ArrayList<>();

                linkList.forEach(link -> {
                    PagesParser pagesParser = new PagesParser(link,false,site,pageRepository,siteRepository);
                    pagesParser.setSiteLink(siteLinkList);

                    taskList.add(pagesParser);
                    pagesParser.fork();
                });

                taskList.forEach(task ->{
                    boolean isDone = task.join();
                });
                return true;
            }
        }
        return false;
    }

    private void createPageFromRoot(Site site, Document jsoupDoc){
        Page page = new Page();
        page.setSite(site);
        page.setPath("/");
        page.setCode(200);
        page.setContent(getTextHtml(jsoupDoc));

        pageRepository.save(page);
    }
    private String getTextHtml(Document jsoupDoc){
        String regex = "[\n\"]";
        return jsoupDoc.html().replaceAll(regex,"");
//        return "";
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
            return null;
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
            String absUrl = new String(element.absUrl("href"));

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

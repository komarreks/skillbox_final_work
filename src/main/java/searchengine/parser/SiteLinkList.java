package searchengine.parser;

import java.util.concurrent.ConcurrentSkipListSet;

public class SiteLinkList {
    private String name;
    private ConcurrentSkipListSet<String> linkList;

    public SiteLinkList(String name){
        this.name = name;
        linkList = new ConcurrentSkipListSet<>();
    }

    public void addLink(String link){
        linkList.add(link);
    }

    public void clear(){
        linkList.clear();
    }

    public boolean linkAddeded(String link){
        if (linkList.contains(link)){
            return true;
        }
        return false;
    }
}

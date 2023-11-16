package searchengine.parser;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class TaskPool {
    @Getter
    private static boolean indexing = true;
    private static CopyOnWriteArrayList<PagesParser> pagesParsers = new CopyOnWriteArrayList<>();

    public static void setIndexing(boolean doIndexing){
        indexing = doIndexing;

        if(!indexing){
            pagesParsers.forEach(pagesParser -> {
                pagesParser.cancel(true);
            });
        }
    }

    public static void addTask(PagesParser pagesParser) {
        pagesParsers.add(pagesParser);
    }
}

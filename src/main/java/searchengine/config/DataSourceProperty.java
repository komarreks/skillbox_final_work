package searchengine.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "spring")
public class DataSourceProperty {
    @Value("${spring.datasource.username}")
    private String username;
    @Value("${spring.datasource.password}")
    private String password;
    @Value("${spring.datasource.url}")
    private String url;

    public DriverManagerDataSource getDataSourse(){
        DriverManagerDataSource dataSource = new DriverManagerDataSource();

        int index = url.indexOf("?");
        String urlDs = url.substring(0,index);

        dataSource.setUrl(urlDs);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        return dataSource;
    }
}

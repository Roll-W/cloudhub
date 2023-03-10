package org.huel.cloudhub.client.configuration;

import org.huel.cloudhub.client.configuration.properties.DirectoriesProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author RollW
 */
@Configuration
public class DirectoryConfiguration {

    @Bean
    public DirectoriesProperties configureDirectories() {
        DirectoriesProperties properties = new DirectoriesProperties();
        properties.setCacheDirectory("cache");
        properties.setTempDirectory("temp");

        return properties;
    }
}

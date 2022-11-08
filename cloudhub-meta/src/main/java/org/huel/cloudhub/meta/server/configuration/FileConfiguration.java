package org.huel.cloudhub.meta.server.configuration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author RollW
 */
@Configuration
@EnableConfigurationProperties(FileProperties.class)
public class FileConfiguration {
}

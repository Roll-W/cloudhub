package org.huel.cloudhub.client.configuration;

import com.fasterxml.jackson.databind.MapperFeature;
import org.huel.cloudhub.client.configuration.json.ErrorCodeDeserializer;
import org.huel.cloudhub.client.configuration.json.ErrorCodeSerializer;
import org.huel.cloudhub.common.ErrorCode;
import org.huel.cloudhub.common.ErrorCodeFinderChain;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @author RollW
 */
@Configuration
public class WebMvcConfiguration implements WebMvcConfigurer {
    @Bean
    public Jackson2ObjectMapperBuilder jackson2ObjectMapperBuilder(ErrorCodeFinderChain finderChain) {
        ErrorCodeDeserializer errorCodeDeserializer = new ErrorCodeDeserializer(finderChain);
        ErrorCodeSerializer errorCodeSerializer = new ErrorCodeSerializer();

        return Jackson2ObjectMapperBuilder
                .json()
                .featuresToEnable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
                .serializerByType(ErrorCode.class, errorCodeSerializer)
                .deserializerByType(ErrorCode.class, errorCodeDeserializer);
    }

}

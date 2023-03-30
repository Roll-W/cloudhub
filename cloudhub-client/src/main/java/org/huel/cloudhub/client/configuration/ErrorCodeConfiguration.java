package org.huel.cloudhub.client.configuration;

import org.huel.cloudhub.client.service.object.ObjectErrorCode;
import org.huel.cloudhub.common.ErrorCodeFinderChain;
import org.huel.cloudhub.common.ErrorCodeMessageProvider;
import org.huel.cloudhub.common.IoErrorCode;
import org.huel.cloudhub.web.AuthErrorCode;
import org.huel.cloudhub.web.DataErrorCode;
import org.huel.cloudhub.web.ErrorCodeMessageProviderImpl;
import org.huel.cloudhub.web.UserErrorCode;
import org.huel.cloudhub.web.WebCommonErrorCode;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author RollW
 */
@Configuration
public class ErrorCodeConfiguration {

    @Bean
    public ErrorCodeMessageProvider errorCodeMessageProvider(MessageSource messageSource) {
        return new ErrorCodeMessageProviderImpl(messageSource);
    }

    @Bean
    public ErrorCodeFinderChain errorCodeFinderChain() {
        return ErrorCodeFinderChain.start(
                WebCommonErrorCode.getFinderInstance(),
                AuthErrorCode.getFinderInstance(),
                DataErrorCode.getFinderInstance(),
                IoErrorCode.getFinderInstance(),
                UserErrorCode.getFinderInstance(),
                ObjectErrorCode.getFinderInstance()
        );
    }

}

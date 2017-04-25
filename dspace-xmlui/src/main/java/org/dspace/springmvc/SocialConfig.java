package org.dspace.springmvc;

import org.apache.log4j.Logger;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.services.ConfigurationService;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.utils.DSpace;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.social.UserIdSource;
import org.springframework.social.config.annotation.ConnectionFactoryConfigurer;
import org.springframework.social.config.annotation.EnableSocial;
import org.springframework.social.config.annotation.SocialConfigurer;
import org.springframework.social.connect.ConnectionFactoryLocator;
import org.springframework.social.connect.ConnectionRepository;
import org.springframework.social.connect.UsersConnectionRepository;
import org.springframework.social.connect.jdbc.JdbcUsersConnectionRepository;
import org.springframework.social.connect.web.ConnectController;
import org.springframework.social.google.api.Google;
import org.springframework.social.google.connect.GoogleConnectionFactory;

import java.sql.SQLException;

/**
 * Created by ondra on 21.4.17.
 */
@Configuration
@EnableSocial
public class SocialConfig implements SocialConfigurer {

    Logger log = Logger.getLogger(SocialConfig.class);

    ConfigurationService configurationService = new DSpace().getConfigurationService();

    @Override
    public void addConnectionFactories(ConnectionFactoryConfigurer connectionFactoryConfigurer, Environment environment) {
        String clientId = configurationService.getProperty("social.google.clientId");
        String clientSecret = configurationService.getProperty("social.google.clientSecret");
        connectionFactoryConfigurer.addConnectionFactory(new GoogleConnectionFactory(clientId, clientSecret));
    }

    @Override
    public UserIdSource getUserIdSource() {
        return new UserIdSource() {
            @Override
            public String getUserId() {
                try {
                    Context context = ContextUtil.obtainContext(new DSpace().getRequestService().getCurrentRequest().getHttpServletRequest());
                    EPerson ePerson = context.getCurrentUser();
                    if(ePerson != null){
                        return Integer.toString(ePerson.getID());
                    }
                }catch (SQLException e){
                    log.error(e);
                }
                throw new IllegalStateException("Unable to get a ConnectionRepository: no user signed in");
            }
        };
    }

    @Override
    public UsersConnectionRepository getUsersConnectionRepository(ConnectionFactoryLocator connectionFactoryLocator) {
        return new JdbcUsersConnectionRepository(DatabaseManager.getDataSource(), connectionFactoryLocator, Encryptors.noOpText());
    }

    @Bean
    public ConnectController connectController(ConnectionFactoryLocator connectionFactoryLocator, ConnectionRepository connectionRepository){
        ConnectController controller = new ConnectController(connectionFactoryLocator, connectionRepository);
        controller.setApplicationUrl(configurationService.getProperty("dspace.url"));
        return controller;
    }

    @Bean
    @Scope(value="request", proxyMode = ScopedProxyMode.INTERFACES)
    public Google google(ConnectionRepository connectionRepository){
        return connectionRepository.getPrimaryConnection(Google.class).getApi();
    }

}

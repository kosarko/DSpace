package org.dspace.springmvc;

import org.apache.log4j.Logger;
import org.dspace.app.xmlui.aspect.submission.submit.UploadStep;
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
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.social.UserIdSource;
import org.springframework.social.config.annotation.ConnectionFactoryConfigurer;
import org.springframework.social.config.annotation.EnableSocial;
import org.springframework.social.config.annotation.SocialConfigurer;
import org.springframework.social.connect.Connection;
import org.springframework.social.connect.ConnectionFactoryLocator;
import org.springframework.social.connect.ConnectionRepository;
import org.springframework.social.connect.UsersConnectionRepository;
import org.springframework.social.connect.jdbc.JdbcUsersConnectionRepository;
import org.springframework.social.connect.web.ConnectController;
import org.springframework.social.connect.web.GenericConnectionStatusView;
import org.springframework.social.google.api.Google;
import org.springframework.social.google.connect.GoogleConnectionFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.view.AbstractView;
import org.springframework.web.servlet.view.BeanNameViewResolver;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Map;

/**
 * Created by ondra on 21.4.17.
 */
@Configuration
@EnableSocial
@EnableTransactionManagement
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

    @Bean
    public DataSource dataSource(){
        return DatabaseManager.getDataSource();
    }

    @Bean
    public PlatformTransactionManager txManager() {
          return new DataSourceTransactionManager(dataSource());
    }

    @Override
    public UsersConnectionRepository getUsersConnectionRepository(ConnectionFactoryLocator connectionFactoryLocator) {
        return new JdbcUsersConnectionRepository(dataSource(), connectionFactoryLocator, Encryptors.noOpText());
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
        Connection<Google> googleConnection = connectionRepository.getPrimaryConnection(Google.class);
        if(googleConnection.hasExpired()){
            googleConnection.refresh();
        }
        return googleConnection.getApi();
    }

    @Bean
    public ViewResolver beanViewResolver(){
        BeanNameViewResolver viewResolver = new BeanNameViewResolver();
        viewResolver.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return viewResolver;
    }

    /*
    @Bean(name={"connect/googleConnect", "connect/googleConnected", "connect/status"})
    public View googleConnectView() {
        return new GenericConnectionStatusView("google", "Google");
    }
    */
    @Bean(name={"connect/googleConnected"})
    public View googleConnectView(){
        return new AbstractView() {
            @Override
            protected void renderMergedOutputModel(Map<String, Object> model, HttpServletRequest request, HttpServletResponse response) throws Exception {
                String url = (String) request.getSession().getAttribute(UploadStep.RETURN_TO);
                response.sendRedirect(url);
            }
        };
    }

}

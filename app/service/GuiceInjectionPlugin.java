package service;

import java.util.Set;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import models.User;

import org.jcrom.Jcrom;

import com.google.common.collect.ImmutableSet;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.wingnest.play2.jackrabbit.Jcr;
import com.wingnest.play2.jackrabbit.plugin.ConfigConsts;

import play.Application;
import play.Plugin;
import providers.JackrabbitEmailPasswordAuthProvider;
import service.filestore.FileStore;

public class GuiceInjectionPlugin extends Plugin {

  private final Application application;
  private Injector injector = null;

  public GuiceInjectionPlugin(Application application) {
    this.application = application;
  }

  @Override
  public void onStart() {
    injector = createInjector();
  }

  @Override
  public void onStop() {
    injector = null;
  }

  public Injector getInjector() {
    return injector;
  }

  public static Injector getInjector(Application application) {
    return application.plugin(GuiceInjectionPlugin.class).getInjector();
  }

  private Injector createInjector() {
    Module pluginModule = new AbstractModule() {
      @Override
      protected void configure() {}

      @Provides
      SimpleUserServicePlugin getUserService() {
        return application.plugin(SimpleUserServicePlugin.class);
      }

      @Provides
      JackrabbitEmailPasswordAuthProvider getEmailPasswordAuthProvider() {
        return application.plugin(JackrabbitEmailPasswordAuthProvider.class);
      }

    };
    Module sessionModule = new AbstractModule() {
      @Override
      protected void configure() {
        JcrSessionFactory sessionFactory = new JcrSessionFactory() {
          @Override
          public Session newAdminSession() {
            try {
            return Jcr.login(
                cfgStr(ConfigConsts.CONF_JCR_USERID),
                cfgStr(ConfigConsts.CONF_JCR_PASSWORD));
            } catch (RepositoryException e) {
              throw new RuntimeException(e);
            }
          }

          private String cfgStr(String key) {
            return application.configuration().getString(key);
          }
        };
        bind(JcrSessionFactory.class).toInstance(sessionFactory);
        bind(FileStore.class).toInstance(new FileStore(sessionFactory));
      }
    };
    Module jcromModule = new AbstractModule() {
      @Override
      protected void configure() {}

      @Provides
      Jcrom getJcrom() {
        final Set<Class<?>> classes = ImmutableSet.<Class<?>>builder()
            .add(User.class)
            .build();
        return new Jcrom(classes);
      }
    };
    return Guice.createInjector(pluginModule, sessionModule, jcromModule);
  }

}

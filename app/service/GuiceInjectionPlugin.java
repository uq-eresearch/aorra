package service;

import javax.jcr.Credentials;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import models.User;
import notification.NotificationManager;

import org.apache.jackrabbit.core.TransientRepository;
import org.jcrom.Jcrom;

import play.Application;
import play.Plugin;
import providers.CacheableUserProvider;
import providers.DeadboltHandlerImpl;
import providers.JackrabbitEmailPasswordAuthProvider;
import service.filestore.FileStore;
import service.filestore.FileStoreImpl;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.wingnest.play2.jackrabbit.Jcr;
import com.wingnest.play2.jackrabbit.plugin.ConfigConsts;

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
      protected final TransientRepository repository =
          new TransientRepository();

      @Override
      protected void configure() {
        final Credentials adminCredentials = new SimpleCredentials(
            cfgStr(ConfigConsts.CONF_JCR_USERID),
            cfgStr(ConfigConsts.CONF_JCR_PASSWORD).toCharArray());

        JcrSessionFactory sessionFactory = new JcrSessionFactory() {
          @Override
          public Session newAdminSession() throws RepositoryException {
            return Jcr.getRepository().login(adminCredentials);
          }
        };
        bind(JcrSessionFactory.class).toInstance(sessionFactory);
        bind(FileStore.class).to(FileStoreImpl.class).in(Singleton.class);
        bind(CacheableUserProvider.class)
          .to(DeadboltHandlerImpl.class)
          .in(Singleton.class);
        bind(NotificationManager.class).in(Singleton.class);
      }

      private String cfgStr(String key) {
        return application.configuration().getString(key);
      }
    };
    Module jcromModule = new AbstractModule() {
      @Override
      protected void configure() {}

      @Provides
      Jcrom getJcrom() {
        final Jcrom jcrom = new Jcrom(false, true);
        jcrom.map(User.class);
        jcrom.map(models.filestore.File.class);
        jcrom.map(models.filestore.Folder.class);
        return jcrom;
      }
    };
    return Guice.createInjector(pluginModule, sessionModule, jcromModule);
  }

}

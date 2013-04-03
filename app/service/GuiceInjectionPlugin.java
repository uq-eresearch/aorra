package service;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.wingnest.play2.jackrabbit.Jcr;
import com.wingnest.play2.jackrabbit.plugin.ConfigConsts;

import play.Application;
import play.Plugin;

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
      JackrabbitUserService getUserService() {
        return application.plugin(JackrabbitUserService.class);
      }

    };
    Module sessionModule = new AbstractModule() {
      @Override
      protected void configure() {
        bind(JcrSessionFactory.class).toInstance(new JcrSessionFactory() {
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
        });
      }
    };
    return Guice.createInjector(pluginModule, sessionModule);
  }

}

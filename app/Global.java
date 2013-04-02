import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.wingnest.play2.jackrabbit.Jcr;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import play.GlobalSettings;
import service.JcrSessionFactory;
import service.ContributionFolderProvider;
import service.FreeformFileStore;
import service.JackrabbitUserService;
import com.wingnest.play2.jackrabbit.plugin.ConfigConsts;
import static play.Play.application;

public class Global extends GlobalSettings {

  /*
   * Dependency injection setup from: http://git.io/CgkboA
   */
  public static final Injector INJECTOR = createInjector();

  @Override
  public <A> A getControllerInstance(Class<A> controllerClass)
      throws Exception {
    return INJECTOR.getInstance(controllerClass);
  }

  private static Injector createInjector() {
    Module pluginModule = new AbstractModule() {
      @Override
      protected void configure() {}

      @Provides
      JackrabbitUserService getUserService() {
        return application().plugin(JackrabbitUserService.class);
      }

      @Provides
      ContributionFolderProvider getContributionFolderProvider() {
        return application().plugin(FreeformFileStore.class);
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
            return application().configuration().getString(key);
          }
        });
      }
    };
    return Guice.createInjector(pluginModule, sessionModule);
  }

}
package service;

import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import models.User;

import org.apache.jackrabbit.core.TransientRepository;
import org.jcrom.Jcrom;

import play.Application;
import play.Plugin;
import play.api.libs.JNDI;
import play.libs.Akka;
import play.libs.F;
import providers.CacheableUserProvider;
import providers.CacheableUserProviderImpl;
import providers.JackrabbitEmailPasswordAuthProvider;
import service.filestore.CommentStore;
import service.filestore.CommentStoreImpl;
import service.filestore.FileStore;
import service.filestore.FileStoreImpl;
import service.filestore.FlagStore;
import akka.actor.TypedActor;
import akka.actor.TypedProps;
import charts.builder.CachedChartBuilder;
import charts.builder.ChartBuilder;

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
    final Jcrom jcrom = buildJcrom();
    final Credentials adminCredentials = new SimpleCredentials(
        cfgStr(ConfigConsts.CONF_JCR_USERID),
        cfgStr(ConfigConsts.CONF_JCR_PASSWORD).toCharArray());
    registerRepoInJNDI(Jcr.getRepository());
    final Module eventManagerModule = new AbstractModule() {
      @Override
      protected void configure() {
        final EventManager em =
          TypedActor.get(Akka.system()).typedActorOf(
              new TypedProps<EventManagerImpl>(
                  EventManager.class, EventManagerImpl.class));
        bind(EventManager.class).toInstance(em);
      }
    };
    final JcrSessionFactory sessionFactory = new JcrSessionFactory() {
      @Override
      public Session newAdminSession() {
        try {
          return Jcr.getRepository().login(adminCredentials);
        } catch (RepositoryException e) {
          throw new RuntimeException();
        }
      }
    };
    final Module sessionModule = new AbstractModule() {
      protected final TransientRepository repository =
          new TransientRepository();

      @Override
      protected void configure() {
        bind(Jcrom.class).toInstance(jcrom);
        bind(JcrSessionFactory.class).toInstance(sessionFactory);
        bind(FileStore.class)
          .to(FileStoreImpl.class)
          .asEagerSingleton();
        sessionFactory.inSession(new F.Function<Session, Session>() {
          @Override
          public Session apply(Session session) throws Throwable {
            bind(FlagStore.class)
              .toInstance(new FlagStore(jcrom, session));
            bind(CommentStore.class)
              .toInstance(new CommentStoreImpl(jcrom, session));
            return session;
          }
        });
        bind(CacheableUserProvider.class)
          .to(CacheableUserProviderImpl.class)
          .in(Singleton.class);
        bind(ChartBuilder.class)
          .to(CachedChartBuilder.class)
          .in(Singleton.class);
      }
    };

    final Module pluginModule = new AbstractModule() {
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
    return Guice.createInjector(
        eventManagerModule, pluginModule, sessionModule);
  }

  private void registerRepoInJNDI(Repository repo) {
    try {
      InitialContext ic = JNDI.initialContext();
      ic.rebind("/jackrabbit", repo);
    } catch (NamingException ne) {
      throw new RuntimeException(ne);
    }
  }

  private Jcrom buildJcrom() {
    final Jcrom jcrom = new Jcrom(false, true);
    jcrom.map(User.class);
    jcrom.map(models.Comment.class);
    jcrom.map(models.Flag.class);
    jcrom.map(models.Notification.class);
    jcrom.map(models.filestore.File.class);
    jcrom.map(models.filestore.Folder.class);
    return jcrom;
  }

  private String cfgStr(String key) {
    return application.configuration().getString(key);
  }

}

import static play.Play.application;
import jackrabbit.AorraAccessManager;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.security.AccessControlManager;

import play.Application;
import play.GlobalSettings;
import play.libs.F.Function;
import play.mvc.Call;
import service.GuiceInjectionPlugin;
import service.JcrSessionFactory;

import com.feth.play.module.pa.PlayAuthenticate;
import com.feth.play.module.pa.PlayAuthenticate.Resolver;
import com.feth.play.module.pa.exceptions.AccessDeniedException;
import com.feth.play.module.pa.exceptions.AuthException;
import com.google.inject.Injector;

public class Global extends GlobalSettings {

  @Override
  public <A> A getControllerInstance(Class<A> controllerClass)
      throws Exception {
    return getInjector().getInstance(controllerClass);
  }

  private Injector getInjector() {
    return GuiceInjectionPlugin.getInjector(application());
  }

  private JcrSessionFactory sessionFactory() {
      return GuiceInjectionPlugin.getInjector(application())
                                 .getInstance(JcrSessionFactory.class);
    }

  @Override
  public void onStart(final Application app) {

    PlayAuthenticate.setResolver(new Resolver() {

      @Override
      public Call login() {
        // Your login page
        return controllers.routes.Application.login();
      }

      @Override
      public Call afterAuth() {
        // The user will be redirected to this page after authentication
        // if no original URL was saved
        return controllers.routes.FileStoreController.index();
      }

      @Override
      public Call afterLogout() {
        return controllers.routes.FileStoreController.index();
      }

      @Override
      public Call auth(final String provider) {
        // You can provide your own authentication implementation,
        // however the default should be sufficient for most cases
        return com.feth.play.module.pa.controllers.routes.Authenticate
            .authenticate(provider);
      }

      @Override
      public Call onException(final AuthException e) {
        if (e instanceof AccessDeniedException) {
          return controllers.routes.Application
              .oAuthDenied(((AccessDeniedException) e)
                  .getProviderKey());
        }

        // more custom problem handling here...

        return super.onException(e);
      }

      @Override
      public Call askLink() {
        // We don't support moderated account linking in this sample.
        // See the play-authenticate-usage project for an example
        return null;
      }

      @Override
      public Call askMerge() {
        // We don't support moderated account merging in this sample.
        // See the play-authenticate-usage project for an example
        return null;
      }
    });
    sessionFactory().inSession(new Function<Session, String>() {
        public String apply(Session session) throws UnsupportedRepositoryOperationException, RepositoryException {
            final AccessControlManager acm = session.getAccessControlManager();
            ((AorraAccessManager)acm).initStore(session);
            return null;
        }
      });
  }

}

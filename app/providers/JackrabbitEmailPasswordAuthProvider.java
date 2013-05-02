package providers;

import static play.test.Helpers.fakeRequest;
import static java.util.Collections.emptyMap;
import static play.data.Form.form;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.jcr.Session;

import org.jcrom.Jcrom;

import models.User;
import models.User.Invite;
import models.User.Login;
import models.UserDAO;
import play.Application;
import play.Logger;
import play.Play;
import play.api.http.MediaRange;
import play.data.Form;
import play.i18n.Lang;
import play.libs.F;
import play.libs.Scala;
import play.mvc.Call;
import play.mvc.Http;
import play.mvc.Http.Context;
import play.mvc.Http.Cookies;
import play.mvc.Http.Request;
import play.mvc.Http.RequestBody;
import scala.collection.Seq;
import service.GuiceInjectionPlugin;
import service.JcrSessionFactory;

import com.feth.play.module.mail.Mailer.Mail.Body;
import com.feth.play.module.pa.providers.password.UsernamePasswordAuthProvider;
import com.feth.play.module.pa.providers.password.UsernamePasswordAuthUser;
import com.google.common.collect.ImmutableMap;

public class JackrabbitEmailPasswordAuthProvider
    extends
    UsernamePasswordAuthProvider<String, JackrabbitEmailPasswordAuthProvider.LoginUser, JackrabbitEmailPasswordAuthProvider.SignupUser, Login, Invite> {

  private final Application application;

  public JackrabbitEmailPasswordAuthProvider(Application application) {
    super(application);
    this.application = application;
  }

  private JcrSessionFactory getSessionFactory() {
    return GuiceInjectionPlugin.getInjector(application).getInstance(
        JcrSessionFactory.class);
  }

  private Jcrom getJcrom() {
    return GuiceInjectionPlugin.getInjector(application).getInstance(
        Jcrom.class);
  }

  private UserDAO getUserDAO(Session session) {
    return new UserDAO(session, getJcrom());
  }

  public static class SignupUser extends UsernamePasswordAuthUser {
    private static final long serialVersionUID = 1L;

    public final String name;

    public SignupUser(final String email, final String name) {
      super(null, email);
      this.name = name;
    }

    public User asNewUser() {
      User user = new User();
      user.setEmail(getEmail());
      user.setName(name);
      user.setVerified(false);
      return user;
    }

  }

  public static class LoginUser extends UsernamePasswordAuthUser {
    private static final long serialVersionUID = 1L;

    public LoginUser(final String clearPassword, final String email) {
      super(clearPassword, email);
    }

    @Override
    public String getId() {
      return getEmail();
    }
  }

  public void signup(Invite invite) {
    Map<String, String> data = ImmutableMap.<String, String>builder()
        .put("email", invite.email)
        .put("name", invite.name)
        .build();
    handleSignup(fakeContext(data));
  }

  private Http.Context fakeContext(Map<String,String> data) {
    final play.api.mvc.Request<Http.RequestBody> req =
        fakeRequest().withFormUrlEncodedBody(data).getWrappedRequest();
    Http.Request request = new Request() {

      @Override
      public RequestBody body() {
        return req.body();
      }

      @Override
      public String uri() {
        return req.uri();
      }

      @Override
      public String method() {
        return req.method();
      }

      @Override
      public String version() {
        return req.version();
      }

      @Override
      public String remoteAddress() {
        return req.remoteAddress();
      }

      @Override
      public String host() {
        try {
          URL url = new URL(Play.application()
              .configuration().getString("application.baseUrl"));
          if (url.getDefaultPort() == url.getPort()) {
            return url.getHost();
          } else {
            return url.getHost() + ":" + url.getPort();
          }
        } catch (MalformedURLException e) {
          throw new RuntimeException(e);
        }
      }

      @Override
      public String path() {
        return req.path();
      }

      @Override
      public List<Lang> acceptLanguages() {
        List<Lang> list = new LinkedList<Lang>();
        for (play.api.i18n.Lang l : Scala.asJava(req.acceptLanguages())) {
          list.add(new Lang(l));
        }
        return list;
      }

      @Override
      @Deprecated
      public List<String> accept() {
        return Scala.asJava(req.accept());
      }

      @Override
      public List<MediaRange> acceptedTypes() {
        return Scala.asJava(req.acceptedTypes());
      }

      @Override
      public boolean accepts(String mimeType) {
        return req.accepts(mimeType);
      }

      @Override
      public Map<String, String[]> queryString() {
        return javaMap(req.queryString());
      }

      @Override
      public Cookies cookies() {
        // Not implemented
        return null;
      }

      @Override
      public Map<String, String[]> headers() {
        return javaMap(req.headers().toMap());
      }

      private Map<String, String[]> javaMap(scala.collection.Map<String, Seq<String>> map) {
        Map<String, String[]> m = new HashMap<String, String[]>();
        for (Entry<String,Seq<String>> e : Scala.asJava(map).entrySet()) {
          m.put(e.getKey(),
              Scala.asJava(e.getValue()).toArray(new String[]{}));
        }
        return m;
      }

    };
    Map<String,String> sessionData = emptyMap();
    Map<String,String> flashData = emptyMap();
    Map<String,Object> args = emptyMap();
    return new Http.Context(req.id(), req,
        request, sessionData, flashData, args);
  }

  @Override
  protected String generateVerificationRecord(final SignupUser user) {
    return getSessionFactory().inSession(
        new F.Function<Session, String>() {
      @Override
      public String apply(final Session session) {
        UserDAO dao = getUserDAO(session);
        User u = dao.findByEmail(user.getEmail());
        try {
          return u.createVerificationToken();
        } finally {
          dao.update(u);
        }
      }
    });
  }

  @Override
  protected String getVerifyEmailMailingSubject(SignupUser user, Context ctx) {
    return "Please verify your email address";
  }

  @Override
  protected Body getVerifyEmailMailingBody(String verificationToken,
      SignupUser user, Context ctx) {
    // TODO: Make this intelligible
    return new Body(controllers.routes.Application.verify(
        user.getEmail(), verificationToken).absoluteURL(ctx.request()));
  }

  @Override
  protected LoginUser buildLoginAuthUser(Login login, Context ctx) {
    return new LoginUser(login.getPassword(), login.getEmail());
  }

  @Override
  protected LoginUser transformAuthUser(SignupUser signupUser, Context ctx) {
    // This should never be called
    throw new UnsupportedOperationException("Signup users must validate");
  }

  @Override
  protected SignupUser
      buildSignupAuthUser(Invite invite, Context ctx) {
    return new SignupUser(invite.email, invite.name);
  }

  @Override
  protected
      com.feth.play.module.pa.providers.password.UsernamePasswordAuthProvider.LoginResult
      loginUser(final LoginUser loginUser) {
    return getSessionFactory().inSession(
        new F.Function<Session, LoginResult>() {
      @Override
      public LoginResult apply(final Session session) throws Throwable {
        final UserDAO dao = getUserDAO(session);
        final User u = dao.findByEmail(loginUser.getEmail());
        if (u == null) {
          Logger.debug(loginUser.getEmail() +
              " attempted to login but was not found.");
          return LoginResult.NOT_FOUND;
        }
        if (!u.isVerified()) {
          Logger.debug(u + " attempted to login but is still unverified.");
          return LoginResult.USER_UNVERIFIED;
        }
        if (!dao.checkPassword(u, loginUser.getPassword())) {
          Logger.debug(u + " provided an incorrect password.");
          return LoginResult.WRONG_PASSWORD;
        }
        Logger.debug(u + " successfully authenticated.");
        return LoginResult.USER_LOGGED_IN;
      }
    });
  }

  @Override
  protected
      com.feth.play.module.pa.providers.password.UsernamePasswordAuthProvider.SignupResult
      signupUser(final SignupUser user) {
    final String e = user.getEmail();
    return getSessionFactory().inSession(
        new F.Function<Session, SignupResult>() {
      @Override
      public SignupResult apply(final Session session) throws Throwable {
        final UserDAO dao = getUserDAO(session);
        {
          final User existingUser = dao.findByEmail(e);
          if (existingUser != null) {
            Logger.debug("Found existing user for "+e+": "+existingUser);
            if (existingUser.isVerified()) {
              return SignupResult.USER_EXISTS;
            } else {
              return SignupResult.USER_EXISTS_UNVERIFIED;
            }
          }
        }
        dao.create(user.asNewUser());
        Logger.debug("Created new user: "+user.asNewUser());
        return SignupResult.USER_CREATED_UNVERIFIED;
      }
    });
  }

  @Override
  protected Form<Invite> getSignupForm() {
    return form(Invite.class);
  }

  @Override
  protected Form<Login> getLoginForm() {
    return form(Login.class);
  }

  @Override
  protected Call userExists(UsernamePasswordAuthUser authUser) {
    return controllers.routes.Application.userExists(authUser.getEmail());
  }

  @Override
  protected Call userUnverified(UsernamePasswordAuthUser authUser) {
    return controllers.routes.Application.userUnverified(authUser.getEmail());
  }

}

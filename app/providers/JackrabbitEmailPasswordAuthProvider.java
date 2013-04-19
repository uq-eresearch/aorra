package providers;

import static play.test.Helpers.fakeRequest;
import static java.util.Collections.emptyMap;
import static play.data.Form.form;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import models.User.Invite;
import models.User.Login;
import play.Application;
import play.Logger;
import play.api.http.MediaRange;
import play.api.mvc.AnyContentAsFormUrlEncoded;
import play.data.Form;
import play.i18n.Lang;
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
import com.google.common.collect.ImmutableMap.Builder;

public class JackrabbitEmailPasswordAuthProvider
    extends
    UsernamePasswordAuthProvider<String, JackrabbitEmailPasswordAuthProvider.LoginUser, JackrabbitEmailPasswordAuthProvider.SignupUser, Login, Invite> {

  private final Application application;

  private final Map<String, String> verifiedUsers = new HashMap<String, String>();
  private final Map<String, String> unverifiedUsers = new HashMap<String, String>();
  private final Map<String, String> verificationTokens = new HashMap<String, String>();

  public JackrabbitEmailPasswordAuthProvider(Application application) {
    super(application);
    this.application = application;
  }

  public JcrSessionFactory getSessionFactory() {
    return GuiceInjectionPlugin.getInjector(application).getInstance(
        JcrSessionFactory.class);
  }

  public static class SignupUser extends UsernamePasswordAuthUser {
    private static final long serialVersionUID = 1L;

    public final String name;

    public SignupUser(final String email, final String name) {
      super(null, email);
      this.name = name;
    }
  }

  public static class LoginUser extends UsernamePasswordAuthUser {
    private static final long serialVersionUID = 1L;

    public LoginUser(final String email) {
      super(null, email);
    }

    public LoginUser(final String clearPassword, final String email) {
      super(clearPassword, email);
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
    long id = 0;
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
        return req.host();
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


  // Only used for testing.
  public String getVerificationToken(String email) {
    for (Entry<String, String> e : verificationTokens.entrySet()) {
      if (e.getValue().equals(email)) {
        return e.getKey();
      }
    }
    return null;
  }

  public LoginUser verifyWithToken(String token) {
    if (verificationTokens.containsKey(token)) {
      final String email = verificationTokens.get(token);
      if (!unverifiedUsers.containsKey(email)) {
        return null;
      }
      final String hashedPassword = unverifiedUsers.get(email);
      verifiedUsers.put(email, hashedPassword);
      verificationTokens.remove(token);
      unverifiedUsers.remove(email);
      return new LoginUser(email);
    } else {
      return null;
    }
  }

  @Override
  protected String generateVerificationRecord(SignupUser user) {
    final String token = UUID.randomUUID().toString();
    verificationTokens.put(token, user.getEmail());
    return token;
  }

  @Override
  protected String getVerifyEmailMailingSubject(SignupUser user, Context ctx) {
    return "Please verify your email address";
  }

  @Override
  protected Body getVerifyEmailMailingBody(String verificationRecord,
      SignupUser user, Context ctx) {
    // TODO: Make this intelligible
    return new Body(verificationRecord);
  }

  @Override
  protected LoginUser buildLoginAuthUser(Login login, Context ctx) {
    return new LoginUser(login.getPassword(), login.getEmail());
  }

  @Override
  protected LoginUser transformAuthUser(SignupUser signupUser, Context context) {
    return new LoginUser(signupUser.getEmail());
  }

  @Override
  protected SignupUser
      buildSignupAuthUser(Invite invite, Context ctx) {
    return new SignupUser(invite.email, invite.name);
  }

  @Override
  protected
      com.feth.play.module.pa.providers.password.UsernamePasswordAuthProvider.LoginResult
      loginUser(LoginUser user) {
    final String e = user.getEmail();
    if (unverifiedUsers.containsKey(e)) {
      Logger.debug(e + " attempted to login but is still unverified.");
      return LoginResult.USER_UNVERIFIED;
    }
    if (!verifiedUsers.containsKey(e)) {
      Logger.debug(e + " attempted to login but was not found.");
      return LoginResult.NOT_FOUND;
    }
    final boolean passwordCorrect = user.checkPassword(verifiedUsers.get(e),
        user.getPassword());
    if (!passwordCorrect) {
      Logger.debug(e + " provided an incorrect password.");
      return LoginResult.WRONG_PASSWORD;
    }
    Logger.debug(e + " successfully authenticated.");
    return LoginResult.USER_LOGGED_IN;
  }

  @Override
  protected
      com.feth.play.module.pa.providers.password.UsernamePasswordAuthProvider.SignupResult
      signupUser(SignupUser user) {
    final String e = user.getEmail();
    if (verifiedUsers.containsKey(e)) {
      return SignupResult.USER_EXISTS;
    }
    if (unverifiedUsers.containsKey(e)) {
      return SignupResult.USER_EXISTS_UNVERIFIED;
    }
    unverifiedUsers.put(user.getEmail(), user.getHashedPassword());
    return SignupResult.USER_CREATED_UNVERIFIED;
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
    return controllers.routes.Application.userExists();
  }

  @Override
  protected Call userUnverified(UsernamePasswordAuthUser authUser) {
    return controllers.routes.Application.userUnverified();
  }

}

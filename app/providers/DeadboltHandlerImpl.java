package providers;

import java.util.Collections;
import java.util.List;

import com.feth.play.module.pa.PlayAuthenticate;
import com.feth.play.module.pa.user.AuthUser;
import com.feth.play.module.pa.user.EmailIdentity;

import play.mvc.Http.Context;
import play.mvc.Result;
import be.objectify.deadbolt.core.models.Permission;
import be.objectify.deadbolt.core.models.Role;
import be.objectify.deadbolt.core.models.Subject;
import be.objectify.deadbolt.java.AbstractDeadboltHandler;
import be.objectify.deadbolt.java.DynamicResourceHandler;

public class DeadboltHandlerImpl extends AbstractDeadboltHandler {

  public class EmailIdentitySubject implements Subject, EmailIdentity {

    protected final EmailIdentity identity;

    public EmailIdentitySubject(final EmailIdentity identity) {
      this.identity = identity;
    }

    @Override
    public List<? extends Role> getRoles() {
      return Collections.emptyList();
    }

    @Override
    public List<? extends Permission> getPermissions() {
      return Collections.emptyList();
    }

    @Override
    public String getIdentifier() {
      return getId();
    }

    @Override
    public String getId() {
      return identity.getId();
    }

    @Override
    public String getProvider() {
      return identity.getProvider();
    }

    @Override
    public String getEmail() {
      return identity.getEmail();
    }

  }


  @Override
  public Result beforeAuthCheck(Context context) {
    // Nothing should be required - we're using security annotations
    return null;
  }

  @Override
  public Subject getSubject(Context context) {
    AuthUser authUser = PlayAuthenticate.getUser(context);
    if (authUser instanceof EmailIdentity) {
      return new EmailIdentitySubject((EmailIdentity) authUser);
    }
    return null;
  }

  @Override
  public Result onAuthFailure(Context context, String content) {
    // TODO: Implement more user-friendly view
    return forbidden(content);
  }

  @Override
  public DynamicResourceHandler getDynamicResourceHandler(Context context) {
    // None for now
    return null;
  }

}

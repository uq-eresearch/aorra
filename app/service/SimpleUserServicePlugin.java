package service;

import play.Application;
import providers.JackrabbitEmailPasswordAuthProvider.LoginUser;

import com.feth.play.module.pa.service.UserServicePlugin;
import com.feth.play.module.pa.user.AuthUser;
import com.feth.play.module.pa.user.AuthUserIdentity;
import com.feth.play.module.pa.user.EmailIdentity;

public class SimpleUserServicePlugin extends UserServicePlugin {

	public SimpleUserServicePlugin(Application app) {
		super(app);
	}

	@Override
	public Object save(final AuthUser authUser) {
	  if (authUser instanceof LoginUser) {
  	  // No saving done - we'll just trust the ID
  		return authUser;
	  }
	  throw new UnsupportedOperationException(
	      "Only email/password logins are currently supported");
	}

	@Override
	public Object getLocalIdentity(final AuthUserIdentity identity) {
	  if (identity.getProvider().equals("password")) {
	    return new EmailIdentity() {
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
          return getId();
        }
	    };
	  }
    throw new UnsupportedOperationException(
        "Only email/password logins are currently supported");
	}

	@Override
	public AuthUser merge(AuthUser newUser, AuthUser oldUser) {
		// Not Implemented
		return newUser;
	}

	@Override
	public AuthUser link(AuthUser oldUser, AuthUser newUser) {
		// Not Implemented
		return newUser;
	}

}

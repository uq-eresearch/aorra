import static play.Play.application;

import org.crsh.cli.Command
import org.crsh.cli.Usage
import org.crsh.cli.Argument
import org.crsh.cli.Required
import models.User
import play.Play
import service.GuiceInjectionPlugin
import providers.JackrabbitEmailPasswordAuthProvider


@Usage("User information")
class user {

  @Usage("invite user")
  @Command
  String invite(
      @Usage("email")
      @Argument
      @Required
      String email,
      @Usage("name")
      @Argument
      @Required
      String name) {
    authProvider().signup(new User.Invite(email, name))
  }
  
  private JackrabbitEmailPasswordAuthProvider authProvider() {
    return application().plugin(JackrabbitEmailPasswordAuthProvider.class)
  }
  
}
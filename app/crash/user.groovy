import static play.Play.application;

import org.crsh.cli.Command
import org.crsh.cli.Usage
import org.crsh.cli.Option
import play.Play
import service.GuiceInjectionPlugin;
import service.JackrabbitUserService
import securesocial.core.UserId

@Usage("User information")
class user {
  
  @Usage("list existing users")
  @Command
  String list() {
    userService().list().mkString("\n")
  }
  
  @Usage("find existing user")
  @Command
  String find(
      @Usage("identity provider (default: userpass)")
      @Option(names=["p","provider"])
      String provider,
      @Usage("user id")
      @Argument
      String id) {
    if (provider == null) {
      provider = "userpass"
    }
    userService().find(new UserId(id, provider)).get().toString()
  }
  
  private JackrabbitUserService userService() {
    return GuiceInjectionPlugin.getInjector(application())
                               .getInstance(JackrabbitUserService.class);
  }
  
}
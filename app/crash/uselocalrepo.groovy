import org.crsh.command.MethodDispatcher

// "use" is a Groovy keyword, which means we can't write repo.use
(new MethodDispatcher(repo, "use")) "org.apache.jackrabbit.repository.jndi.name=/jackrabbit"
ws.login "default"
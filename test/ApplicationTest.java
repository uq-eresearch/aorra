import static org.fest.assertions.Assertions.assertThat;
import static play.mvc.Http.Status.OK;
import static play.test.Helpers.callAction;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.running;
import static play.test.Helpers.status;
import static test.AorraTestUtils.fakeAorraApp;

import org.junit.Test;

import play.mvc.Result;

public class ApplicationTest {

  @Test
	public void indexShowsLoginPage() {
		running(fakeAorraApp(), new Runnable() {
			@Override
			public void run() {
				Result result = callAction(controllers.routes.ref.Application.index());
				assertThat(status(result)).isEqualTo(OK);
				String pageContent = contentAsString(result);
				assertThat(pageContent).contains("name=\"email\"");
        assertThat(pageContent).contains("name=\"password\"");
			}
		});
	}

}

package play.test;

public class CopiedFakeRequest extends play.test.FakeRequest {

  @SuppressWarnings("rawtypes")
  public CopiedFakeRequest(play.api.test.FakeRequest fake) {
    this.fake = fake;
  }

  public CopiedFakeRequest(play.test.FakeRequest other) {
    this(other.fake);
  }

}

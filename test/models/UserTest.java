package models;

import static org.fest.assertions.Assertions.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

public class UserTest {

  @Test
  public void generatedNodeNames() {
    List<String> emailAddresses = Arrays.asList(
        "user@domain.com",
        "user@domain.net",
        "user@uq.edu.au",
        "tommy@domain.com");
    Map<String,String> nodeNames = new HashMap<String,String>();
    for (String email : emailAddresses) {
      nodeNames.put(User.generateNodeName(email), email);
    }
    assertThat(nodeNames.size()).isEqualTo(emailAddresses.size());
    for (String email : emailAddresses) {
      assertThat(nodeNames.get(User.generateNodeName(email))).isEqualTo(email);
    }
  }

}

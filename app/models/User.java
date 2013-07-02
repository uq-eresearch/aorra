package models;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.jcr.SimpleCredentials;
import javax.jcr.nodetype.NodeType;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.jcrom.annotations.JcrIdentifier;
import org.jcrom.annotations.JcrName;
import org.jcrom.annotations.JcrNode;
import org.jcrom.annotations.JcrPath;
import org.jcrom.annotations.JcrProperty;

import play.data.validation.Constraints.Email;
import play.data.validation.Constraints.MinLength;
import play.data.validation.Constraints.Required;
import play.data.validation.ValidationError;

import com.feth.play.module.pa.providers.password.UsernamePasswordAuthProvider.UsernamePassword;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

@JcrNode(
    nodeType = NodeType.NT_UNSTRUCTURED,
    mixinTypes = {
      NodeType.MIX_CREATED,
      NodeType.MIX_LAST_MODIFIED,
      NodeType.MIX_REFERENCEABLE
    },
    classNameProperty = "className")
public class User {

  @JcrIdentifier private String id;
  @JcrName private String nodeName;
  @JcrPath private String nodePath;
  @JcrProperty private String email;
  @JcrProperty private String name;
  @JcrProperty private String verificationToken;
  @JcrProperty private boolean verified;

  public static class ChangePassword {
    @Required
    @MinLength(8)
    public String password;

    @Required
    public String repeatPassword;

    public Map<String,List<ValidationError>> validate() {
      Builder<String, List<ValidationError>> b =
          ImmutableMap.<String, List<ValidationError>>builder();
      if (!password.equals(repeatPassword)) {
        b.put("repeatPassword", Arrays.asList(
            new ValidationError("password.match", "Passwords don't match")));
      }
      return b.build();
    }
  }

  public static class Login implements UsernamePassword {
    @Required
    @Email
    public String email;

    @Required
    public String password;

    @Override
    public String getEmail() {
      return email;
    }

    @Override
    public String getPassword() {
      return password;
    }
  }

  public static class Invite implements UsernamePassword {
    @Required
    @Email
    public String email;

    @Required
    @MinLength(2)
    public String name;

    public Invite() {}
    public Invite(String e, String n) { this.email = e; this.name = n; }

    @Override
    public String getEmail() {
      return email;
    }

    @Override
    public String getPassword() {
      return null;
    }
  }

  public User() {}

  public String getId() {
    return id;
  }

  public String getJackrabbitUserId() {
    return getId();
  }

  public SimpleCredentials getCredentials() {
    return new SimpleCredentials(email, "".toCharArray());
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
    this.nodeName = generateNodeName(email);
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  String getNodePath() {
    return nodePath;
  }

  public boolean isVerified() {
    return verified;
  }

  public void clearVerificationToken() {
    this.verificationToken = null;
  }

  public String createVerificationToken() {
    this.verificationToken = generateVerificationToken();
    return this.verificationToken;
  }

  public boolean checkVerificationToken(String token) {
    if (this.verificationToken == null)
      return false;
    return this.verificationToken.equals(token);
  }

  protected String generateVerificationToken() {
    return UUID.randomUUID().toString();
  }

  public void setVerified(boolean state) {
    this.verified = state;
  }

  static String generateNodeName(String email) {
    final MessageDigest md;
    try {
      md = MessageDigest.getInstance("SHA-1");
    } catch (NoSuchAlgorithmException e) {
      // Algorithm is fixed, so this should never happen
      throw new RuntimeException(e);
    }
    return Hex.encodeHexString(
        md.digest(email.getBytes(Charset.forName("UTF-8"))));
  }

  @Override
  public boolean equals(Object other) {
    return EqualsBuilder.reflectionEquals(this, other);
  }

  @Override
  public int hashCode() {
    return HashCodeBuilder.reflectionHashCode(this);
  }

  @Override
  public String toString() {
    if (verified) {
      return String.format("%s <%s> [id: %s]", getName(), getEmail(), getId());
    } else {
      return String.format("%s <%s> [VT: %s][id: %s]",
          getName(), getEmail(), verificationToken, getId());
    }
  }

}
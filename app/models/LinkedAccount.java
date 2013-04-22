package models;

import org.jcrom.annotations.JcrName;
import org.jcrom.annotations.JcrNode;
import org.jcrom.annotations.JcrPath;
import org.jcrom.annotations.JcrProperty;

@JcrNode
public class LinkedAccount {

  @JcrName private String id;
  @JcrPath private String nodePath;
  @JcrProperty private String provider;

}
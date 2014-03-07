package controllers;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import javax.jcr.Session;

import models.CacheableUser;

import org.jcrom.Jcrom;

import play.libs.F;
import play.mvc.Controller;
import providers.CacheableUserProvider;
import service.JcrSessionFactory;

public abstract class SessionAwareController extends Controller {

  protected final JcrSessionFactory sessionFactory;
  protected final Jcrom jcrom;
  protected final CacheableUserProvider subjectHandler;

  protected SessionAwareController(final JcrSessionFactory sessionFactory,
      final Jcrom jcrom, final CacheableUserProvider subjectHandler) {
    this.sessionFactory = sessionFactory;
    this.jcrom = jcrom;
    this.subjectHandler = subjectHandler;
  }

  protected CacheableUser getUser() {
    return subjectHandler.getUser(ctx().session());
  }

  protected <A extends Object> A inUserSession(final F.Function<Session, A> f) {
    return sessionFactory.inSession(getUser().getJackrabbitUserId(), f);
  }

  private static final SimpleDateFormat httpDateFormat = new SimpleDateFormat(
      "EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH);

  protected static String asHttpDate(final Calendar calendar) {
    httpDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    return httpDateFormat.format(calendar.getTime());
  }

  protected static String asHttpDate(final Date date) {
    GregorianCalendar c = new GregorianCalendar();
    c.setTime(date);
    return asHttpDate(c);
  }

  protected static Calendar fromHttpDate(final String timeStr)
      throws ParseException {
    final Calendar cal = Calendar.getInstance();
    cal.setTime(httpDateFormat.parse(timeStr));
    return cal;
  }

  protected boolean modified(Date date) throws ParseException {
    String clientcached = ctx().request().getHeader("If-Modified-Since");
    if(clientcached != null) {
      return fromHttpDate(asHttpDate(date)).getTime().after(
          fromHttpDate(clientcached).getTime());
    }
    return true;
  }

}

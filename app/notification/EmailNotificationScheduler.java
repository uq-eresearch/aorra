package notification;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.lang3.tuple.Pair;

import play.Logger;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class EmailNotificationScheduler {

  private final Timer timer = new Timer();

  private final EmailNotifier emailNotifier;

  @Inject
  public EmailNotificationScheduler(EmailNotifier emailNotifier) {
    if(emailNotifier == null) {
      throw new RuntimeException("emailNotifier is null");
    }
    this.emailNotifier = emailNotifier;
  }

  public void start() {
    schedule();
  }

  public void stop() {
    timer.cancel();
  }

  private void sendEmailNotifications(EmailNotifier en) {
    Pair<Date, Date> period = notificationPeriod();
    en.sendEmailNotifications(period.getLeft(), period.getRight());
  }

  private void schedule() {
    Date next = nextEmailNotificationTime();
    timer.schedule(new TimerTask() {
      @Override
      public void run() {
        try {
          sendEmailNotifications(emailNotifier);
        } finally {
          schedule();
        }
      }}, next);
    Logger.info("Next email notification scheduled for "+next.toString());
  }

  private Date nextEmailNotificationTime() {
    GregorianCalendar calendar = new GregorianCalendar();
    Date now = calendar.getTime();
    calendar.set(Calendar.HOUR_OF_DAY, 6);
    calendar.set(Calendar.MINUTE, 0);
    calendar.set(Calendar.SECOND, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    if(now.getTime() >= calendar.getTime().getTime()) {
      calendar.add(Calendar.DAY_OF_MONTH, 1);
    }
    return calendar.getTime();
  }

  private Pair<Date, Date> notificationPeriod() {
    GregorianCalendar calendar = new GregorianCalendar();
    calendar.set(Calendar.HOUR_OF_DAY, 0);
    calendar.set(Calendar.MINUTE, 0);
    calendar.set(Calendar.SECOND, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    Date until = calendar.getTime();
    calendar.add(Calendar.DAY_OF_MONTH, -1);
    Date since = calendar.getTime();
    return Pair.of(since, until);
  }

}

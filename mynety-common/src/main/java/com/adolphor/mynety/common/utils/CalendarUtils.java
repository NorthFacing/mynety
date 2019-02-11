package com.adolphor.mynety.common.utils;

import java.util.Calendar;
import java.util.Date;

/**
 * Calendar Utils
 *
 * @author Bob.Zhu
 * @Email adolphor@qq.com
 * @since v0.0.6
 */
public class CalendarUtils {

  public static Date today() {
    Calendar cal = Calendar.getInstance();
    cal.set(Calendar.HOUR_OF_DAY, 0);
    cal.set(Calendar.MINUTE, 0);
    cal.set(Calendar.SECOND, 0);
    cal.set(Calendar.MILLISECOND, 0);
    return cal.getTime();
  }

  public static Date addYears(Date date, int count) {
    Calendar cal = Calendar.getInstance();
    cal.setTime(date);
    cal.add(Calendar.YEAR, count);
    return cal.getTime();
  }

}

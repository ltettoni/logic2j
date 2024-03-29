/*
 * logic2j - "Bring Logic to your Java" - Copyright (c) 2017 Laurent.Tettoni@gmail.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Foobar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.logic2j.core.impl;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.beanutils.PropertyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EnvManager {
  private static final Logger logger = LoggerFactory.getLogger(EnvManager.class);

  /**
   * Thread variables will be prefixed with this name.
   */
  public static final String VAR_PREFIX_THREAD = "thread.";

  /**
   * JVM properties will be prefixed with this name.
   */
  public static final String VAR_PREFIX_JVM = "jvm.";

  /**
   * Operating-system process environment variables will be prefixed with this name.
   */
  public static final String VAR_PREFIX_ENV = "env.";

  private final Map<String, Object> environment = new HashMap<>();


  public EnvManager() {
    this.environment.put(VAR_PREFIX_ENV.replace(".", ""), System.getenv());
  }


  public Object getVariable(String theExpression) {
    final Object value;
    try {
      if (theExpression.startsWith(VAR_PREFIX_JVM)) {
        // VAR_PREFIX_JVM properties have this stupid habit of using dot (".") which is also BeanUtils (and most ELs)
        // nested properties separator - we have to work this around
        final String rest = theExpression.replaceFirst(VAR_PREFIX_JVM, "");
        value = System.getProperty(rest);
      } else if (theExpression.startsWith(VAR_PREFIX_THREAD)) {
        final String rest = theExpression.replaceFirst(VAR_PREFIX_THREAD, "");
        value = getThreadVariable(rest);
      } else {
        value = PropertyUtils.getProperty(this.environment, theExpression);
      }
    } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
      // No solution
      return null;
    }
    logger.debug("Getting variable \"{}\",  value={}", theExpression, value);
    return value;
  }

  public void setVariable(String theExpression, Object theValue) {
    logger.debug("Setting variable \"{}\", value={}", theExpression, theValue);
    if (theExpression.startsWith(VAR_PREFIX_THREAD)) {
      final String rest = theExpression.replaceFirst(VAR_PREFIX_THREAD, "");
      setThreadVariable(rest, theValue);
    } else {
      this.environment.put(theExpression, theValue);
    }
  }


  private static final ThreadLocal<Map<String, Object>> threadLocalBindings = ThreadLocal.withInitial(HashMap::new);

  public static Object getThreadVariable(String theVariableName) {
    return threadLocalBindings.get().get(theVariableName);
  }

  public static void setThreadVariable(String theVariableName, Object theValue) {
    threadLocalBindings.get().put(theVariableName, theValue);
  }

}

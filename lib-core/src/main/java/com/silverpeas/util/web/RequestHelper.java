/**
 * Copyright (C) 2000 - 2009 Silverpeas
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * As a special exception to the terms and conditions of version 3.0 of
 * the GPL, you may redistribute this Program in connection with Free/Libre
 * Open Source Software ("FLOSS") applications as described in Silverpeas's
 * FLOSS exception.  You should have received a copy of the text describing
 * the FLOSS exception, and it is also available here:
 * "http://repository.silverpeas.com/legal/licensing"
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.silverpeas.util.web;

import com.silverpeas.util.StringUtil;
import java.io.UnsupportedEncodingException;
import javax.servlet.http.HttpServletRequest;

/**
 * Class to be used while waiting for the usage of UTF-8 on all Silverpeas.
 * @author ehugonnet
 */
public class RequestHelper {

  public static String getRequestParameter(HttpServletRequest request, String parameterName) 
      throws UnsupportedEncodingException {
    String value = request.getParameter(parameterName);
    if (StringUtil.isDefined(value)) {
      value = new String(value.getBytes("ISO-8859-1"), "UTF-8");
    }
    return value;
  }

  private RequestHelper() {
  }
}
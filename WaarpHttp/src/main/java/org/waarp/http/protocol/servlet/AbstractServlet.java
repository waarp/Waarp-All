/*
 * This file is part of Waarp Project (named also Waarp or GG).
 *
 *  Copyright (c) 2019, Waarp SAS, and individual contributors by the @author
 *  tags. See the COPYRIGHT.txt in the distribution for a full listing of
 * individual contributors.
 *
 *  All Waarp Project is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Waarp is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 * Waarp . If not, see <http://www.gnu.org/licenses/>.
 */

package org.waarp.http.protocol.servlet;

import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.http.protocol.WaarpStartup;
import org.waarp.openr66.protocol.utils.ChannelUtils;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import java.io.File;
import java.net.URL;

public class AbstractServlet extends HttpServlet {
  private static final long serialVersionUID = 2001L;
  public static final String R_66_CONFIG = "r66config";
  public static final String AUTHENT_CLASSNAME = "authentClassName";
  public static final String RULENAME = "rulename";
  public static final String COMMENT = "comment";
  protected static final String INVALID_REQUEST_PARAMS =
      "Invalid request params.";
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(AbstractServlet.class);
  protected Class<HttpAuthent> authentClass;

  @Override
  public void destroy() {
    ChannelUtils.exit();
    super.destroy();
  }

  @Override
  public void init(ServletConfig config) throws ServletException {
    final String r66Config = config.getInitParameter(R_66_CONFIG);
    logger.warn("Parameter Init: {}", r66Config);
    File file = new File(r66Config);
    logger
        .debug("Parameter Init: {} {}?", file.getAbsolutePath(), file.exists());
    if (!file.exists()) {
      final ClassLoader classLoader = AbstractServlet.class.getClassLoader();
      URL url = classLoader.getResource(r66Config);
      if (url != null) {
        file = new File(url.getFile());
      }
      logger.debug("Parameter Init: {} {}?", file.getAbsolutePath(),
                   file.exists());
    }
    String sauthent = config.getInitParameter(AUTHENT_CLASSNAME);
    logger.warn("Parameter Init: {}", sauthent);
    try {
      authentClass = (Class<HttpAuthent>) Class.forName(sauthent);
    } catch (ClassNotFoundException e) {
      logger.error("Cannot find authent class {}", sauthent);
      throw new ServletException("Cannot find authent class");
    }
    WaarpStartup.startupWaarp(file);
    logger.info("{}: {} {}", config.getServletName(),
                config.getServletContext().getContextPath(),
                config.getServletContext().getServletContextName());
  }
}

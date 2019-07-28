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
package org.waarp.openr66.context.task.test;

import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.openr66.context.R66BusinessFactoryInterface;
import org.waarp.openr66.context.R66BusinessInterface;
import org.waarp.openr66.context.R66Session;
import org.waarp.openr66.context.task.exception.OpenR66RunnerErrorException;

/**
 * Example dummy Business Factory which only generates Log Business
 */
public class TestBusinessFactory implements R66BusinessFactoryInterface {

  /**
   * Empry constructor
   */
  public TestBusinessFactory() {
    // nothing
  }

  @Override
  public R66BusinessInterface getBusinessInterface(R66Session session) {
    return new R66BusinessInterface() {
      /**
       * Internal Logger
       */
      private final WaarpLogger logger =
          WaarpLoggerFactory.getLogger(R66BusinessInterface.class);
      private String info;

      @Override
      public void setInfo(R66Session session, String info) {
        logger.warn("setInfo: " + session + " = " + info);
        this.info = info;
      }

      @Override
      public void releaseResources(R66Session session) {
        logger.warn("releaseResources: " + session);
      }

      @Override
      public String getInfo(R66Session session) {
        return info;
      }

      @Override
      public void checkAtStartup(R66Session session)
          throws OpenR66RunnerErrorException {
        logger.warn("checkAtStartup: " + session);
      }

      @Override
      public void checkAtError(R66Session session) {
        logger.warn("checkAtError: " + session);
      }

      @Override
      public void checkAtConnection(R66Session session)
          throws OpenR66RunnerErrorException {
        logger.warn("checkAtConnection: " + session);
      }

      @Override
      public void checkAtChangeFilename(R66Session session)
          throws OpenR66RunnerErrorException {
        logger.warn("checkAtChangeFilename: " + session);
      }

      @Override
      public void checkAtAuthentication(R66Session session)
          throws OpenR66RunnerErrorException {
        logger.warn("checkAtAuthentication: " + session);
      }

      @Override
      public void checkAfterTransfer(R66Session session)
          throws OpenR66RunnerErrorException {
        logger.warn("checkAfterTransfer: " + session);
      }

      @Override
      public void checkAfterPreCommand(R66Session session)
          throws OpenR66RunnerErrorException {
        logger.warn("checkAfterPreCommand: " + session);
      }

      @Override
      public void checkAfterPost(R66Session session)
          throws OpenR66RunnerErrorException {
        logger.warn("checkAfterPost: " + session);
      }
    };
  }

  @Override
  public void releaseResources() {
    // nothing
  }

}

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

package org.waarp.common.logging;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

@SuppressWarnings("javadoc")
public class WaarpLoggerFactoryTest {
  private static final Exception e = new Exception();
  private WaarpLoggerFactory oldLoggerFactory;
  private WaarpLogger mock;

  @Before
  public void init() {
    oldLoggerFactory = WaarpLoggerFactory.getDefaultFactory();
    final WaarpLoggerFactory mockFactory = createMock(WaarpLoggerFactory.class);
    mock = createStrictMock(WaarpLogger.class);
    expect(mockFactory.newInstance("mock")).andReturn(mock).anyTimes();
    expect(mockFactory.getLevelSpecific()).andReturn(WaarpLogLevel.DEBUG);
    mockFactory.seLevelSpecific(anyObject(WaarpLogLevel.class));
    expectLastCall();
    replay(mockFactory);
    WaarpLoggerFactory.setDefaultFactory(mockFactory);
  }

  @After
  public void destroy() {
    reset(mock);
    WaarpLoggerFactory.setDefaultFactory(oldLoggerFactory);
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldNotAllowNullDefaultFactory() {
    WaarpLoggerFactory.setDefaultFactory(null);
  }

  @Test
  public void shouldGetInstance() {
    WaarpLoggerFactory.setDefaultFactory(oldLoggerFactory);

    final String helloWorld = "Hello, world!";

    final WaarpLogger one = WaarpLoggerFactory.getInstance("helloWorld");
    final WaarpLogger two =
        WaarpLoggerFactory.getInstance(helloWorld.getClass());

    assertNotNull(one);
    assertNotNull(two);
    assertNotSame(one, two);
    final WaarpLogLevel logLevel = WaarpLoggerFactory.getLogLevel();
    WaarpLoggerFactory.setLogLevel(WaarpLogLevel.ERROR);
    assertEquals(WaarpLogLevel.ERROR, WaarpLoggerFactory.getLogLevel());
    WaarpLoggerFactory.setLogLevel(logLevel);
  }

  @Test
  public void testIsTraceEnabled() {
    expect(mock.isTraceEnabled()).andReturn(true);
    replay(mock);

    final WaarpLogger logger = WaarpLoggerFactory.getInstance("mock");
    assertTrue(logger.isTraceEnabled());
    verify(mock);
  }

  @Test
  public void testIsDebugEnabled() {
    expect(mock.isDebugEnabled()).andReturn(true);
    replay(mock);

    final WaarpLogger logger = WaarpLoggerFactory.getInstance("mock");
    assertTrue(logger.isDebugEnabled());
    verify(mock);
  }

  @Test
  public void testIsInfoEnabled() {
    expect(mock.isInfoEnabled()).andReturn(true);
    replay(mock);

    final WaarpLogger logger = WaarpLoggerFactory.getInstance("mock");
    assertTrue(logger.isInfoEnabled());
    verify(mock);
  }

  @Test
  public void testIsWarnEnabled() {
    expect(mock.isWarnEnabled()).andReturn(true);
    replay(mock);

    final WaarpLogger logger = WaarpLoggerFactory.getInstance("mock");
    assertTrue(logger.isWarnEnabled());
    verify(mock);
  }

  @Test
  public void testIsErrorEnabled() {
    expect(mock.isErrorEnabled()).andReturn(true);
    replay(mock);

    final WaarpLogger logger = WaarpLoggerFactory.getInstance("mock");
    assertTrue(logger.isErrorEnabled());
    verify(mock);
  }

  @Test
  public void testTrace() {
    mock.trace("a");
    replay(mock);

    final WaarpLogger logger = WaarpLoggerFactory.getInstance("mock");
    logger.trace("a");
    verify(mock);
  }

  @Test
  public void testTraceWithException() {
    mock.trace("a", e);
    replay(mock);

    final WaarpLogger logger = WaarpLoggerFactory.getInstance("mock");
    logger.trace("a", e);
    verify(mock);
  }

  @Test
  public void testDebug() {
    mock.debug("a");
    replay(mock);

    final WaarpLogger logger = WaarpLoggerFactory.getInstance("mock");
    logger.debug("a");
    verify(mock);
  }

  @Test
  public void testDebugWithException() {
    mock.debug("a", e);
    replay(mock);

    final WaarpLogger logger = WaarpLoggerFactory.getInstance("mock");
    logger.debug("a", e);
    verify(mock);
  }

  @Test
  public void testInfo() {
    mock.info("a");
    replay(mock);

    final WaarpLogger logger = WaarpLoggerFactory.getInstance("mock");
    logger.info("a");
    verify(mock);
  }

  @Test
  public void testInfoWithException() {
    mock.info("a", e);
    replay(mock);

    final WaarpLogger logger = WaarpLoggerFactory.getInstance("mock");
    logger.info("a", e);
    verify(mock);
  }

  @Test
  public void testWarn() {
    mock.warn("a");
    replay(mock);

    final WaarpLogger logger = WaarpLoggerFactory.getInstance("mock");
    logger.warn("a");
    verify(mock);
  }

  @Test
  public void testWarnWithException() {
    mock.warn("a", e);
    replay(mock);

    final WaarpLogger logger = WaarpLoggerFactory.getInstance("mock");
    logger.warn("a", e);
    verify(mock);
  }

  @Test
  public void testError() {
    mock.error("a");
    replay(mock);

    final WaarpLogger logger = WaarpLoggerFactory.getInstance("mock");
    logger.error("a");
    verify(mock);
  }

  @Test
  public void testErrorWithException() {
    mock.error("a", e);
    replay(mock);

    final WaarpLogger logger = WaarpLoggerFactory.getInstance("mock");
    logger.error("a", e);
    verify(mock);
  }
}

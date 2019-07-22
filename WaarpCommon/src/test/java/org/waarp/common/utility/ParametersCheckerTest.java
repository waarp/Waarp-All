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

package org.waarp.common.utility;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Parameters Checker Test
 */
public class ParametersCheckerTest {

  @Test
  public final void testCheckParamaterStringStringArray() {
    try {
      ParametersChecker.checkParameter("test message", (String[]) null);
      fail("SHOULD_RAIZED_ILLEGAL_ARGUMENT_EXCEPTION");
    } catch (final IllegalArgumentException e) { // NOSONAR
    }
    try {
      ParametersChecker.checkParameter("test message", null, "notnull");
      fail("SHOULD_RAIZED_ILLEGAL_ARGUMENT_EXCEPTION");
    } catch (final IllegalArgumentException e) { // NOSONAR
    }
    try {
      ParametersChecker.checkParameter("test message", "notnull", null);
      fail("SHOULD_RAIZED_ILLEGAL_ARGUMENT_EXCEPTION");
    } catch (final IllegalArgumentException e) { // NOSONAR
    }
    try {
      ParametersChecker.checkParameter("test message", "", "notnull");
      fail("SHOULD_RAIZED_ILLEGAL_ARGUMENT_EXCEPTION");
    } catch (final IllegalArgumentException e) { // NOSONAR
    }
    try {
      ParametersChecker.checkParameter("test message", "notnull", "");
      fail("SHOULD_RAIZED_ILLEGAL_ARGUMENT_EXCEPTION");
    } catch (final IllegalArgumentException e) { // NOSONAR
    }
    try {
      ParametersChecker.checkParameter("test message", "notNull", "notnull");
      ParametersChecker.checkParameter("test message", "notnull");
    } catch (final IllegalArgumentException e) { // NOSONAR
      fail("SHOULD_NOT_RAIZED_ILLEGAL_ARGUMENT_EXCEPTION");
    }
  }

  @Test
  public final void testCheckParamaterNullOnlyStringStringArray() {
    try {
      ParametersChecker.checkParameterNullOnly("test message", (String[]) null);
      fail("SHOULD_RAIZED_ILLEGAL_ARGUMENT_EXCEPTION");
    } catch (final IllegalArgumentException e) { // NOSONAR
    }
    try {
      ParametersChecker.checkParameterNullOnly("test message", null, "notnull");
      fail("SHOULD_RAIZED_ILLEGAL_ARGUMENT_EXCEPTION");
    } catch (final IllegalArgumentException e) { // NOSONAR
    }
    try {
      ParametersChecker.checkParameterNullOnly("test message", "notnull", null);
      fail("SHOULD_RAIZED_ILLEGAL_ARGUMENT_EXCEPTION");
    } catch (final IllegalArgumentException e) { // NOSONAR
    }
    try {
      ParametersChecker.checkParameterNullOnly("test message", "", "notnull");
    } catch (final IllegalArgumentException e) { // NOSONAR
      fail("SHOULD_NOT_RAIZED_ILLEGAL_ARGUMENT_EXCEPTION");
    }
    try {
      ParametersChecker.checkParameterNullOnly("test message", "notnull", "");
    } catch (final IllegalArgumentException e) { // NOSONAR
      fail("SHOULD_NOT_RAIZED_ILLEGAL_ARGUMENT_EXCEPTION");
    }
    try {
      ParametersChecker
          .checkParameterNullOnly("test message", "notNull", "notnull");
      ParametersChecker.checkParameterNullOnly("test message", "notnull");
    } catch (final IllegalArgumentException e) { // NOSONAR
      fail("SHOULD_NOT_RAIZED_ILLEGAL_ARGUMENT_EXCEPTION");
    }
  }

  @Test
  public final void testCheckParamaterStringObjectArray() {
    try {
      ParametersChecker.checkParameter("test message", (Object[]) null);
      fail("SHOULD_RAIZED_ILLEGAL_ARGUMENT_EXCEPTION");
    } catch (final IllegalArgumentException e) { // NOSONAR
    }
    final List<String> list = new ArrayList<String>();
    try {
      ParametersChecker.checkParameter("test message", null, list);
      fail("SHOULD_RAIZED_ILLEGAL_ARGUMENT_EXCEPTION");
    } catch (final IllegalArgumentException e) { // NOSONAR
    }
    try {
      ParametersChecker.checkParameter("test message", list, null);
      fail("SHOULD_RAIZED_ILLEGAL_ARGUMENT_EXCEPTION");
    } catch (final IllegalArgumentException e) { // NOSONAR
    }
    try {
      ParametersChecker.checkParameter("test message", list, list);
      ParametersChecker.checkParameter("test message", list);
    } catch (final IllegalArgumentException e) { // NOSONAR
      fail("SHOULD_NOT_RAIZED_ILLEGAL_ARGUMENT_EXCEPTION");
    }
  }

  @Test
  public final void testCheckValue() {
    try {
      ParametersChecker.checkValue("test", 1, 2);
      fail("SHOULD_RAIZED_ILLEGAL_ARGUMENT_EXCEPTION");
    } catch (final IllegalArgumentException e) { // NOSONAR
      // ok
    }
    ParametersChecker.checkValue("test", 1, 1);
    ParametersChecker.checkValue("test", 1, 0);
  }

}

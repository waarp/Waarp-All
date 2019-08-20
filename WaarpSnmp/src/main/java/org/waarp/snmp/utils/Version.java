// DO NOT MODIFY - WILL BE OVERWRITTEN DURING THE BUILD PROCESS
package org.waarp.snmp.utils;

/**
 * Provides the version information of Waarp Snmp.
 */
public final class Version {
 /**
 * The version identifier.
 */
 public static final String ID = "3.2.1";
 /**
 * Prints out the version identifier to stdout.
 */
 public static void main(String[] args) {
     System.out.println(ID);
 }
 private Version() {
     super();
 }
}

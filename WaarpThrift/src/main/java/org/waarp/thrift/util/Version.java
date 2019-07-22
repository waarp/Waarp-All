// DO NOT MODIFY - WILL BE OVERWRITTEN DURING THE BUILD PROCESS
package org.waarp.thrift.util;

/**
 * Provides the version information of WaarpThrift.
 */
public final class Version {
 /**
 * The version identifier.
 */
 public static final String ID = "3.2.0";
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

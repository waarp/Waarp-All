package org.waarp.openr66.protocol.http.restv2.resthandlers;

import io.netty.util.ResourceLeakDetector;
import io.netty.util.ResourceLeakDetector.Level;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.waarp.common.crypto.Des;
import org.waarp.common.crypto.HmacSha256;
import org.waarp.common.logging.WaarpLogLevel;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.logging.WaarpSlf4JLoggerFactory;
import org.waarp.common.utility.TestWatcherJunit4;
import org.waarp.common.utility.WaarpStringUtils;
import org.waarp.openr66.pojo.Host;
import org.waarp.openr66.protocol.configuration.Configuration;

import javax.ws.rs.InternalServerErrorException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.junit.Assert.*;

/**
 * RestHandlerHook
 */
public class RestHandlerHookTest {
  @Rule(order = Integer.MIN_VALUE)
  public TestWatcher watchman = new TestWatcherJunit4();

  @BeforeClass
  public static void beforeClass() {
    WaarpLoggerFactory.setDefaultFactoryIfNotSame(
        new WaarpSlf4JLoggerFactory(WaarpLogLevel.WARN));
    ResourceLeakDetector.setLevel(Level.PARANOID);
  }

  public static final class RestHandlerHookForTest extends RestHandlerHook {
    public RestHandlerHookForTest(final boolean authenticated,
                                  final HmacSha256 hmac, final long delay) {
      super(authenticated, hmac, delay);
    }

    public void testValidateHMACredentials(Host host, String authDate,
                                           String authUser, String authKey)
        throws InternalServerErrorException {
      validateHMACCredentials(host, authDate, authUser, authKey);
    }
  }

  @Test
  public void testCheckCredentialsWithHMAC() throws Exception {
    final HmacSha256 hmac = new HmacSha256();
    hmac.generateKey();

    final Des dyn = new Des();
    dyn.generateKey();
    Des oldKey = Configuration.configuration.getCryptoKey();
    Configuration.configuration.setCryptoKey(dyn);

    final String user = "user";
    final String password = "mypassword";
    final String timestamp =
        new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXX").format(new Date());

    try {

      final String hostkey = dyn.cryptToHex(password);
      final String sig = hmac.cryptToHex(timestamp + user + password);

      final RestHandlerHookForTest hook =
          new RestHandlerHookForTest(true, hmac, 10000);

      try {
        final Host host = new Host(user, "127.0.0.1", 1,
                                   hostkey.getBytes(WaarpStringUtils.UTF8),
                                   false, true);

        hook.testValidateHMACredentials(host, timestamp, user, sig);
      } catch (InternalServerErrorException e) {
        System.out.println(e);
        fail("credentials validation failed, it should have succeeded");
      }

    } finally {
      Configuration.configuration.setCryptoKey(oldKey);
    }

  }
}

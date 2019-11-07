Demo of Waarp HTTP
==================

In order to run the demo:

1) Initialize the database for R66 (see org.waarp.openr66.protocol.junit.InitDatabase.java)
2) Configure the WebApp in WEB-INF/web.xml

- `r66config` pointing to exact emplacement of Waarp R66 configuration file
- `authentClassName` pointing to the full class name (default being org.waarp.http.protocol.servlet.HttpAuthentDefault)

3) Launch a Servlet server using the following configuration:

- WebApp folder: Waarp-All/WaarpHttp/src/main/webapp
- Class folder: Waarp-All/WaarpHttp/target/classes (plus other modules from Waarp)
- Path of application: /upload
- Choose the port: for instance 8090
- You need to change all paths in the following files in order to get the correct one according to your
 installation, replacing `/home/frederic/Waarp2/` by your absolute path before Waarp-All:
   - `src/test/resources/config/config-serverA-minimal.xml`
   - `src/test/resources/config/OpenR66-authent-A.xml`
   - eventually other files such as `src/test/resources/config/logback.xml`

For testing, we use Jetty runner from IntelliJ.

- Once launched, look at the logs to see if the server starts. If not correct the error.
- Open a browser on: http://localhost:8090/upload/


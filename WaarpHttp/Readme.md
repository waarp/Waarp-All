Waarp Http Module
=================

## 1) Presentation

Waarp Http is a module of Waarp which allows to upload/download files from a 
browser for an end User, while those files are going to/coming from a Waarp server.

This module is not usable as is, some integration must be done to fit several points 
(see integration focus).

### 1.1) For Upload
The browser allows the user to select a local file (drag and drop or select through a window).

Once the file is selected, the javascript embedded computes the Hash (SHA-256) of this
local file and then sends this one by chunk (1MB by default) to the Waarp Web Server.


#### The client perspective
The sending follows the Waarp rules of transfers:
- a local unique identifier is assigned to the transfer
- a hash (SHA-256) is computed locally
- a rule of transfer is selected (in Send mode)
- an identifier of the user is selected
- the local name of the file
- the size of the chunk (default being 1MB)

##### Visual comportment
When the file is currently uploaded (a progression bar is visible for the user), 
the user can stop and restart the transfer or cancel the transfer .

A user can select multiple files, but they will be transferred one by one however.

##### Functional Comportment
For each chunk (block of binary content), the browser test if this block was already sent
before to the Waarp Web Server. If so, the chunk is ignored, else it is sent.

This allows to have "resumable" compliant transfers, including when lost of carrier.

#### Waarp Web Server perspective
From Waarp Web Server perspective, this transfer is as a "usual" transfer, using the standard
database of Waarp product. 

##### Functional Comportment
The unique identifier is given by the client. 

The hash is compared with the one given by the client.

The rule is given by the client. The Waarp Web Server will apply all pre and post
tasks, according to the rule used, and also the error tasks if any.

Therefore, it allows to use this transfer to reroute it to another Waarp server 
(using other Waarp protocols), or to execute a task that will manage the content
of this file for another application, potentially also embedded by the same Web
Application server.


### 1.2) For Download
This part is partially developed, since the availability of files to be downloaded
is part of the integration process. Therefore the HTML part and some of the javascript 
are given but it is straightforward to adapt it to specific download scenario.

Once the file is selected, the javascript download all the file and then compares 
the received file with the received Hash (SHA-256) from the Waarp Web Server.

#### The client perspective
The download follows the Waarp rules of transfers:
- a local unique identifier is assigned to the transfer
- a rule of transfer is selected (in Recv mode)
- an identifier of the user is selected
- the name of the file to download

##### Visual comportment
The file download is the usual download from the web browser, except, through 
integration, the check of the hash computed by the Waarp Web Server to check 
within the client's browser. Currently, there is a demo feature where you click
on the button once downloaded the file, select the file downloaded and it checks
the hash according to the very last item downloaded from the Waarp Web Server.

Therefore, there is no resumable download yet, since javascript is not able to 
write directly to a local file (security reasons). However a javascript can check
the downloaded file once it is done.

#### Waarp Web Server perspective
From Waarp Web Server perspective, this transfer is as a "usual" transfer, using the standard
database of Waarp product. 

##### Functional Comportment
The unique identifier is given by the client. 

The hash is computed by the server and sent to the client, which allows the 
client to check against it.

The rule is given by the client. The Waarp Web Server will apply all pre and post
tasks, according to the rule used, and also the error tasks if any.

Therefore, it allows to use this transfer to execute a task that will manage the content
of this file from another application, potentially also embedded by the same Web
Application server.


## 2) Integration focus

### 2.1) Web Integration

You can use any Servlet server accepting 3.1 servlet versions (such as Jetty, Tomcat).

- Install the `WaarpHttp-X.Y.Z-jar-with-dependencies.jar` (containing all dependencies) 
within an application directory of a web server, such as Tomcat or Jetty.
- Create a `webapp` directory for WaarpHttp, containing the files and subdirectories
 as in `WaarpHttp/src/main/webapp`
- In the WEB-INF directory, define the necessary entries in the `web.xml`, such 
 as the one in example in  `WaarpHttp/src/main/webapp/WEB-INF/web.xml`

For instance, for Jetty, the following configuration can be used:

- Path: `/WaarpHttp`
- WebApp directory: `/WaarpHttp/src/main/webapp`
- WebApp Class directory: where you put the `WaarpHttp-X.Y.Z-jar-with-dependencies.jar`
- Extra argument for Jetty: `-Dlogback.configurationFile=/WaarpHttp/test/resources/logback.xml`

Therefore the url of the two servlet will be:
- `http://server:port/WaarpHttp/resumable` (upload)
- `http://server:port/WaarpHttp/download` (download)

### 2.2) Missing integration

#### Authentication

In the prototype, even if the authentication relies on Waarp repository, it is
currently hard coded in the javascript of the web page. So you should obviously
use the authentication of your company and integrates it within the code by 
specifying the right class to use for authentication (see `authentClassName` in `web.xml`).

#### Selection of rule to apply

In the prototype, even if the authentication relies on Waarp repository, it is
currently hard coded in the javascript of the web page. So you should obviously
either change it to a specific fixed value or propose a way for the final user to
select a correct value, according to his/her rights.

#### Selection of file to download (Download)

The prototype is very simple with fixed files. One should take care to create the 
file before running the Jetty server (`test.txt` and `test2.bin` in `/tmp/R66/out` 
directory). Take a look at the
Junit tests (`DownloadServletTest`) to see what to send and what is returned.
But obviously you should provide a way to propose various files for the end user
according to his/her rights.

#### Initialization of the database

To run the prototype, you should initialize the database. To do it simply,
you can use the test named `InitDatabase` by commenting the `@Ignore` specification. 

#### Extra elements

One could integrate also some extra features such as:
- testing if a file was already sent before (Hash based comparison)
- allowing to see historic of transfers
- ... 

### 2.3) Web integration for Upload

##### Content of `web.xml`

   - `servlet-class` points to the Java Servlet, here `org.waarp.http.protocol.servlet.UploadServlet`
       - One could write another servlet implementation, inspiring from this default servlet
   - `init-param` contains the parameter `r66config` which has to point to the xml R66 configuration file
   - `init-param` contains the parameter `authentClassName` which has to point to the Java implementation
     for the authentication, here simple one is `org.waarp.http.protocol.servlet.HttpAuthentDefault`
       - One could write another authentication implementation, inspiring from this default servlet
   - `servlet-mapping` points to the Java Servlet name, here `UploadServlet` and its `url-pattern`
       - Note this has to be different than Download, and remember that the configuration of the Application
         server could prepend an extra url
    

        <servlet>
            <servlet-name>UploadServlet</servlet-name>
            <servlet-class>org.waarp.http.protocol.servlet.UploadServlet</servlet-class>
            <load-on-startup>1</load-on-startup>
            <init-param>
                <param-name>r66config</param-name>
                <param-value>/tmp/R66/conf/config-serverA-minimal.xml</param-value>
            </init-param>
            <init-param>
                <param-name>authentClassName</param-name>
                <param-value>org.waarp.http.protocol.servlet.HttpAuthentDefault</param-value>
            </init-param>
        </servlet>
        <servlet-mapping>
            <servlet-name>UploadServlet</servlet-name>
            <url-pattern>/resumable</url-pattern>
        </servlet-mapping>

##### Content of `index.html`

A prototype file is given. As this is only an example, the user authentication and
rule selection is fixed in the javascript part as:

      <script>
      // Configuration to adapt with real authentication and selection of rule (send) to apply
            var user = "test";
            var key = "ef99dfce1fdaa787cef4701368f79cfb";
            var rulename = "rule3";
            var comment = "Web Upload";
            setIdHiddenHash("finalhash"); // Used by compute-hash.js to store the result
      </script>

`finalhash` is the id of an hidden input field, such as:

      <input type="hidden" name="finalhash" id="finalhash" value="">

Therefore, one should adapt this page or service to add a specific authentication
system and a way to select a rule accordingly, as to provide a specific comment if any.

The methods used are:
 
    GET/POST http(s)://server-url/base-url/resumable
      body or url arguments:
        "user": username/servername from Waarp repository
        "key": associated key for this user
        "rulename": rulename from Waarp repository
        "comment": actual comment to set for this transfer
        // Below parameters are set by the external javascript
        "resumableChunkNumber": the current chunk
        "resumableChunkSize": the size of this chunk
        "resumableTotalSize": the total size of the file
        "resumableIdentifier": unique identifier, set using Javascript function uuidv4()
        "resumableFilename": the filename for the upload
        "resumableRelativePath": relative path of the file
    Answer:
        GET: 404 Transfer or Chunk not found, 200 Transfer chunk found
        POST: 201 First chunk ok (creation of the transfer), 200 All chunks received
        Both: 500 Internal error such as wrong authentication or arguments (prevents DDOS)

One could also change some general options of Resumable.js javascript module:

    var r = new Resumable({
         target:'/WaarpHttp/resumable', // Target for Waarp servlet according to Application Web server
         forceChunkSize: true, // Recommended
         chunkSize:1*1024*1024, // Might be less but not less than 1024
         simultaneousUploads:1, // Mandatory
         testChunks: true, // Not mandatory, one could skip existence of previous chunks
         throttleProgressCallbacks:1,
         method: "multipart", // "octet" for url args + chunk in body, "multipart" for standard UPLOAD
         generateUniqueIdentifier: uuidv4, // Recommended
         preprocessFile: initSha256, // Recommended
         query: queryArgs // Recommended
       }); 


### 2.4) Web integration for the Download

##### Content of `web.xml`

   - `servlet-class` points to the Java Servlet, here `org.waarp.http.protocol.servlet.DownloadServlet`
       - One could write another servlet implementation, inspiring from this default servlet
   - `init-param` contains the parameter `r66config` which has to point to the xml R66 configuration file
   - `init-param` contains the parameter `authentClassName` which has to point to the Java implementation
     for the authentication, here simple one is `org.waarp.http.protocol.servlet.HttpAuthentDefault`
       - One could write another authentication implementation, inspiring from this default servlet
   - `servlet-mapping` points to the Java Servlet name, here `DownloadServlet` and its `url-pattern`
       - Note this has to be different than Upload, and remember that the configuration of the Application
         server could prepend an extra url

        
        <servlet>
            <servlet-name>DownloadServlet</servlet-name>
            <servlet-class>org.waarp.http.protocol.servlet.DownloadServlet</servlet-class>
            <load-on-startup>1</load-on-startup>
            <init-param>
                <param-name>r66config</param-name>
                <param-value>/tmp/R66/conf/config-serverA-minimal.xml</param-value>
            </init-param>
            <init-param>
                <param-name>authentClassName</param-name>
                <param-value>org.waarp.http.protocol.servlet.HttpAuthentDefault</param-value>
            </init-param>
        </servlet>
        <servlet-mapping>
            <servlet-name>DownloadServlet</servlet-name>
            <url-pattern>/download</url-pattern>
        </servlet-mapping>

##### Content of `index.html`

This file is not written for Download, since the listing of the available files is out of the
 responsibility of this module. But mainly, the code is to apply a call such as:
 
    GET/POST http(s)://server-url/base-url/download
      body or url arguments:
        "user": username/servername from Waarp repository
        "key": associated key for this user
        "rulename": rulename from Waarp repository
        "comment": actual comment to set for this transfer
        "identifier": unique identifier, preferably using Javascript function uuidv4()
        "filename": filename to download (according to rule)
    Answer:
       header: 
        "X-Hash-Sha-256": HEX representation of SHA-256 of downloaded file
       body:
        the file
     
    HEAD http(s)://server-url/base-url/download
          body or url arguments:
            "user": username/servername from Waarp repository
            "key": associated key for this user
            "rulename": rulename from Waarp repository
            "comment": actual comment to set for this transfer
            "identifier": unique identifier, preferably using Javascript function uuidv4()
            "filename": filename to download (according to rule)
        Answer: 404 if not found, 202 if still on going, 200 if done
           header: 
            "X-Hash-Sha-256": HEX representation of SHA-256 of downloaded file if status 200
 
 

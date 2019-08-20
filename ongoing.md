Done
====
All
***
- Fix await on Netty Future
- Fix await on R66Future
- Fix Bootstrap (deprecated 2 thread groups)
- Remove unused main()
- Remove @author for Waarp team
- Add FIXME for wrong usage of DbAdmin, clean elsewhere
- Effort to remove duplicate codes
- Clean dependencies (not used)

Common
******
- Add ConcurrentUtility.newConcurrentSet() utility (deprecation from Netty)
- R66Future deprecation of await/awaitUniterruptibly for awaitOrInterrupted
- Fix XML reading if possible (compatible with reformat)

FTP
***
- Fix Threads

R66
***
- Fix Threads (still some left)
- Fix memory (mainly KeepAlive and Startup/Validation)
- Fix Business request
- Add a specific file for Version of protocol (stick to 3.1.0)

New Done
========

All
***
- Clean dependencies (not used)
- Fix memory
- Change to Guava where possible
- Fix various coding issues

Digest
******
- Fix SecureRandom for Windows (Sun provider) and Linux (urandom)
- Add Global configuration for SecureRandom
- Add support for TLS 1.2 using Native Netty Uber jar, but reverting to
 standard of JRE if not possible

Common
******
- Finalize GUID and IntegerUUID implementation (new global transfer uid
 and current localchannel uuid) (deprecation of UUID)
- Fix WaarpShutdownHook exit issue
- Add ThreadLocalRandom for Java 6

R66
***
- Fix Threads usage (roughly R66: 2xCLIENT + 2xRUNNER(LIMIT) + 1xSERVER, 
HTTP/REST: 1xCLIENT)
- Remove LocalChannel and associated threads (LocalChannel tasks now are
 based on the map of requests and simulation of localChannels through
  method calls and using LocalChannelReference object)
- Fix memory (mainly KeepAlive and Startup/Validation)
- Add simple Junit (Selenium) on Monitor and Admin Web
- Add Junits on snmp and http (UI) and monitoring
- Test with just start and stop for every DB types (notice: Oracle Bad Driver
 since Java 6 differs on jdbc url)
- Add Junits for Java Tasks (AddDigest, AddUuid)
- Add Junits for SpooledDirectoryTransfer
- Extract XML definition (deduplication of codes with Proxy)
- OWASP security fixes
- checkParameters on REST/RESTV2/HTTP method 
- Fix issue on Commander not found any request of transfer

R66 Proxy
*********
- Add simple Junit (Selenium) on Monitor and Admin Web
- Add Junits

GatewayKernel
*************
- Add simple Junit Rest
- Improve CPU usage (mainly StringBuilder)
- checkParameters on REST/HTTP method 

FTP
***
- Change SSL to TLS in FTPS connection

GWFTP
*****
- Fix memory (mainly KeepAlive)
- Add Junits


New New Done
============

All
***
- Improve some Junits
- Improve build by including local repository (increase the size of the
 project however)
 

Common
******
- Remove PassThrough file implementation (never used)

FTP
***
- Add XSITE DIGEST and XDIGEST (CRC32, ADLER32, MD5, MD2, SHA-1/SHA1, SHA-256
/SHA256, SHA-384/SHA384, SHA-512/SHA512) and Junits
- Add Junit tests on FTPS (native and dynamic)

R66
***
- Fix some closing functions not called
- Add simple Junit for Transfer and ExecMove Tasks
- Kernel Exec (Execute, Java, LocalExec, LogJava, 
    R66PreparedTransfer) moved to GGFTP (better place)

TODO
====

All
***
- +Allow 2 kinds of configuration (xml / json)

R66
***
- +Add Junits on forward send
- +Add Junits on rest/restV2
- +Check if properties used?
- +Test for XML DAO ?
- +Test for RescheduleTransfer and Transfer and SpooledInform and
  RestartServer and LinkRename and FtpTransfer and ExecMove Tasks
- +Test for kernel Exec (Execute, Java, LocalExec, LogJava, 
    R66PreparedTransfer) ?
- +Look at REST V2
- +See if Rest V1 is available while V2 active (right now only V2 active)
- +Vitam plugin


VitamPlugin
***********
L'objet est de permettre l'ingest (upload dans Vitam) de SIP (fichier binaire
 ZIP) d'archives dans Vitam sans passer par un HttpUpload mais via Waarp pour
  assurer une qualité de délivrance et un suivi. L'objet est de pouvoir aussi
   en retour effectuer la transmission du fichier ATR (fichier d'accusé de r
   éception de Vitam du traitement Ingest).
   
VitamPlugin devra opérer selon la logique suivante:

1)  Après réception via Waarp d'un fichier avec des informations pour Vitam re
çues

- Déclencher le plugin Vitam (extension Waarp)
- Pour cela, appeler via le client IngestExternalClient la méthode

   
        ingest a file that has been uploaded locally on a vitam folder then launch an ingest workflow
   
        * @param vitamContext the vitam context
        * @param localFile the localFile information
        * @param contextId a type of ingest among "DEFAULT_WORKFLOW" (Sip ingest), "HOLDING_SCHEME" (tree) 
        *        "FILING_SCHEME" (plan) and "BLANK_TEST" (Sip ingest test)
        * @param action an action as a string among "RESUME" (launch workflow entirely) and "NEXT" (launch ingest in step
        *        by step mode)
        * @return response
        * @throws IngestExternalException
        */
       RequestResponse<Void> ingestLocal(VitamContext vitamContext, LocalFile localFile,
           String contextId,
           String action)
           throws IngestExternalException;

- Arguments:
  - vitamContext = fonction du client ou des informations de transfert
   (attention: information de sécurité donc à contrôler) ; contient :
    -  Integer tenantId;
    -  String accessContract;
    -  String applicationSessionId;
    -  String personalCertificate;
  - localFile = le fichier à transmettre à Vitam (path)
  - contextId = information transmise par la transfert (entre
   DEFAULT_WORKFLOW, HOLDING_SCHEME, FILING_SCHEME, BLANK_TEST) 
  - action = "RESUME" (pas de mode test via NEXT)
 
- Celle-ci fait appel au server Ingest External sur la méthode suivante
:  
    
    
         * upload a local file
         *
         * @param contextId the context id of upload
         * @param action in workflow
         * @param localFile local file
         * @param asyncResponse the asynchronized response
         */
    
        @Path("ingests")
        @POST
        @Consumes(MediaType.APPLICATION_JSON)
        @Secured(permission = "ingests:local:create",
            description = "Envoyer un SIP en local à Vitam afin qu'il en réalise l'entrée")
        public void uploadLocal(@HeaderParam(GlobalDataRest.X_CONTEXT_ID) String contextId,
            @HeaderParam(GlobalDataRest.X_ACTION) String action, LocalFile localFile,
            @Suspended final AsyncResponse asyncResponse)
            
            
2) Check Ingest et retour:

- Si Vitam est configuré pour un MOVE post traitement, il est possible
     par scan du répertoire de déclencher une tentative de retour du résultat
      de l'ingest via l'appel (tant que non prêt, refaite = spooling):
- Sinon, il est possible, une fois le retour reçu, si KO => transmission du
 problème, si OK, spooling de la ressource:
      
      
           * Download archive transfer reply stored by Ingest operation
           * <p>
           * Return the archive transfer reply as stream asynchronously<br/>
           * <br/>
           * <b>The caller is responsible to close the Response after consuming the inputStream.</b>
           *
           * @param objectId the id of archive transfer reply to download
           * @return response
           */
          @GET
          @Path("/ingests/{objectId}/archivetransferreply")
          @Produces(MediaType.APPLICATION_OCTET_STREAM)
          @Secured(permission = "ingests:id:archivetransfertreply:read",
                  description = "Récupérer l'accusé de récéption pour une opération d'entrée donnée")
          public Response downloadArchiveTransferReplyAsStream(@PathParam("objectId") String objectId)
          
- via le client: en utilisant:
    - vitamContext = similaire à l'appel précédent (conservation de la donnée
     en référence à prévoir via l'ObjectId)
    - objectId = celui reçu dans la réponse à l'ingest
    - type = IngestCollection.ARCHIVETRANSFERREPLY


     * Download object stored by ingest operation<br>
     * <br>
     * <b>The caller is responsible to close the Response after consuming the inputStream.</b>
     * 
     *
     *
     * @param vitamContext the vitam context
     * @param objectId
     * @param type
     * @return object as stream
     * @throws VitamClientException
     */
    Response downloadObjectAsync(VitamContext vitamContext, String objectId,
        IngestCollection type)
        throws VitamClientException;
        
3) Documentation
- http://www.programmevitam.fr/ressources/DocCourante/html/manuel-integration/client-usage.html
- http://www.programmevitam.fr/ressources/DocCourante/raml/externe/ingest.html
- http://www.programmevitam.fr/ressources/DocCourante/javadoc/fr/gouv/vitam/ingest/external/client/IngestExternalClient.html
- http://www.programmevitam.fr/ressources/DocCourante/html/archi/archi-applicative/20-services-list.html#api-externes-ingest-external-et-access-external
- http://www.programmevitam.fr/ressources/DocCourante/html/archi/archi-exploit-infra/services/ingest-external.html
- http://www.programmevitam.fr/ressources/DocCourante/html/archi/securite/00-principles.html#principes-de-securisation-des-acces-externes

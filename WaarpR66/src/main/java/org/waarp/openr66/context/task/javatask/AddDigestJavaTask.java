/**
   This file is part of Waarp Project.

   Copyright 2009, Frederic Bregier, and individual contributors by the @author
   tags. See the COPYRIGHT.txt in the distribution for a full listing of
   individual contributors.

   All Waarp Project is free software: you can redistribute it and/or 
   modify it under the terms of the GNU General Public License as published 
   by the Free Software Foundation, either version 3 of the License, or
   (at your option) any later version.

   Waarp is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with Waarp .  If not, see <http://www.gnu.org/licenses/>.
 */
package org.waarp.openr66.context.task.javatask;

import java.io.IOException;

import org.waarp.common.database.exception.WaarpDatabaseException;
import org.waarp.common.digest.FilesystemBasedDigest;
import org.waarp.common.digest.FilesystemBasedDigest.DigestAlgo;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.openr66.context.task.AbstractExecJavaTask;

/**
 * Add a digest in the TransferInformation to the current Task.</br>
 * This should be called on caller side in pre-task since the transfer information will be transfered just after.</br>
 * The second argument is -digest followed by one of ADLER32 CRC32 MD2 MD5 SHA1 SHA256 SHA384 SHA512, default being MD5.</br>
 * The third argument, optional, is "-format" followed by a string containing "#DIGEST#" to be replaced by the digest 
 * and starting with - or +, meaning this will be added at the beginning or the end of the generated new string. Default is equivalent to "-format -##DIGEST##".</br>
 * </br>
 * To be called as: <task><type>EXECJAVA</type><path>org.waarp.openr66.context.task.javatask.AddDigestJavaTask -digest ADLER32|CRC32|MD2|MD5|SHA1|SHA256|SHA384|SHA512 [-format (-/+)##DIGEST##]</path></task> 
 * 
 * @author "Frederic Bregier"
 *
 */
public class AddDigestJavaTask extends AbstractExecJavaTask {
    /**
     * Internal Logger
     */
    private static final WaarpLogger logger = WaarpLoggerFactory
            .getLogger(AddDigestJavaTask.class);

    private static final String sDIGEST = "#DIGEST#";
    @Override
    public void run() {
        logger.debug(this.toString());
        String []args = fullarg.split(" ");
        String fileInfo = null;
        String format = "-##DIGEST##";
        String algo = "MD5";
        int way = -1;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("-format")) {
                format = args[i+1];
                if (format.charAt(0) == '-') {
                    way = -1;
                    format = format.substring(1);
                } else if (format.charAt(0) == '+') {
                    way = 1;
                    format = format.substring(1);
                }
                i++;
            } else if (args[i].equals("-digest")) {
                algo = args[i+1].toUpperCase();
                i++;
            }
        }
        DigestAlgo digest = null;
        try {
            digest = DigestAlgo.valueOf(algo);
        } catch (Exception e) {
            logger.error("Bad algorithm format: " + algo);
            this.status = 3;
            return;
        }
        String key;
        try {
            key = FilesystemBasedDigest.getHex(FilesystemBasedDigest.getHash(this.session.getFile().getTrueFile(), true, digest));
        } catch (IOException e1) {
            logger.error("Digest not correctly computed: " + algo, e1);
            this.status = 4;
            return;
        }
        logger.debug("Replace Digest in {} way: {} digest: {} key: {}", format, way, algo, key);
        if (way < 0) {
            fileInfo = format.replace(sDIGEST, key) + " " + this.session.getRunner().getFileInformation();
        } else {
            fileInfo = this.session.getRunner().getFileInformation() + " " + format.replace(sDIGEST, key);
        }
        this.session.getRunner().setFileInformation(fileInfo);
        try {
            this.session.getRunner().update();
        } catch (WaarpDatabaseException e) {
            logger.error("Digest cannot be saved to fileInformation:" + fileInfo);
            this.status = 2;
            return;
        }
        logger.debug("Digest saved to fileInformation:" + fileInfo);
        this.status = 0;
    }
}

/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2023
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package misc;

import java.io.File;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import common.Log;

public class ScanTest {
    private final static Logger LOGGER = Logger.getLogger(ScanTest.class.getName());

    /**
     * @param args
     */
    public static void main(String[] args) {
        try {
            Log.initTrace();

            System.out.println(new Date() + " Starting");

            List<File> meDirList = new LinkedList<File>();
            File topDir = new File(args[0]);
            for (File typeDir : topDir.listFiles()) {
                for (File rnsDir : typeDir.listFiles()) {
                    for (File meDir : rnsDir.listFiles()) {
                        meDirList.add(meDir);
                    }
                }
            }
            System.out.println(new Date() + " Found " + meDirList.size() + " dirs");

            int fileCount = 0;
            int dirCount = 0;
            for (File meDir : meDirList) {
                dirCount++;
                if (LOGGER.isLoggable(Level.FINE))
                    LOGGER.fine("dir=" + meDir.getName() + ", dirCount=" + dirCount);

                for (File file : meDir.listFiles()) {
                    file.getName();
                    fileCount++;
                }
            }
            System.out.println(new Date() + " Done fileCount=" + fileCount);
        } catch (Throwable t) {
            t.printStackTrace();
        }
        System.exit(0);
    }

}

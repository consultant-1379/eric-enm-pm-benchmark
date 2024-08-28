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
package common;

import java.io.File;

public class PathMapper {
    private static PathMapper instance;

    private String rootDirs[] = null;
    private int dirIndex = 0;

    public static synchronized PathMapper getInstance() {
        if (instance == null) {
            instance = new PathMapper();
        }
        return instance;
    }

    public void setRootDirs(final String rootDir) {
        this.rootDirs = rootDir.split(",");
        for ( final String dir : this.rootDirs) {
            File rootDirObj = new File(dir);
            if (!rootDirObj.exists()) {
                System.out.println("ERROR: " + rootDir + " doesn't exist");
                System.exit(1);
            }
        }
    }

    public synchronized String getNextRootDir() {
        final String result = rootDirs[dirIndex];
        dirIndex++;
        if ( dirIndex >= rootDirs.length) {
            dirIndex = 0;
        }
        return result;
    }

    public String[] getRootDirs() {
        return rootDirs;
    }
}

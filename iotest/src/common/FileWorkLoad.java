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

import java.io.Serializable;

public class FileWorkLoad implements Serializable {
    public final int numFiles;
    public final int size;
    public final String type;

    public FileWorkLoad(int numFiles, int size, String type) {
        this.numFiles = numFiles;
        this.size = size;
        this.type = type;
    }
}
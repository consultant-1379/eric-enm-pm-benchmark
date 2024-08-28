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

public class FileInfo implements Serializable {
    public final String filePath;
    public final int size;
    public final String type;

    public FileInfo(String filePath, int size, String type) {
        this.filePath = filePath;
        this.size = size;
        this.type = type;
    }
}
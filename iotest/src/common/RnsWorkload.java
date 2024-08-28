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
import java.util.List;

public class RnsWorkload implements Serializable {
    public final int id;
    public List<FileWorkLoad> rncFileList;
    public final int rncBandWidth;
    public int numOfNodes;
    public List<FileWorkLoad> nodeBFileList;
    public final int nodeBbandWidth;

    RnsWorkload(int id, List<FileWorkLoad> rncFileList, int rncBandWidth,
            int numOfNodes, List<FileWorkLoad> nodeBFileList, int nodeBbandWidth) {
        this.id = id;

        this.rncFileList = rncFileList;
        this.rncBandWidth = rncBandWidth;

        this.numOfNodes = numOfNodes;
        this.nodeBFileList = nodeBFileList;
        this.nodeBbandWidth = nodeBbandWidth;
    }
}
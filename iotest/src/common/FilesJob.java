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

/**
 * A list of files to read/write for a node
 */
public class FilesJob implements Serializable {
    public enum Type { READ, WRITE }

    final public String node;
    final public List<FileInfo> files;
    final public Type type;
    final public Integer priority;

    public FilesJob(final String node, final List<FileInfo> files, FilesJob.Type type, final Integer priority) {
        this.node = node;
        this.files = files;
        this.type = type;
        this.priority = priority;
    }
}

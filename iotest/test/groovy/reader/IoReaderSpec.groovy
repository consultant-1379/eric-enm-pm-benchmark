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
package reader

import common.FileInfo
import common.IReadWriteMonitor
import common.ReadWriteMonitorFactory
import common.RopMonitor
import reader.FilesReader
import spock.lang.Specification

class IoReaderSpec extends Specification {

    public static final int priority = 10;
    public static final  nodeName = "RNC00001";
    public static final  filePath = "testfiles/loaddir2/";
    public static final List<FileInfo> files = new LinkedList<FileInfo>();
    public static final String period= "202301040625:60";
    public static final RopMonitor ropMonitor = new RopMonitor(period);
    public static final IReadWriteMonitor rwMonitor = ReadWriteMonitorFactory.create();


    FilesReader filesReader ;

//    FilesJob filejob = new FilesJob();

    def setup() {
        files.add(new FileInfo(filePath, 0, "CTR"));
        filesReader = new FilesReader(nodeName, files, ropMonitor, rwMonitor, priority)
    }


    def 'set file reader'() {
        given:
        when:
        setup()
        then:
        (filesReader.nodeName).equals(nodeName)
        filesReader.priority == 10
    }

    def 'get file read size'() {
        given:
        String filePath = "testfiles/loaddir2/"
        FileInfo fi = new FileInfo(filePath, 0, "CTR")
        when:
        def fileReadSize = filesReader.getIoSize(fi)
        then:
        fileReadSize == 8
    }


}
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
package writer;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import common.FileInfo;
import common.FileLookupService;
import common.FileWorkLoad;
import common.FilesJob;
import common.IFilesJobsExecutor;
import common.RnsWorkload;
import common.RopMonitor;
import common.RopScheduler;

public class WriteScheduler extends RopScheduler {
    private ExecutorService flsEs = Executors.newCachedThreadPool();
    private final Set<String> typeDirCache = new HashSet<>();

    public WriteScheduler(final IFilesJobsExecutor es, final List<RnsWorkload> workload, final int period) {
        super(es, workload, period, 0);
    }

    @Override
    public FilesJob generateJob(String rootDir, String node, String nodeDir, String rop,
            List<FileWorkLoad> fileWorkLoadList) throws Exception {

        final List<FileInfo> files = new LinkedList<FileInfo>();
        for (FileWorkLoad fwl : fileWorkLoadList) {
            // Check if the file type dir exists, creation of the
            // node level directory is done in FilesWriter
            final String typeDir = rootDir + "/" + fwl.type;
            if (! typeDirCache.contains(typeDir) ) {
                final File typeDirObj = new File(typeDir);
                if ( !typeDirObj.exists() ) {
                    if (!typeDirObj.mkdirs()) {
                        throw new Exception("Failed to create " + typeDir);
                    }
                }
                typeDirCache.add(typeDir);
            }

            final String dir = typeDir + "/" + nodeDir;
            final String filenameBase = dir + "/" + rop + "_" + periodTag + ".0000-0000-0000_" + nodeDir + "_"
                    + fwl.type + ".";
            for (int fileIndex = 1; fileIndex <= fwl.numFiles; fileIndex++) {
                files.add(new FileInfo(filenameBase + fileIndex, fwl.size, fwl.type));
            }
        }
        final Integer priority = Integer.parseInt(periodTag);
        return new FilesJob(node, files, FilesJob.Type.WRITE,priority);
    }

    @Override
    public void preExec(final String rop, final RopMonitor ropMonitor, final List<FilesJob> jobs) {
        final Runnable saveRop = new Runnable() {
            public void run() {
                try {
                    ropMonitor.awaitCompletion();

                    final Map<String, List<String>> filesByType = new HashMap<>();
                    for (final FilesJob job : jobs) {
                        for (final FileInfo fileInfo : job.files) {
                            List<String> filesForType = filesByType.get(fileInfo.type);
                            if (filesForType == null) {
                                filesForType = new LinkedList<>();
                                filesByType.put(fileInfo.type, filesForType);
                            }
                            filesForType.add(fileInfo.filePath);
                        }
                    }

                    for (Map.Entry<String, List<String>> entry : filesByType.entrySet()) {
                        FileLookupService.getInstance().storeRop(
                                entry.getValue(),
                                entry.getKey(),
                                WriteScheduler.this.period,
                                rop);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        flsEs.submit(saveRop);
    }
}

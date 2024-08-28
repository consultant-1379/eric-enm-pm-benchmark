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
package reader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import common.FileInfo;
import common.FileLookupService;
import common.FileWorkLoad;
import common.FilesJob;
import common.IFilesJobsExecutor;
import common.RnsWorkload;
import common.RopScheduler;

public class ReadScheduler extends RopScheduler {
    private final static Logger logger = Logger.getLogger(ReadScheduler.class.getName());

    public ReadScheduler(final IFilesJobsExecutor es, final List<RnsWorkload> workload, int peroid) {
        super(es, workload, peroid, -1);
    }

    @Override
    protected List<FilesJob> generateJobs(String ropName) throws Exception {
        logger.fine("generateJobs: period=" + this.period + ", ropName=" + ropName);

        final Set<String> typesInRop = new HashSet<>();
        for (final RnsWorkload rns : workload) {
            if (rns.rncFileList != null && rns.rncFileList.size() > 0) {
                for (final FileWorkLoad fileWorkLoad : rns.rncFileList) {
                    typesInRop.add(fileWorkLoad.type);
                }
            }
            if (rns.nodeBFileList != null && rns.nodeBFileList.size() > 0) {
                for (final FileWorkLoad fileWorkLoad : rns.nodeBFileList) {
                    typesInRop.add(fileWorkLoad.type);
                }
            }
        }
        logger.fine("generateJobs: period=" + this.period + ", typesInRop=" + typesInRop);

        final String rop = ropName.substring(1); // Drop the A
        logger.fine("generateJobs: period=" + this.period + ", rop=" + rop);

        final Map<String, List<FileInfo>> filesToRead = new HashMap<>();

        for (final String type : typesInRop) {
            final List<String> filePaths = FileLookupService.getInstance().readRop(type, period, rop);
            for (final String filePath : filePaths) {
                final String pathParts[] = filePath.split("/");
                final String node = pathParts[pathParts.length - 2]
                        .substring(pathParts[pathParts.length - 2].lastIndexOf("=") + 1);
                List<FileInfo> filesForNode = filesToRead.get(node);
                if (filesForNode == null) {
                    filesForNode = new LinkedList<>();
                    filesToRead.put(node, filesForNode);
                }
                filesForNode.add(new FileInfo(filePath, 0, type));
            }
        }

        final List<FilesJob> jobs = new ArrayList<>(filesToRead.size());
        final Integer priority = 10;
        for (final Map.Entry<String, List<FileInfo>> entry : filesToRead.entrySet()) {
            jobs.add(new FilesJob(entry.getKey(), entry.getValue(), FilesJob.Type.READ,priority));
        }

        logger.fine("generateJobs: period=" + this.period + ", #jobs=" + jobs.size());

        return jobs;
    }

    public FilesJob generateJob(String rootDir, String node, String nodeDir,
            String rop, List<FileWorkLoad> fileWorkLoadList)
            throws Exception {
        throw new Exception("Should not be called");
    }
}

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

import java.util.HashMap;
import java.util.Map;

public class JobExecutorFactory {
    private static JobExecutorFactory instance;

    private Map<FilesJob.Type, IJobTypeExecutorFactory> typeFactories = new HashMap<>();

    public static synchronized JobExecutorFactory getInstance() {
        if ( instance == null ) {
            instance = new JobExecutorFactory();
        }
        return instance;
    }

    public void registerJobExecutorFactory( FilesJob.Type type, IJobTypeExecutorFactory factory) {
        typeFactories.put(type, factory);
    }

    public Runnable getTypeExecutor(final FilesJob job, final IRopMonitor ropMonitor) {
        return typeFactories.get(job.type).createExecutor(job, ropMonitor);
    }
}

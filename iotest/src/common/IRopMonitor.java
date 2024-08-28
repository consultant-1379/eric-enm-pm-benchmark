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

public interface IRopMonitor {
    void jobStarted();

    void jobCompleted(String nodeName, int files, int kb, long openTime, long closeTime, long jobTime);

    void jobsCompleted(int jobs, int files, int kb, long openTime, long closeTime, long jobTime);

    void setOutliers(long maxOpenTime, long minOpenTime, long maxCloseTime, long minCloseTime, long maxIOtime, long minIOtime);

    public void awaitCompletion();

    public String getDescription();
}
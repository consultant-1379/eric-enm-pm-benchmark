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

public class SleepTest {

    /**
     * @param args
     */

    public static void main(String[] args) {
        long sleepTime = Long.parseLong(args[0]);
        int iterCount = Integer.parseInt(args[1]);
        Object testObj = new Object();
        synchronized (testObj) {
            try {
                for (int index = 0; index < 2; index++) {

                    long startTime = System.currentTimeMillis();
                    for (int i = 0; i < iterCount; i++) {
                        if (index == 0)
                            Thread.sleep(sleepTime);
                        else
                            testObj.wait(sleepTime, 0);
                    }

                    long endTime = System.currentTimeMillis();

                    System.out.println((endTime - startTime) / iterCount);
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

}

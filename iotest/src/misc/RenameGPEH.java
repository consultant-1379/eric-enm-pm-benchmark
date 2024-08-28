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
package misc;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import common.FileWorkLoad;
import common.RnsWorkload;
import common.WorkloadReader;

public class RenameGPEH {
    public static void main(String args[]) {
        try {
            new RenameGPEH().run(args);
        } catch (Throwable t) {
            t.printStackTrace();
        }
        System.exit(0);
    }

    private void run(String args[]) throws Exception {
        WorkloadReader wlr = new WorkloadReader(args[1]);
        Map<Integer, List<RnsWorkload>> workloads = wlr.parseWorkload();

        Map<String, Integer> rncRopType = new HashMap<String, Integer>();
        for (Integer ropPeriod : workloads.keySet()) {
            List<RnsWorkload> workload = workloads.get(ropPeriod);
            for (RnsWorkload rns : workload) {
                if (rns.rncFileList != null && rns.rncFileList.size() > 0) {
                    for (FileWorkLoad fwl : rns.rncFileList) {
                        if (fwl.type.equals("GPEH")) {
                            String rnsDir = "RNS" + rns.id;
                            rncRopType.put(rnsDir, ropPeriod);
                        }
                    }
                }
            }
        }

        File gpehDir = new File(args[0] + "/GPEH");
        for (String dir : gpehDir.list()) {
            Integer period = rncRopType.get(dir);
            System.out.println(dir + " " + period);
            if (period != null) {
                int renamedCount = processDir(gpehDir.getAbsolutePath() + "/" + dir, period);
                if (renamedCount > 0)
                    System.out.println("Renamed " + renamedCount + " files");
            }
        }
    }

    private int processDir(String dir, Integer period) throws Exception {
        String rnsList[] = new File(dir).list();
        String rncName = null;
        for (String rnsEntry : rnsList) {
            if (rnsEntry.startsWith("RNC"))
                rncName = rnsEntry;
        }
        File rncDir = new File(dir + "/" + rncName);
        File files[] = rncDir.listFiles();

        String periodTag = String.format("%02d", period.intValue());
        int renamedCount = 0;
        for (File file : files) {
            String fileName = file.getName();
            if (fileName.startsWith("A")) {
                String ropPeriod = fileName.substring(14, 16);
                if (!periodTag.equals(ropPeriod)) {
                    String newName = fileName.replaceFirst("_" + ropPeriod, "_" + periodTag);
                    File newFile = new File(rncDir, newName);
                    file.renameTo(newFile);
                    renamedCount++;
                }
            }
        }
        return renamedCount;
    }

}

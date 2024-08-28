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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class FileLookupService {
    private static FileLookupService instance;
    private final String flsRootDir;

    public static synchronized FileLookupService getInstance() {
        if (instance == null) {
            instance = new FileLookupService();
        }

        return instance;
    }

    private FileLookupService() {
        flsRootDir = PathMapper.getInstance().getRootDirs()[0] + "/.fls";
    }

    private String getRopDir(final String type, final int period) {
        return String.format("%s/%s/%d", flsRootDir, type, period);
    }

    public void storeRop(final List<String> files, final String type, final int period, final String rop)
            throws Exception {
        final String dir = getRopDir(type, period);
        final File dirObj = new File(dir);
        if (!dirObj.exists()) {
            if (!dirObj.mkdirs()) {
                throw new Exception("Failed to create " + dir);
            }
        }
        final String tmpOutputFile = String.format("%s/.%s", dir, rop);
        final GZIPOutputStream outputStream = new GZIPOutputStream(new FileOutputStream(tmpOutputFile));
        final PrintWriter out = new PrintWriter(new OutputStreamWriter(outputStream));
        for (final String file : files) {
            out.println(file);
        }
        out.close();

        final String outputFile = String.format("%s/%s", dir, rop);
        Files.move(Paths.get(tmpOutputFile), Paths.get(outputFile));
    }

    public List<String> readRop(final String type, final int period, final String rop) {
        final String file = String.format("%s/%s", getRopDir(type, period), rop);
        final List<String> results = new LinkedList<>();
        try {
            GZIPInputStream inputStream = new GZIPInputStream(new FileInputStream(file));
            final BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = in.readLine()) != null) {
                results.add(line);
            }
            in.close();
        } catch (Throwable t) {
//            t.printStackTrace();
            Log.log("FileNotFound: " + t.getMessage());
        }


        return results;
    }

    public void removeRop(final String type, final int period, final String rop) throws Exception {
        final String file = String.format("%s/%s", getRopDir(type, period), rop);
        Files.deleteIfExists(Paths.get(file));
    }

    public List<String> listRop(final String type, final int period) throws Exception {
        final List<String> results = new LinkedList<>();
        final File dir = new File(getRopDir(type, period));
        if (dir.exists()) {
            for (final File file : dir.listFiles()) {
                final String fileName = file.getName();
                if (!fileName.startsWith(".")) {
                    results.add(fileName);
                }
            }
        }
        return results;
    }
}

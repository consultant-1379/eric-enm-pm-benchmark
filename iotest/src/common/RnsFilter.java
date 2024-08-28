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

import java.io.FileReader;
import java.io.LineNumberReader;
import java.util.HashSet;
import java.util.Set;

public class RnsFilter {
	private static RnsFilter singleton;
	private Set<Integer> filterSet = null;

	RnsFilter() {
		String rnsFilterFile = System.getProperty("rnsfilter");
		Log.log("RnsFilter rnsfilter=" + rnsFilterFile);
		if ( rnsFilterFile == null )
			return;

		try {
			filterSet = new HashSet<Integer>();
			LineNumberReader in = new LineNumberReader(new FileReader(rnsFilterFile));
			String line;
			while ( (line = in.readLine()) != null ) {
				String[] parts = line.split(" ");
				int start = Integer.parseInt(parts[0]);
				int end = Integer.parseInt(parts[1]);			
				for ( int i = start; i <= end; i++ )
					filterSet.add(Integer.valueOf(i));

			}
			in.close();
		} catch ( Throwable t ) {
			System.out.println("Failed to read rnsFilterFile" + rnsFilterFile);
			t.printStackTrace();
			System.exit(1);
		}
	}

	public boolean check(Integer id) {
		if ( filterSet == null )
			return true;
		else 
			return filterSet.contains(id);
	}

	public static synchronized RnsFilter getInstance() {
		if ( singleton == null )
			singleton = new RnsFilter();
		return singleton;
	}

}

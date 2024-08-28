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
package common

import common.RnsFilter;
import common.PathMapper
import spock.lang.Specification
//import javax.inject.Inject
//import com.ericsson.cds.cdi.support.rule.ObjectUnderTest


class RnsFilterSpec extends Specification {

//    @ObjectUnderTest
//    RnsFilter rnsFilter

//    @Inject
//    PathMapper pathMapper

    RnsFilter rnsFilter = new RnsFilter();
    PathMapper pathMapper = new PathMapper();

    def 'verify filter'() {
        given:
//        rnsFilter.filterSet = null
        int id = 1
        when:
        def obj = rnsFilter.check(id)
        then:
        obj.equals(true)
    }


    def 'verify root directory'() {
        given:
        final String rootDir = "testfiles/loaddir1,testfiles/loaddir2";
        when:
        pathMapper.setRootDirs(rootDir)
        then:
        (pathMapper.getRootDirs()).length == 2

    }

    def 'get next root filter'() {
        given:
        String rootDir = "testfiles/loaddir1,testfiles/loaddir2"
        pathMapper.setRootDirs(rootDir)
        pathMapper.dirIndex = 0
        when:
        String obj = pathMapper.getNextRootDir()
        then:
        obj.equals("testfiles/loaddir1")
    }
}
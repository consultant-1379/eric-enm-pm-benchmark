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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class WorkloadReader {
    private Element docEle;

    public WorkloadReader(String workloadFile) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document dom = db.parse(workloadFile);
        docEle = dom.getDocumentElement();
    }

    public Map<String, Integer> parseRetention() throws Exception {
        Map<String, Integer> result = new HashMap<String, Integer>();
        NodeList nl = docEle.getElementsByTagName("retention");
        if (nl != null && nl.getLength() > 0) {
            for (int i = 0; i < nl.getLength(); i++) {
                Element retEl = (Element) nl.item(i);
                String type = retEl.getAttribute("type");
                Integer value = Integer.valueOf(retEl.getAttribute("value"));
                result.put(type, value);

                Log.log("WorkloadReader: Rentention " + type + " " + value);
            }
        }

        return result;
    }

    public Map<String, Integer> parseWriteSize() throws Exception {
        Map<String, Integer> result = new HashMap<String, Integer>();
        NodeList nl = docEle.getElementsByTagName("writesize");
        if (nl != null && nl.getLength() > 0) {
            for (int i = 0; i < nl.getLength(); i++) {
                Element retEl = (Element) nl.item(i);
                String type = retEl.getAttribute("type");
                Integer value = Integer.valueOf(retEl.getAttribute("value"));
                result.put(type, value);

                Log.log("WorkloadReader: Write Size " + type + " " + value);
            }
        }

        return result;
    }

    public Map<Integer, List<RnsWorkload>> parseWorkload() throws Exception {
        Map<Integer, List<RnsWorkload>> result = new HashMap<Integer, List<RnsWorkload>>();

        NodeList nl = docEle.getElementsByTagName("rop");
        if (nl != null && nl.getLength() > 0) {
            for (int i = 0; i < nl.getLength(); i++) {
                Element ropEl = (Element) nl.item(i);
                int ropId = Integer.parseInt(ropEl.getAttribute("id"));
                Log.log("WorkloadReader: Workload ROP " + ropId);
                NodeList rnsList = ropEl.getElementsByTagName("rns");
                List<RnsWorkload> rnsWl = new LinkedList<RnsWorkload>();
                if (rnsList != null && rnsList.getLength() > 0) {
                    for (int j = 0; j < rnsList.getLength(); j++) {
                        Element rnsEl = (Element) rnsList.item(j);
                        int start = Integer.parseInt(rnsEl.getAttribute("start"));
                        int count = Integer.parseInt(rnsEl.getAttribute("count"));
                        int numOfNodes = Integer.parseInt(rnsEl.getAttribute("numOfNodes"));
                        Log.log("WorkloadReader: Workload RNS start=" + start + " count=" + count + " numOfNodes="
                                + numOfNodes);

                        Log.log("WorkloadReader: Workload RNC FileList");
                        List<FileWorkLoad> rncWl = parseWlList("rnc", rnsEl);
                        int rncBW = getBandwidth("rnc", rnsEl);
                        Log.log("WorkloadReader: Workload NodeBandwidth FileList");
                        List<FileWorkLoad> nodeBWl = parseWlList("nodeBandwidth", rnsEl);
                        int nodeBbw = getBandwidth("nodeBandwidth", rnsEl);

                        for (int id = start; id < (start + count); id++) {
                            rnsWl.add(new RnsWorkload(id, rncWl, rncBW, numOfNodes, nodeBWl, nodeBbw));
                        }
                    }
                }
                result.put(Integer.valueOf(ropId), rnsWl);
            }
        }

        return result;
    }

    private int getBandwidth(String nodeType, Element rnsEl) throws Exception {
        int result = 0;

        NodeList nl = rnsEl.getElementsByTagName(nodeType);
        if (nl != null && nl.getLength() > 0) {
            Element el = (Element) nl.item(0);
            result = Integer.parseInt(el.getAttribute("bandwidth"));
        }

        return result;
    }

    private List<FileWorkLoad> parseWlList(String nodeType, Element rnsEl) throws Exception {
        NodeList nl = rnsEl.getElementsByTagName(nodeType);
        if (nl != null && nl.getLength() > 0) {
            Element el = (Element) nl.item(0);
            List<FileWorkLoad> result = new LinkedList<FileWorkLoad>();
            NodeList fwlList = el.getElementsByTagName("fileworkload");
            if (fwlList != null && fwlList.getLength() > 0) {
                for (int i = 0; i < fwlList.getLength(); i++) {
                    Element fwlEl = (Element) fwlList.item(i);
                    String fileType = fwlEl.getAttribute("type");
                    int numfiles = Integer.parseInt(fwlEl.getAttribute("numfiles"));
                    int filesize = Integer.parseInt(fwlEl.getAttribute("filesize"));
                    Log.log("WorkloadReader: Workload " + numfiles + " " + fileType + " files of size " + filesize
                            + " KB");
                    result.add(new FileWorkLoad(numfiles, filesize, fileType));
                }
            }
            return result;
        } else {
            return null;
        }
    }

}

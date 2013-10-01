package nl.openbeelden;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class XmlToCsv { 
	public static void main(String[] args) throws XPathExpressionException, ParserConfigurationException, SAXException, IOException {  
		//writeVideoTagsToCsv();
		writeTagVideoToCsv();
		writeDatasetToCsv();
	}
		
	private static void writeVideoTagsToCsv() throws XPathExpressionException, ParserConfigurationException, SAXException, IOException {
		List<List<String>> result = new ArrayList<List<String>>();
		NodeList nodes = getNodes("tags.xml", "tags/tag");
		
		//Add headers
		List<String> headers = new ArrayList<String>();
		headers.add("VideoName");
		headers.add("VideoNumber");
		headers.add("Plays");
		headers.add("Tags");
		result.add(headers);
		
		//Add rows
		for (int i = 0; i < nodes.getLength(); i++) {
			Node tag = nodes.item(i);
	          
	        if (tag.getNodeType() == Node.ELEMENT_NODE) {
	            Element firstTag = (Element)tag;
	            String tagName = firstTag.getElementsByTagName("name").item(0).getChildNodes().item(0).getNodeValue();
	            NodeList videos = firstTag.getElementsByTagName("video");
	            
	            for (int k = 0; k < videos.getLength(); k++){
	            	Node video = videos.item(k);
	            	String videoName = "";
	    			String videoNumber = "";
	    			String plays = "";
	            	if (video.getNodeType() == Node.ELEMENT_NODE) {
		                Element firstVideo = (Element)video;
		                videoName = firstVideo.getElementsByTagName("title").item(0).getChildNodes().item(0).getNodeValue();
		                videoNumber = firstVideo.getAttribute("number");
		                plays = firstVideo.getElementsByTagName("plays").item(0).getChildNodes().item(0).getNodeValue();
		            }
	            	
	            	boolean contains = false;
	            	
	            	for (int l = 0; l < result.size(); l++) {
	            		List<String> temprow = result.get(l);
	            		if (temprow.contains(videoNumber)){
	            			String tags = temprow.get(3);
	            			tags = tags + ";" + tagName.trim();
	            			temprow.set(3, tags);
	            			l = result.size();
	            			contains = true;
	            		}
	            	}
            		
            		if (!contains) {
    	            	List<String> row = new ArrayList<String>();
    	    			row.add(videoName.trim());
    	    			row.add(videoNumber);
    	    			row.add(plays);
    	    			row.add(tagName.trim());
    	    			result.add(row);	            			
            		}
	            }
	        }    
		}

		writeToCSV(result, "videotag.csv");
	}
	
	private static void writeTagVideoToCsv() throws XPathExpressionException, ParserConfigurationException, SAXException, IOException {
		List<List<String>> result = new ArrayList<List<String>>();
		NodeList nodes = getNodes("tags.xml", "tags/tag");
		
		//Add headers
		List<String> headers = new ArrayList<String>();
		headers.add("Tag");
		headers.add("VideoName");
		headers.add("VideoNumber");
		headers.add("Plays");
		headers.add("Total Videos");
		result.add(headers);
		
		//Add rows
		for (int i = 0; i < nodes.getLength(); i++) {
			Node tag = nodes.item(i);
	          
	        if (tag.getNodeType() == Node.ELEMENT_NODE) {
	            Element firstTag = (Element)tag;
	            String tagName = firstTag.getElementsByTagName("name").item(0).getChildNodes().item(0).getNodeValue();
	            System.out.println(tagName.trim());
	            Element mediatotal = (Element) firstTag.getElementsByTagName("media").item(0);
	            String totalVideos = mediatotal.getAttribute("total");
	            NodeList videos = firstTag.getElementsByTagName("video");
	            
	            if (videos.getLength() == 0) {
	            	List<String> row = new ArrayList<String>();
	            	row.add(tagName.trim());
	            	row.add("");
	            	row.add("");
	            	row.add("");
	            	row.add(totalVideos);
	            	result.add(row);
	            }
	            else {
		            for (int k = 0; k < videos.getLength(); k++){
		            	Node video = videos.item(k);
		            	String videoName = "";
		    			String videoNumber = "";
		    			String plays = "";
		            	if (video.getNodeType() == Node.ELEMENT_NODE) {
			                Element firstVideo = (Element)video;
			                videoName = firstVideo.getElementsByTagName("title").item(0).getChildNodes().item(0).getNodeValue();
			                videoNumber = firstVideo.getAttribute("number");
			                plays = firstVideo.getElementsByTagName("plays").item(0).getChildNodes().item(0).getNodeValue();
			            }
		            	            		
		            	List<String> row = new ArrayList<String>();
		    			row.add(tagName.trim());
		    			row.add(videoName.trim());
		    			row.add(videoNumber);
		    			row.add(plays);
		    			row.add(totalVideos);
		    			result.add(row);	
		            }
	            }
	        }    
		}

		//writeToCSV(result, "tagvideo.csv");
	}
	  
	private static void writeDatasetToCsv() throws XPathExpressionException, ParserConfigurationException, SAXException, IOException {
			List<List<String>> result = new ArrayList<List<String>>();
			NodeList nodes = getNodes("dataset.xml", "OAI-PMH/ListRecords/record/metadata//*[name()='oai_oi:oi']");
			
			//Add headers
			List<String> headers = new ArrayList<String>();
			headers.add("Title");
			headers.add("Subject");
			headers.add("Description");
			headers.add("Abstract");
			headers.add("Date");
			headers.add("Extent");
			headers.add("Language");
			headers.add("AttributionURL");
			result.add(headers);

			for (int i = 0; i < nodes.getLength(); i++){
				Node video = nodes.item(i);
				
		        if (video.getNodeType() == Node.ELEMENT_NODE) {
		            Element firstVideo = (Element)video;
		            String title = firstVideo.getElementsByTagName("oi:title").item(0).getChildNodes().item(0).getNodeValue();
		            //All subjects
		            String subject = "";
		            NodeList subjects = firstVideo.getElementsByTagName("oi:subject");
		            for (int j = 0; j < subjects.getLength(); j++) {
		            	Node subjectNode = subjects.item(j);
		            	if (subjectNode.getChildNodes() != null && subjectNode.getChildNodes().item(0) != null)
		            		subject = subject + subjectNode.getChildNodes().item(0).getNodeValue() + ";";
		            }
		            String description = "";
		            if(firstVideo.getElementsByTagName("oi:description").item(0).getChildNodes().item(0) != null)
		            	description = firstVideo.getElementsByTagName("oi:description").item(0).getChildNodes().item(0).getNodeValue();
		            String abstr = "";
		            if (firstVideo.getElementsByTagName("oi:abstract").item(0).getChildNodes().item(0) != null)
		            	abstr = firstVideo.getElementsByTagName("oi:abstract").item(0).getChildNodes().item(0).getNodeValue();
		            String date = firstVideo.getElementsByTagName("oi:date").item(0).getChildNodes().item(0).getNodeValue();
		            String extent = firstVideo.getElementsByTagName("oi:extent").item(0).getChildNodes().item(0).getNodeValue();
		            extent = timeToSeconds(extent);
		            String language = "";
		            if (firstVideo.getElementsByTagName("oi:language").item(0).getChildNodes().item(0) != null)
		            	language = firstVideo.getElementsByTagName("oi:language").item(0).getChildNodes().item(0).getNodeValue();
		            String attrurl = firstVideo.getElementsByTagName("oi:attributionURL").item(0).getChildNodes().item(0).getNodeValue();
		            attrurl = rewriteAttributionUrl(attrurl);
		            
			        ArrayList<String> row = new ArrayList<String>();
			        row.add(title);
			        row.add(subject);
			        row.add(description);
			        row.add(abstr);
			        row.add(date);
			        row.add(extent);
			        row.add(language);
			        row.add(attrurl);
			        result.add(row);
		        }
			}
			
			writeToCSV(result, "dataset.csv");
	}
	
	private static String timeToSeconds(String extent) {
		//Format is PT1M36S
		String result = "";
		result = extent.replace("PT", "");
		
		if (result.contains("M")) {
			int m = result.indexOf("M");
			int minutesInSeconds = Integer.parseInt(result.substring(0, m)) * 60;
			int seconds = Integer.parseInt(result.substring(m+1, result.indexOf("S")));
			result = String.valueOf(minutesInSeconds + seconds);
		}
		else {
			result = result.replace("S", "");
		}
		
		return result;
	}

	private static String rewriteAttributionUrl(String url) {
		    String result = "";    
		    String[] results = url.split("/");
		    result = results[4];    
		    return result;
	}

	private static NodeList getNodes(String dataset, String expression) throws ParserConfigurationException, SAXException, IOException, XPathExpressionException {
		    DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();   
		    domFactory.setIgnoringComments(true);
		    DocumentBuilder builder = domFactory.newDocumentBuilder();
		    Document doc = builder.parse(new File(dataset));
		    XPath xPath = XPathFactory.newInstance().newXPath();
		    NodeList nodeList = (NodeList) xPath.compile(expression).evaluate(doc,XPathConstants.NODESET);
		    return nodeList;
	}


	  private static void writeToCSV(List<List<String>> results, String filename) throws IOException {
		    boolean created = false;
		    File file = new File(filename);
		    
		    if (!file.exists()) {
		      file.createNewFile();
		      created = true;
		    }
		    
		    FileWriter writer = new FileWriter(file.getName(), true);
		    //TODO: Check if rows are not empty.
		    if (results.size() == 0) {
		      System.out.println("No results Found.");
		    }
		    else {
		      if (created) {
		        for (String header : results.get(0)) {
		          writer.append(header.replaceAll(",", ";") + ",");
		        }
		        writer.append('\n');
		      }
		      
		      List<List<String>> rows = results;
		      for (int i = 1; i < rows.size(); i++) {
		        List<String> row = rows.get(i);
		        for (String column : row) {
		          writer.append(column.replaceAll(",", ";") + ",");
		        }
		        writer.append('\n');
		      }      
		    }

		    writer.flush();
		    writer.close();
	  }
}

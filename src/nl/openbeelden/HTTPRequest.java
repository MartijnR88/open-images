package nl.openbeelden;

import java.net.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.io.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.lang3.StringEscapeUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/** 
 * 
 * This class retrieves metadata from OpenBeelden and writes it to a XML file
 * 
 * */
public class HTTPRequest {
	private XPath xPath = XPathFactory.newInstance().newXPath();
	private int count = 0;
	private static final String MERGE_FILENAME = "mergedmetadata.xml";
	
	/** Writes the XML file that is read from an URL to a DOM format and use the method writeDomToFile to store it */
	public void writeXMLtoDom(String urlToRead)
			throws ParserConfigurationException, SAXException, IOException,
			TransformerException, XPathExpressionException {
		URL url;
		HttpURLConnection conn;
		BufferedReader rd;
		String line;
		String result = "";
		//Set up connection
		try {
			url = new URL(urlToRead);
			conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			rd = new BufferedReader(
					new InputStreamReader(conn.getInputStream(), "UTF-8"));
			//Add everything that is read to a String
			while ((line = rd.readLine()) != null) {
				result += line;
			}
			rd.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		// Parse requested string to DOM
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document document = builder.parse(new InputSource(new StringReader(
				result)));
		//Write DOM to a file
		writeDomToFile(document);
		
		//Get the resumptiontoken
		String expression = "OAI-PMH/ListRecords/resumptionToken";
		NodeList nodeList = (NodeList) xPath.compile(expression).evaluate(document,
				XPathConstants.NODESET);
		String token = "";
		if (nodeList != null) {
			System.out.println("NodeList is not null");
			if (nodeList.item(0) != null) {
				System.out.println("Item(0) is not null");
				if (nodeList.item(0).getFirstChild() != null)
					token = nodeList.item(0).getFirstChild().getNodeValue();
			}
		}
		
		System.out.println(token);
		//As long as there are more results (pagination is used), retrieve them
		if (token != ""){
			writeXMLtoDom("http://openbeelden.nl/feeds/oai/?verb=ListRecords&resumptionToken=" + token);
		}
	}
	
	/** Write a Dom to a File */
	public void writeDomToFile(Document document) throws TransformerException{
		// Write DOM to XML file
				TransformerFactory transformerFactory = TransformerFactory
						.newInstance();
				Transformer transformer = transformerFactory.newTransformer();
				DOMSource source = new DOMSource(document);
				StreamResult stream = new StreamResult(new File("metadata" + count + ".xml"));
				transformer.transform(source, stream);
				System.out.println(count);
				count = count + 1;
	}
	
	/** Because all pages (from the pagination) are stored as a seperate XML file, they have to be merged into one big dataset */
	public void mergeXMLFiles() throws TransformerException, SAXException, IOException, ParserConfigurationException {
		DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();   
		domFactory.setIgnoringComments(true);  
		DocumentBuilder builder = domFactory.newDocumentBuilder();   
		
		Document doc = builder.parse(new File("metadata0.xml"));
		NodeList nodes = doc.getElementsByTagName("record");
		
		//Start with 1, because 0 is done before. 33 is hardcoded the number of pages that are retrieved and thus the number of files to be merged.
		for (int i = 1; i < 42; i++){
			Document doctemp = builder.parse(new File("metadata" + i + ".xml"));
			NodeList nodestemp = doctemp.getElementsByTagName("record");
			
			for(int j = 0; j < nodestemp.getLength(); j++){
				Node n = (Node) doc.importNode(nodestemp.item(j), true);  
				nodes.item(j).getParentNode().appendChild(n);
			}  	
		}

		//Write the merged DOM to a XML file
		Transformer transformer = TransformerFactory.newInstance().newTransformer();  
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");  
		StreamResult stream = new StreamResult(new File(MERGE_FILENAME));
		DOMSource source = new DOMSource(doc);  
		transformer.transform(source, stream);  
	}
	
	/** Removes duplicate nodes from a XML file */
	public void removeDuplicateNodes() throws ParserConfigurationException, SAXException, IOException, TransformerFactoryConfigurationError, TransformerException, XPathExpressionException{
		DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();   
		domFactory.setIgnoringComments(true);  
		DocumentBuilder builder = domFactory.newDocumentBuilder();   
		
		//Retrieve the data
		Document doc = builder.parse(new File(MERGE_FILENAME));
		NodeList nodeList = (NodeList) xPath.compile("OAI-PMH/ListRecords/record/header/identifier").evaluate(doc,
				XPathConstants.NODESET);
		ArrayList<String> duplicates = new ArrayList<String>();
		
		//Check for every node if there is a duplicate in the list
		for (int i = 0; i < nodeList.getLength(); i++){
			String n = nodeList.item(i).getFirstChild().getNodeValue();
			for (int j = i+1; j < nodeList.getLength(); j++) {
				if (n.equals(nodeList.item(j).getFirstChild().getNodeValue()) && !duplicates.contains(n))
					duplicates.add(n);
			}
		}
				
		//Remove the duplicates
		for (int k = 0; k < duplicates.size(); k++) {
			NodeList list = doc.getElementsByTagName("record");
			for (int l = 0; l < list.getLength(); l++){
				String id = list.item(l).getChildNodes().item(1).getChildNodes().item(1).getFirstChild().getNodeValue();
				if (duplicates.contains(id)){
					duplicates.remove(id);
					NodeList listRecords = doc.getElementsByTagName("ListRecords");
					listRecords.item(0).removeChild(list.item(l));
				}
			}
		}
		
		//Write the list without duplicates to a file
		Transformer transformer = TransformerFactory.newInstance().newTransformer();  
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");  
		StreamResult stream = new StreamResult(new File("dataset.xml"));
		DOMSource source = new DOMSource(doc);  
		transformer.transform(source, stream);  
	}

	/** Parse a XML file to a DOM */
	public Document getMetadata() throws SAXException, IOException,
			ParserConfigurationException, TransformerException,
			XPathExpressionException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document document = builder.parse(new FileInputStream("dataset.xml"));
		
		return document;
	}
	
	/** Returns the total items in the dataset */
	public int getTotalItems() throws XPathExpressionException, SAXException, IOException, ParserConfigurationException, TransformerException{		
		return getMetadata().getElementsByTagName("record").getLength();
	}

	/** Get a list of nodes from a DOM. Which nodes is specified by the expression */
	public List<String> getNodes(Document document, String expression)
			throws XPathExpressionException {
		ArrayList<String> result = new ArrayList<String>();
			NodeList nodeList = (NodeList) xPath.compile(expression).evaluate(document,
					XPathConstants.NODESET);
			for (int i = 0; i < nodeList.getLength(); i++) {
				if (nodeList.item(i).getFirstChild() != null){
					result.add(nodeList.item(i).getFirstChild().getNodeValue());
				}
				else {
					result.add("null");
				}
			}

		return result;
	}
	
	/** Print some statistics about the dataset, like the total number of occurences of a certain feature. Sorted by occurence. */
	public Map<String, Integer> getStatistics(List<String> nodes){
		Map<String, Integer> result = new HashMap<String, Integer>();
		
		result.put("Total number of nodes", nodes.size());
		
		//Retrieve all keys and if something was already in the list add 1
		for (int i = 0; i < nodes.size(); i++){
			String description = nodes.get(i);
			if (result.containsKey(description)){
				int total = result.get(description);
				result.put(description, total+1);
			}
			else {
				result.put(description, 1);
			}
		}
		
		//Sort the keys by occurence
		Set<String> keys = result.keySet();
		ArrayList<String> removeKeys = new ArrayList<String>();
		for (Iterator<String> it = keys.iterator(); it.hasNext(); ) {
			String key = it.next();
			if (result.get(key) == 1 && !key.equals("null")){
				removeKeys.add(key);
			}
		}
		
		result.put("Single values", keys.size());
		
		for (int j = 0; j < removeKeys.size(); j++){
			result.remove(removeKeys.get(j));
		}
		
		//Create sorted map
		ValueComparator bvc = new ValueComparator(result);
		TreeMap<String, Integer> sorted_map = new TreeMap<String, Integer>(bvc);
		sorted_map.putAll(result);
		
		return sorted_map;
	}
	
	/** Print the statistics that you want */
	public void printStatistics() throws XPathExpressionException, SAXException, IOException, ParserConfigurationException, TransformerException{
		PrintWriter out = new PrintWriter("statistics.txt");
		out.println("Total numer of items: " + getTotalItems());
		out.println("Title:");
		out.println(this.getStatistics(this.getNodes(this.getMetadata(), "OAI-PMH/ListRecords/record/metadata//*[name()='oai_oi:oi']//*[name()='oi:title'][@lang='nl']")));
		out.println("\n");
		out.println("Alternative (optioneel)");
		out.println(this.getStatistics(this.getNodes(this.getMetadata(), "OAI-PMH/ListRecords/record/metadata//*[name()='oai_oi:oi']//*[name()='oi:alternative'][@lang='nl']")));
		out.println("\n");
		out.println("Creator:");
		out.println(this.getStatistics(this.getNodes(this.getMetadata(), "OAI-PMH/ListRecords/record/metadata//*[name()='oai_oi:oi']//*[name()='oi:creator'][@lang='nl']")));
		out.println("\n");
		out.println("Subject:");
		out.println(this.getStatistics(this.getNodes(this.getMetadata(), "OAI-PMH/ListRecords/record/metadata//*[name()='oai_oi:oi']//*[name()='oi:subject'][@lang='nl']")));
		out.println("\n");
		out.println("Description:");
		out.println(this.getStatistics(this.getNodes(this.getMetadata(), "OAI-PMH/ListRecords/record/metadata//*[name()='oai_oi:oi']//*[name()='oi:description'][@lang='nl']")));
		out.println("\n");
		out.println("Abstract");
		out.println(this.getStatistics(this.getNodes(this.getMetadata(), "OAI-PMH/ListRecords/record/metadata//*[name()='oai_oi:oi']//*[name()='oi:abstract'][@lang='nl']")));
		out.println("\n");
		out.println("Publisher:");
		out.println(this.getStatistics(this.getNodes(this.getMetadata(), "OAI-PMH/ListRecords/record/metadata//*[name()='oai_oi:oi']//*[name()='oi:publisher'][@lang='nl']")));
		out.println("\n");
		out.println("Contributor:");
		out.println(this.getStatistics(this.getNodes(this.getMetadata(), "OAI-PMH/ListRecords/record/metadata//*[name()='oai_oi:oi']//*[name()='oi:contributor']")));
		out.println("\n");
		out.println("Date:");
		out.println(this.getStatistics(this.getNodes(this.getMetadata(), "OAI-PMH/ListRecords/record/metadata//*[name()='oai_oi:oi']//*[name()='oi:date']")));
		out.println("\n");
		out.println("Type:");
		out.println(this.getStatistics(this.getNodes(this.getMetadata(), "OAI-PMH/ListRecords/record/metadata//*[name()='oai_oi:oi']//*[name()='oi:type']")));
		out.println("\n");
		out.println("Extent");
		out.println(this.getStatistics(this.getNodes(this.getMetadata(), "OAI-PMH/ListRecords/record/metadata//*[name()='oai_oi:oi']//*[name()='oi:extent']")));
		out.println("\n");
		out.println("Medium:");
		out.println(this.getStatistics(this.getNodes(this.getMetadata(), "OAI-PMH/ListRecords/record/metadata//*[name()='oai_oi:oi']//*[name()='oi:medium']")));
		out.println("\n");
		out.println("Identifier:");
		out.println(this.getStatistics(this.getNodes(this.getMetadata(), "OAI-PMH/ListRecords/record/metadata//*[name()='oai_oi:oi']//*[name()='oi:identifier']")));
		out.println("\n");
		out.println("Source:");
		out.println(this.getStatistics(this.getNodes(this.getMetadata(), "OAI-PMH/ListRecords/record/metadata//*[name()='oai_oi:oi']//*[name()='oi:source'][@lang='nl']")));
		out.println("\n");
		out.println("Language:");
		out.println(this.getStatistics(this.getNodes(this.getMetadata(), "OAI-PMH/ListRecords/record/metadata//*[name()='oai_oi:oi']//*[name()='oi:language']")));
		out.println("\n");
		out.println("References:");
		out.println(this.getStatistics(this.getNodes(this.getMetadata(), "OAI-PMH/ListRecords/record/metadata//*[name()='oai_oi:oi']//*[name()='oi:references'][@lang='nl']")));
		out.println("\n");
		out.println("Spatial:");
		out.println(this.getStatistics(this.getNodes(this.getMetadata(), "OAI-PMH/ListRecords/record/metadata//*[name()='oai_oi:oi']//*[name()='oi:spatial'][@lang='nl']")));
		out.println("\n");
		out.println("Attribution Name:");
		out.println(this.getStatistics(this.getNodes(this.getMetadata(), "OAI-PMH/ListRecords/record/metadata//*[name()='oai_oi:oi']//*[name()='oi:attributionName'][@lang='nl']")));
		out.println("\n");
		out.println("Attribution URL:");
		out.println(this.getStatistics(this.getNodes(this.getMetadata(), "OAI-PMH/ListRecords/record/metadata//*[name()='oai_oi:oi']//*[name()='oi:attributionURL']")));
		out.println("\n");
		out.println("License:");
		out.println(this.getStatistics(this.getNodes(this.getMetadata(), "OAI-PMH/ListRecords/record/metadata//*[name()='oai_oi:oi']//*[name()='oi:license']")));
		out.println("\n");
		out.close();		
	}
	
	/** Create XML files for all different portals */
	public void createXMLFiles() throws XPathExpressionException, ParserConfigurationException, SAXException, IOException, TransformerException{
		this.writeXMLtoDom("http://openbeelden.nl/feeds/oai/?verb=ListRecords&metadataPrefix=oai_oi&set=beeldengeluid");
		this.writeXMLtoDom("http://openbeelden.nl/feeds/oai/?verb=ListRecords&metadataPrefix=oai_oi&set=eclap");
		this.writeXMLtoDom("http://openbeelden.nl/feeds/oai/?verb=ListRecords&metadataPrefix=oai_oi&set=euscreen");
		this.writeXMLtoDom("http://openbeelden.nl/feeds/oai/?verb=ListRecords&metadataPrefix=oai_oi&set=nimk");
		this.writeXMLtoDom("http://openbeelden.nl/feeds/oai/?verb=ListRecords&metadataPrefix=oai_oi&set=rotterdam");
		this.writeXMLtoDom("http://openbeelden.nl/feeds/oai/?verb=ListRecords&metadataPrefix=oai_oi&set=stichting_natuurbeelden");
		this.writeXMLtoDom("http://openbeelden.nl/feeds/oai/?verb=ListRecords&metadataPrefix=oai_oi&set=UvA_Theaterwetenschap");
		this.writeXMLtoDom("http://openbeelden.nl/feeds/oai/?verb=ListRecords&metadataPrefix=oai_oi&set=vpro");		
	}

	public static void main(String[] args) throws SAXException, IOException,
			ParserConfigurationException, TransformerException,
			XPathExpressionException {
		HTTPRequest request = new HTTPRequest();
		//request.createXMLFiles();
		request.mergeXMLFiles();
		request.removeDuplicateNodes();
		//int total = request.getTotalItems();
		//System.out.println(total);
		//request.printStatistics();
	}
}

class ValueComparator implements Comparator<String> {

    Map<String, Integer> base;
    public ValueComparator(Map<String, Integer> base) {
        this.base = base;
    }

    // Note: this comparator imposes orderings that are inconsistent with equals.    
    public int compare(String a, String b) {
        if (base.get(a) >= base.get(b)) {
            return -1;
        } else {
            return 1;
        } // returning 0 would merge keys
    }
}
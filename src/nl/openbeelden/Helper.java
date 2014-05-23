package nl.openbeelden;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
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
import org.xml.sax.SAXException;

import com.google.api.services.samples.youtube.cmdline.Dataset;
import com.google.api.services.samples.youtube.cmdline.YouTubeRetriever;

public class Helper {	
	private static XPath xPath = XPathFactory.newInstance().newXPath();

	public static void main(String[] args) throws IOException, XPathExpressionException, SAXException, ParserConfigurationException, TransformerException {
//		Dataset dataset = new Dataset();
//		List<String> ids = dataset.getIdentifiers();
//		
//		for (String id : ids) {
//			String mp4 = YouTubeRetriever.correctPath(id) + ".mp4";
//			File f = new File(mp4);
//			if(!f.exists()) {
//				System.out.println(mp4);
//			}
//		}
		
//		List<String> files = dataset.getUniqueMediums();
//		int counter = 0;
//		
//		for (int i = 0; i < files.size(); i++) {
//			URL website = new URL(files.get(i));
//			try {
//				ReadableByteChannel rbc = Channels.newChannel(website.openStream());
//				String[] stringParts = files.get(i).split("/");
//				System.out.println(stringParts[stringParts.length-1]);
//				String[] formatParts = stringParts[stringParts.length-1].split("\\.");
//				String format = formatParts[formatParts.length-1];
//				System.out.println(format);
//				String id = YouTubeRetriever.correctPath(ids.get(i));
//				FileOutputStream fos = new FileOutputStream(id + "." + format);
//				fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
//			}
//			catch (Exception e) {
//				e.printStackTrace();
//			}
//			System.out.println(files.get(i));
//			counter++;
//			System.out.println(counter + "/2544");
//		}
		mergeXMLFiles();
	}
	
	public static void mergeXMLFiles() throws TransformerException, SAXException, IOException, ParserConfigurationException, XPathExpressionException {
		DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();   
		domFactory.setIgnoringComments(true);  
		DocumentBuilder builder = domFactory.newDocumentBuilder();   
		
		Document doc = builder.parse(new File("dataset.xml"));
		NodeList nodes = doc.getElementsByTagName("record");
		
		for (int i = 0; i < nodes.getLength(); i++) {
			NodeList metaData = nodes.item(i).getChildNodes().item(3).getChildNodes().item(1).getChildNodes();
						
			for (int j = 0; j < metaData.getLength(); j++) {
				System.out.println(metaData.item(j).getNodeName());
				if (!metaData.item(j).getNodeName().equals("oi:medium") && !metaData.item(j).getNodeName().equals("#text")) {
					//System.out.println("Before: " + metaData.item(j).getTextContent());
					//metaData.item(j).setTextContent(StringEscapeUtils.unescapeHtml4(metaData.item(j).getTextContent()));
					Node node;
					node = metaData.item(j);
					node.setTextContent(StringEscapeUtils.unescapeHtml4(node.getTextContent()));
					//nodes.item(i).getChildNodes().item(3).getChildNodes().item(1).getChildNodes().item(j).replaceChild(node, metaData.item(j));
					nodes.item(i).getChildNodes().item(3).getChildNodes().item(1).replaceChild(node, metaData.item(j));
					//System.out.println("After: " + nodes.item(i).getChildNodes().item(3).getChildNodes().item(1).getChildNodes().item(j).getTextContent());
				}
			}
			System.out.println("ITEM :" + i);
		}
		
		nodes.item(0).getChildNodes().item(3).getChildNodes().item(1).getTextContent();
		
		//Write the merged DOM to a XML file
		Transformer transformer = TransformerFactory.newInstance().newTransformer();  
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");  
		StreamResult stream = new StreamResult(new File("dataset_test.xml"));
		DOMSource source = new DOMSource(doc);  
		transformer.transform(source, stream);  
	}
}

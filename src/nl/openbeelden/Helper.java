package nl.openbeelden;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;

import org.xml.sax.SAXException;

import com.google.api.services.samples.youtube.cmdline.Dataset;

public class Helper {
	public static void main(String[] args) throws IOException, XPathExpressionException, SAXException, ParserConfigurationException, TransformerException {
		Dataset dataset = new Dataset();
		List<String> files = dataset.getMediums();
		
		System.out.println(files.size());
		
		for (String file : files) {
			//The extra_fragmenten file creates errors, so left out for now
			if (file.contains(".mpeg") && !file.contains("extra_fragmenten")) {
				URL website = new URL(file);
				ReadableByteChannel rbc = Channels.newChannel(website.openStream());
				String[] stringParts = file.split("/");
				FileOutputStream fos = new FileOutputStream(stringParts[stringParts.length-1]);
				fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
			}
		}		
	}
}

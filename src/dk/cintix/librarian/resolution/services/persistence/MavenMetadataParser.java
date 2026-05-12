package dk.cintix.librarian.resolution.services.persistence;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

public final class MavenMetadataParser {

    public List<String> parseVersions(InputStream xml) throws Exception {
        List<String> versions = new ArrayList<>();
        Document doc = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder().parse(xml);
        NodeList versionNodes = doc.getElementsByTagName("version");
        for (int i = 0; i < versionNodes.getLength(); i++) {
            versions.add(versionNodes.item(i).getTextContent().trim());
        }
        return versions;
    }

    public String parseLatest(InputStream xml) throws Exception {
        Document doc = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder().parse(xml);
        NodeList latestNodes = doc.getElementsByTagName("latest");
        if (latestNodes.getLength() > 0) {
            return latestNodes.item(0).getTextContent().trim();
        }
        return null;
    }
}

package com.quant.trade.support;

import com.quant.trade.dto.UiNode;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * 解析 uiautomator dump 导出的 XML。
 */
@Component
public class UiHierarchyParser {

    public List<UiNode> parse(String xml) {
        if (StringUtils.isBlank(xml)) {
            return List.of();
        }

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setExpandEntityReferences(false);
            Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
            NodeList nodes = document.getElementsByTagName("node");
            List<UiNode> result = new ArrayList<>(nodes.getLength());
            for (int i = 0; i < nodes.getLength(); i++) {
                org.w3c.dom.Node item = nodes.item(i);
                org.w3c.dom.NamedNodeMap attributes = item.getAttributes();
                result.add(new UiNode(
                        attr(attributes, "text"),
                        attr(attributes, "content-desc"),
                        attr(attributes, "resource-id"),
                        attr(attributes, "class"),
                        attr(attributes, "bounds"),
                        Boolean.parseBoolean(attr(attributes, "clickable"))
                ));
            }
            return result;
        } catch (Exception e) {
            throw new IllegalStateException("failed to parse ui hierarchy xml", e);
        }
    }

    private String attr(org.w3c.dom.NamedNodeMap attributes, String name) {
        org.w3c.dom.Node value = attributes == null ? null : attributes.getNamedItem(name);
        return value == null ? "" : value.getNodeValue();
    }
}

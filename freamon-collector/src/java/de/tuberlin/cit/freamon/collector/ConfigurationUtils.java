/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.tuberlin.cit.freamon.collector;

import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;

public abstract class ConfigurationUtils {

	public static void load(Map<String, String> conf, String path) throws IOException {
		try {
			DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
			docBuilderFactory.setIgnoringComments(true);
			DocumentBuilder builder = docBuilderFactory.newDocumentBuilder();
			Document doc = builder.parse(new FileInputStream(path));
			parseDocument(conf, doc);
		} catch (SAXException e) {
			throw new IOException(e);
		} catch (ParserConfigurationException e) {
			throw new IOException(e);
		}
	}

	private static void parseDocument(Map<String, String> conf, Document doc) throws IOException {
		try {
			Element root = doc.getDocumentElement();
			if (!"configuration".equals(root.getTagName())) {
				throw new IOException("bad conf file: top-level element not <configuration>");
			}
			NodeList props = root.getChildNodes();
			for (int i = 0; i < props.getLength(); i++) {
				Node propNode = props.item(i);
				if (!(propNode instanceof Element)) {
					continue;
				}
				Element prop = (Element) propNode;
				if (!"property".equals(prop.getTagName())) {
					throw new IOException("bad conf file: element not <property>");
				}
				NodeList fields = prop.getChildNodes();
				String attr = null;
				String value = null;
				for (int j = 0; j < fields.getLength(); j++) {
					Node fieldNode = fields.item(j);
					if (!(fieldNode instanceof Element)) {
						continue;
					}
					Element field = (Element) fieldNode;
					if ("name".equals(field.getTagName()) && field.hasChildNodes()) {
						attr = ((Text) field.getFirstChild()).getData().trim();
					}
					if ("value".equals(field.getTagName()) && field.hasChildNodes()) {
						value = ((Text) field.getFirstChild()).getData();
					}
				}
				if (attr != null && value != null) {
					conf.put(attr, value);
				}
			}
		} catch (DOMException e) {
			throw new IOException(e);
		}
	}

}

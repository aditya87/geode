/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.geode.internal.config;

import org.apache.commons.lang3.StringUtils;
import org.apache.geode.cache.configuration.XSDRootElement;
import org.apache.geode.internal.ClassPathLoader;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.XMLFilterImpl;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.sax.SAXSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

public class JAXBService {
  Marshaller marshaller;
  Unmarshaller unmarshaller;

  class XMLCacheTagInterceptor extends XMLFilterImpl {
    public XMLCacheTagInterceptor(XMLReader reader) {
      super(reader);
    }

    @Override
    public void startElement(String uri, String localName,
                             String qName, Attributes attributes)
            throws SAXException {
      if (qName.equals("cache")) {
        AttributesImpl overrideAttributes = new AttributesImpl();
        overrideAttributes.addAttribute("", "", "xsi:schemaLocation",
                "IDREFS",
                "http://geode.apache.org/schema/cache http://geode.apache.org/schema/cache/cache-1.0.xsd");
        overrideAttributes.addAttribute("", "", "version", "ID",
                "1.0");
        overrideAttributes.addAttribute("", "", "xmlns", "ID",
                "http://geode.apache.org/schema/cache");
        overrideAttributes.addAttribute("", "", "xmlns:xsi", "ID",
                "http://www.w3.org/2001/XMLSchema-instance");

        super.startElement("http://geode.apache.org/schema/cache", localName, qName,
                attributes);
      } else {
        super.startElement("http://geode.apache.org/schema/cache", localName, qName, attributes);
      }
    }
  }

  public JAXBService(Class<?>... xsdRootClasses) {
    try {
      JAXBContext jaxbContext = JAXBContext.newInstance(xsdRootClasses);
      marshaller = jaxbContext.createMarshaller();
      unmarshaller = jaxbContext.createUnmarshaller();
      marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
      marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);

      String schemas = Arrays.stream(xsdRootClasses).map(c -> {
        XSDRootElement element = c.getAnnotation(XSDRootElement.class);
        if (element != null && StringUtils.isNotEmpty(element.namespace())
            && StringUtils.isNotEmpty(element.schemaLocation())) {
          return (element.namespace() + " " + element.schemaLocation());
        }
        return null;
      }).filter(Objects::nonNull).collect(Collectors.joining(" "));

      marshaller.setProperty(Marshaller.JAXB_SCHEMA_LOCATION, schemas);
    } catch (Exception e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  public void validateWith(URL url) {
    SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
    Schema schema;
    try {
      schema = factory.newSchema(url);
    } catch (SAXException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
    marshaller.setSchema(schema);
  }

  public void validateWithLocalCacheXSD() {
    // find the local Cache-1.0.xsd
    URL local_cache_xsd = ClassPathLoader.getLatest()
        .getResource("META-INF/schemas/geode.apache.org/schema/cache/cache-1.0.xsd");
    validateWith(local_cache_xsd);
  }

  public String marshall(Object object) {
    StringWriter sw = new StringWriter();
    try {
      sw.write("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>");
      marshaller.marshal(object, sw);
    } catch (Exception e) {
      throw new RuntimeException(e.getMessage(), e);
    }
    return sw.toString();
  }

  public <T> T unMarshall(String xml) {
    try {
      SAXParserFactory factory = SAXParserFactory.newInstance();
      XMLReader reader = factory.newSAXParser().getXMLReader();
      XMLFilterImpl xmlFilter = new XMLCacheTagInterceptor(reader);
      reader.setContentHandler(unmarshaller.getUnmarshallerHandler());
      SAXSource source = new SAXSource(xmlFilter, new InputSource(
              new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))));

      return (T) unmarshaller.unmarshal(source);
    } catch (Exception e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }
}

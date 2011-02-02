/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.osgi.parser;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.osgi.parser.CommonAttributes.ACTIVATION;
import static org.jboss.as.osgi.parser.CommonAttributes.CONFIGURATION;
import static org.jboss.as.osgi.parser.CommonAttributes.CONFIGURATION_PROPERTIES;
import static org.jboss.as.osgi.parser.CommonAttributes.MODULES;
import static org.jboss.as.osgi.parser.CommonAttributes.PID;
import static org.jboss.as.osgi.parser.CommonAttributes.PROPERTIES;
import static org.jboss.as.osgi.parser.CommonAttributes.START;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.NewExtension;
import org.jboss.as.controller.NewExtensionContext;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.as.controller.registry.ModelNodeRegistration;
import org.jboss.as.model.ParseUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class NewOSGiExtension implements NewExtension {

    public static final String SUBSYSTEM_NAME = "osgi";

    private static final OSGiSubsystemParser PARSER = new OSGiSubsystemParser();

    @Override
    public void initialize(NewExtensionContext context) {
        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME);
        final ModelNodeRegistration registration = subsystem.registerSubsystemModel(OSGiSubsystemProviders.SUBSYSTEM);
        registration.registerOperationHandler(ADD, NewOSGiSubsystemAdd.INSTANCE, OSGiSubsystemProviders.SUBSYSTEM_ADD, false);
        subsystem.registerXMLElementWriter(PARSER);
    }

    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(Namespace.CURRENT.getUriString(), PARSER);
    }

    static class OSGiSubsystemParser implements XMLStreamConstants, XMLElementReader<List<ModelNode>>,
            XMLElementWriter<SubsystemMarshallingContext> {

        @Override
        public void readElement(XMLExtendedStreamReader reader, List<ModelNode> operations) throws XMLStreamException {
            final ModelNode addSubsystemOp = new ModelNode();
            addSubsystemOp.get(OP).set(ADD);
            addSubsystemOp.get(OP_ADDR).add(SUBSYSTEM, SUBSYSTEM_NAME);

            // Handle attributes
            parseActivationAttribute(reader, addSubsystemOp);

            // Elements
            while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                switch (Namespace.forUri(reader.getNamespaceURI())) {
                    case OSGI_1_0: {
                        final Element element = Element.forName(reader.getLocalName());
                        switch (element) {
                            case CONFIGURATION: {
                                ModelNode configuration = parseConfigurationElement(reader);
                                addSubsystemOp.get(CONFIGURATION).set(configuration);
                                break;
                            }
                            case PROPERTIES: {
                                ModelNode properties = parsePropertiesElement(reader);
                                addSubsystemOp.get(PROPERTIES).set(properties);
                                break;
                            }
                            case MODULES: {
                                ModelNode modules = parseModulesElement(reader);
                                addSubsystemOp.get(MODULES).set(modules);
                                break;
                            }
                            default:
                                throw ParseUtils.unexpectedElement(reader);
                        }
                        break;
                    }
                    default:
                        throw ParseUtils.unexpectedElement(reader);
                }
            }

            operations.add(addSubsystemOp);
        }

        private void parseActivationAttribute(XMLExtendedStreamReader reader, ModelNode addOperation) throws XMLStreamException {

            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case OSGI_1_0: {
                    // Handle attributes
                    int count = reader.getAttributeCount();
                    for (int i = 0; i < count; i++) {
                        final String attrValue = reader.getAttributeValue(i);
                        if (reader.getAttributeNamespace(i) != null) {
                            throw ParseUtils.unexpectedAttribute(reader, i);
                        } else {
                            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                            switch (attribute) {
                                case ACTIVATION: {
                                    addOperation.get(ACTIVATION).set(attrValue);
                                    break;
                                }
                                default:
                                    throw ParseUtils.unexpectedAttribute(reader, i);
                            }
                        }
                    }
                    break;
                }
                default:
                    throw ParseUtils.unexpectedElement(reader);
            }
        }

        ModelNode parseConfigurationElement(XMLExtendedStreamReader reader) throws XMLStreamException {
            final ModelNode configuration = new ModelNode();

            // Handle attributes
            String pid = null;
            int count = reader.getAttributeCount();
            for (int i = 0; i < count; i++) {
                final String attrValue = reader.getAttributeValue(i);
                if (reader.getAttributeNamespace(i) != null) {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                } else {
                    final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                    switch (attribute) {
                        case PID: {
                            pid = attrValue;
                            break;
                        }
                        default:
                            throw ParseUtils.unexpectedAttribute(reader, i);
                    }
                }
            }

            if (pid == null)
                throw ParseUtils.missingRequired(reader, Collections.singleton(Attribute.PID));

            configuration.get(PID).set(pid);

            // Handle elements
            ModelNode configurationProperties = new ModelNode();
            while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                switch (Namespace.forUri(reader.getNamespaceURI())) {
                    case OSGI_1_0: {
                        final Element element = Element.forName(reader.getLocalName());
                        if (element == Element.PROPERTY) {
                            // Handle attributes
                            String name = null;
                            count = reader.getAttributeCount();
                            for (int i = 0; i < count; i++) {
                                final String attrValue = reader.getAttributeValue(i);
                                if (reader.getAttributeNamespace(i) != null) {
                                    throw ParseUtils.unexpectedAttribute(reader, i);
                                } else {
                                    final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                                    switch (attribute) {
                                        case NAME: {
                                            name = attrValue;
                                            if (configurationProperties.has(name))
                                                throw new XMLStreamException("Property " + name + " already exists",
                                                        reader.getLocation());
                                            break;
                                        }
                                        default:
                                            throw ParseUtils.unexpectedAttribute(reader, i);
                                    }
                                }
                            }
                            if (name == null)
                                throw ParseUtils.missingRequired(reader, Collections.singleton(Attribute.NAME));

                            String value = reader.getElementText().trim();
                            if (value == null || value.length() == 0)
                                throw new XMLStreamException("Value for property " + name + " is null", reader.getLocation());

                            configurationProperties.get(name).set(value);
                            break;
                        } else {
                            throw ParseUtils.unexpectedElement(reader);
                        }
                    }
                    default:
                        throw ParseUtils.unexpectedElement(reader);
                }
            }

            if (configurationProperties.asList().size() > 0)
                configuration.get(CONFIGURATION_PROPERTIES).set(configurationProperties);

            return configuration;
        }

        ModelNode parsePropertiesElement(XMLExtendedStreamReader reader) throws XMLStreamException {

            // Handle attributes
            ParseUtils.requireNoAttributes(reader);

            ModelNode properties = new ModelNode();

            // Handle elements
            while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                switch (Namespace.forUri(reader.getNamespaceURI())) {
                    case OSGI_1_0: {
                        final Element element = Element.forName(reader.getLocalName());
                        if (element == Element.PROPERTY) {
                            // Handle attributes
                            String name = null;
                            String value = null;
                            int count = reader.getAttributeCount();
                            for (int i = 0; i < count; i++) {
                                final String attrValue = reader.getAttributeValue(i);
                                if (reader.getAttributeNamespace(i) != null) {
                                    throw ParseUtils.unexpectedAttribute(reader, i);
                                } else {
                                    final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                                    switch (attribute) {
                                        case NAME: {
                                            name = attrValue;
                                            if (properties.has(name)) {
                                                throw new XMLStreamException("Property " + name + " already exists",
                                                        reader.getLocation());
                                            }
                                            break;
                                        }
                                        default:
                                            throw ParseUtils.unexpectedAttribute(reader, i);
                                    }
                                }
                            }
                            if (name == null) {
                                throw ParseUtils.missingRequired(reader, Collections.singleton(Attribute.NAME));
                            }
                            value = reader.getElementText().trim();
                            if (value == null || value.length() == 0) {
                                throw new XMLStreamException("Value for property " + name + " is null", reader.getLocation());
                            }
                            properties.get(name).set(value);
                            break;
                        } else {
                            throw ParseUtils.unexpectedElement(reader);
                        }
                    }
                    default:
                        throw ParseUtils.unexpectedElement(reader);
                }
            }

            return properties;
        }

        ModelNode parseModulesElement(XMLExtendedStreamReader reader) throws XMLStreamException {

            // Handle attributes
            ParseUtils.requireNoAttributes(reader);

            ModelNode modules = new ModelNode();

            // Handle elements
            while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                switch (Namespace.forUri(reader.getNamespaceURI())) {
                    case OSGI_1_0: {
                        final Element element = Element.forName(reader.getLocalName());
                        if (element == Element.MODULE) {
                            String identifier = null;
                            String start = null;
                            final int count = reader.getAttributeCount();
                            for (int i = 0; i < count; i++) {
                                if (reader.getAttributeNamespace(i) != null) {
                                    throw ParseUtils.unexpectedAttribute(reader, i);
                                }
                                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                                switch (attribute) {
                                    case IDENTIFIER: {
                                        identifier = reader.getAttributeValue(i);
                                        break;
                                    }
                                    case START: {
                                        start = reader.getAttributeValue(i);
                                        break;
                                    }
                                    default:
                                        throw ParseUtils.unexpectedAttribute(reader, i);
                                }
                            }
                            if (identifier == null)
                                throw ParseUtils.missingRequired(reader, Collections.singleton(Attribute.IDENTIFIER));
                            if (modules.has(identifier))
                                throw new XMLStreamException(element.getLocalName() + " already declared", reader.getLocation());

                            ModelNode module = new ModelNode();
                            if (start != null) {
                                module.get(START).set(start);
                            }
                            modules.get(identifier).set(module);

                            ParseUtils.requireNoContent(reader);
                        } else {
                            throw ParseUtils.unexpectedElement(reader);
                        }
                        break;
                    }
                    default:
                        throw ParseUtils.unexpectedElement(reader);
                }
            }

            return modules;
        }

        @Override
        public void writeContent(XMLExtendedStreamWriter writer, SubsystemMarshallingContext context) throws XMLStreamException {
            context.startSubsystemElement(Namespace.CURRENT.getUriString(), false);
            ModelNode node = context.getModelNode();

            if (has(node, ACTIVATION)) {
                writeAttribute(writer, Attribute.ACTIVATION, node.get(ACTIVATION));
            }

            if (has(node, CONFIGURATION)) {
                ModelNode configuration = node.get(CONFIGURATION);
                writer.writeStartElement(Element.CONFIGURATION.getLocalName());
                writeAttribute(writer, Attribute.PID, configuration.require(PID));
                if (has(configuration, CONFIGURATION_PROPERTIES)) {
                    ModelNode configurationProperties = configuration.get(CONFIGURATION_PROPERTIES);
                    Set<String> keys = configurationProperties.keys();
                    for (String current : keys) {
                        String value = configurationProperties.get(current).asString();
                        writer.writeStartElement(Element.PROPERTY.getLocalName());
                        writer.writeAttribute(Attribute.NAME.getLocalName(), current);
                        writer.writeCharacters(value);
                        writer.writeEndElement();
                    }
                }
                writer.writeEndElement();
            }

            if (has(node, PROPERTIES)) {
                ModelNode properties = node.get(PROPERTIES);
                writer.writeStartElement(Element.PROPERTIES.getLocalName());
                Set<String> keys = properties.keys();
                for (String current : keys) {
                    String value = properties.get(current).asString();
                    writer.writeStartElement(Element.PROPERTY.getLocalName());
                    writer.writeAttribute(Attribute.NAME.getLocalName(), current);
                    writer.writeCharacters(value);
                    writer.writeEndElement();
                }
                writer.writeEndElement();
            }

            if (has(node, MODULES)) {
                ModelNode modules = node.get(MODULES);
                writer.writeStartElement(Element.MODULES.getLocalName());
                Set<String> keys = modules.keys();
                for (String current : keys) {
                    ModelNode currentModule = modules.get(current);
                    writer.writeEmptyElement(Element.MODULE.getLocalName());
                    writer.writeAttribute(Attribute.IDENTIFIER.getLocalName(), current);
                    if (has(currentModule, START)) {
                        writeAttribute(writer, Attribute.START, currentModule.require(START));
                    }
                }
                writer.writeEndElement();
            }
            writer.writeEndElement();
        }

        private boolean has(ModelNode node, String name) {
            return node.has(name) && node.get(name).isDefined();
        }

        private void writeAttribute(final XMLExtendedStreamWriter writer, final Attribute attr, final ModelNode value)
                throws XMLStreamException {
            writer.writeAttribute(attr.getLocalName(), value.asString());
        }

    }

}
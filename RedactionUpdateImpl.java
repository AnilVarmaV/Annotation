package com.ssl.impl;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.filenet.api.collection.ContentElementList;
import com.filenet.api.constants.PropertyNames;
import com.filenet.api.constants.RefreshMode;
import com.filenet.api.core.Annotation;
import com.filenet.api.core.Connection;
import com.filenet.api.core.ContentElement;
import com.filenet.api.core.ContentTransfer;
import com.filenet.api.core.Document;
import com.filenet.api.core.Factory;
import com.filenet.api.core.ObjectStore;
import com.filenet.api.property.FilterElement;
import com.filenet.api.property.PropertyFilter;
import com.filenet.api.util.Id;
import com.ssl.logger.ErrorLogger;

public class RedactionUpdateImpl {

	private static	ErrorLogger log = new ErrorLogger("RedactionUpdateImpl");
	public void addAnnoToRedactCopyDocument(ObjectStore os,Document redactDoc, String annotationsXml) throws Exception
	{
		log.info("RedactionUpdateImpl createRedactCopyDocument:: Start");

		RedactionUpdateImpl readImpl=null;
		try {
			readImpl=new RedactionUpdateImpl();
			PropertyFilter pf = new PropertyFilter();       
			pf.addIncludeProperty(new FilterElement(null, null, null, PropertyNames.CONTENT_ELEMENTS, null));

			ContentTransfer ctObject = Factory.ContentTransfer.createInstance();
			ContentElementList annContentList = Factory.ContentTransfer.createList();
			Map< String,String> annoMap  = setAnnotationsMap(os,redactDoc, annotationsXml);
			for (Map.Entry<String, String> entry : annoMap.entrySet())
			{
				Id annoId= new Id(entry.getKey());
				Annotation annObject = Factory.Annotation.fetchInstance(os, annoId, pf);
				InputStream stream = new ByteArrayInputStream(entry.getValue().getBytes());
				ctObject.setCaptureSource(stream);
				annContentList.add(ctObject);
				annObject.set_ContentElements(annContentList);
				annObject.save(RefreshMode.REFRESH);
				log.info("RedactionUpdateImpl createRedactCopyDocument:: Annotation Created with ID::"+annoId);
			}
			

			log.info("RedactionUpdateImpl createRedactCopyDocument:: End");
		}
		catch (Exception e)
		{
			e.printStackTrace() ;
			throw new Exception(e);
		}


	}

	public  String createAnnotationObject(ObjectStore os,Document doc)
	{

		log.info("RedactionUpdateImpl createAnnotationObject():: start");
		ContentElementList docContentList = doc.get_ContentElements();
		Integer elementSequenceNumber = ((ContentElement) docContentList.get(0)).get_ElementSequenceNumber();
		Annotation annObject = Factory.Annotation.createInstance(os, "Annotation");
		annObject.set_AnnotatedObject(doc);
		annObject.set_AnnotatedContentElement(elementSequenceNumber.intValue() );
		annObject.set_DescriptiveText("Custom Annotation applied to the document");
		annObject.save(RefreshMode.REFRESH);
		String annoId=annObject.get_Id().toString();
		log.info("RedactionUpdateImpl createAnnotationObject():: End");
		return annoId;

	}



	public String getStringFromFile(String fileName)
	{

		log.info("RedactionUpdateImpl getStringFromFile():: start");
		BufferedReader reader;
		String content=null;
		try {
			reader = new BufferedReader(new FileReader(fileName));

			StringBuilder stringBuilder = new StringBuilder();
			String line = null;
			String ls = System.getProperty("line.separator");
			while ((line = reader.readLine()) != null) {
				stringBuilder.append(line);
				stringBuilder.append(ls);
			}
			// delete the last new line separator
			stringBuilder.deleteCharAt(stringBuilder.length() - 1);
			reader.close();

			content = stringBuilder.toString();
			log.info("RedactionUpdateImpl getStringFromFile():: end");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return content;

	}

	public  Map< String,String>  setAnnotationsMap(ObjectStore os,Document document, String annotationsXml) throws Exception{

		log.info("RedactionUpdateImpl setAnnotationsMap():: Start");
		Map< String,String> annoMap = null;
		RedactionUpdateImpl readImpl=null;
		try {
			readImpl=new RedactionUpdateImpl();
			annoMap =  new HashMap< String,String>();
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			InputStream stream = new ByteArrayInputStream(annotationsXml.getBytes());
			org.w3c.dom.Document doc = dBuilder.parse(stream);
			doc.getDocumentElement().normalize();

			NodeList nList = doc.getElementsByTagName("FnAnno");
			for (int temp = 0; temp < nList.getLength(); temp++) {
				Node nNode = nList.item(temp);
				Element eElement = (Element) nNode;
				Element propElement=(Element) eElement.getElementsByTagName("PropDesc").item(0);
				String annoId = readImpl.createAnnotationObject(os,document);
				propElement.setAttribute("F_ANNOTATEDID", annoId);
				Transformer transformer = TransformerFactory.newInstance().newTransformer();
				transformer.setOutputProperty(OutputKeys.INDENT, "yes");
				StreamResult result = new StreamResult(new StringWriter());
				DOMSource source = new DOMSource(eElement);
				transformer.transform(source, result);
				String xmlString = result.getWriter().toString();
				annoMap.put(annoId, xmlString);
			}
			log.info("RedactionUpdateImpl setAnnotationsMap():: end");

		} catch (Exception e) {
			e.printStackTrace();
			throw new Exception(e);
		}
		return annoMap;
	}

}

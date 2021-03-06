/*
MIT License

Copyright (c) 2012-2017 PASTA Contributors (see CONTRIBUTORS.txt)

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/

package pasta.archive.convert;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import pasta.archive.legacy.Tuple;
import pasta.domain.template.Assessment;
import pasta.domain.template.HandMarkData;
import pasta.domain.template.HandMarking;
import pasta.domain.template.UnitTest;
import pasta.domain.template.WeightedField;
import pasta.domain.template.WeightedHandMarking;
import pasta.domain.template.WeightedUnitTest;
import pasta.repository.AssessmentDAO;
import pasta.repository.HandMarkingDAO;
import pasta.repository.UnitTestDAO;
import pasta.util.ProjectProperties;

public class AssessmentConverter {
	private static Logger logger = Logger.getLogger(AssessmentConverter.class);
	
	@Autowired
	private AssessmentDAO assessmentDAO;
	@Autowired
	private HandMarkingDAO handMarkingDAO;
	@Autowired
	private UnitTestDAO unitTestDAO;
	
	private Map<String, pasta.archive.legacy.UnitTest> allUnitTests;
	private Map<String, pasta.archive.legacy.HandMarking> allHandMarking;
	private Map<String, pasta.archive.legacy.Assessment> allAssessments;
	
	private Map<String, UnitTest> convertedUnitTests;
	private Map<String, HandMarking> convertedHandMarking;
	private Map<String, Assessment> convertedAssessments;
	
	private List<String> output;
	Boolean done = null;
	
	public void convertLegacyContent() {
		if(done != null) {
			return;
		}
		output = Collections.synchronizedList(new LinkedList<String>());
		done = false;
		doLoad();
		doConvert();
		doSave();
		done = true;
	}
	
	public List<String> getOutputSinceLastCall() {
		List<String> outSinceLastCall = new LinkedList<String>(output);
		output.clear();
		return outSinceLastCall;
	}
	
	public boolean isStarted() {
		return done != null;
	}
	
	public boolean isDone() {
		return done != null && done;
	}
	
	public boolean hasOutput() {
		return !output.isEmpty();
	}
	
	private void doLoad() {
		// load up unit tests
		allUnitTests = new TreeMap<>();
		loadUnitTests();

		// load up hand marking
		allHandMarking = new TreeMap<>();
		loadHandMarking();

		// load up all assessments
		allAssessments = new TreeMap<>();
		loadAssessments();
	}
	
	private void doConvert() {
		convertedUnitTests = new TreeMap<>();
		for(Map.Entry<String, pasta.archive.legacy.UnitTest> oldTestEntry : allUnitTests.entrySet()) {
			UnitTest converted = convertUnitTest(oldTestEntry.getValue());
			if(converted != null) {
				convertedUnitTests.put(oldTestEntry.getKey(), converted);
				output.add("Converted legacy unit test " + converted.getName());
			}
		}
		
		convertedHandMarking = new TreeMap<>();
		for(Map.Entry<String, pasta.archive.legacy.HandMarking> oldHMEntry : allHandMarking.entrySet()) {
			HandMarking converted = convertHandMarking(oldHMEntry.getValue());
			if(converted != null) {
				convertedHandMarking.put(oldHMEntry.getKey(), converted);
				output.add("Converted legacy hand marking " + converted.getName());
			}
		}
		
		convertedAssessments = new TreeMap<>();
		for(Map.Entry<String, pasta.archive.legacy.Assessment> oldAssEntry : allAssessments.entrySet()) {
			Assessment converted = convertAssessment(oldAssEntry.getValue());
			if(converted != null) {
				convertedAssessments.put(oldAssEntry.getKey(), converted);
				output.add("Converted legacy assessment " + converted.getName());
			}
		}
	}
	
	private void doSave() {
		for(Map.Entry<String, UnitTest> utEntry : convertedUnitTests.entrySet()) {
			try {
				unitTestDAO.save(utEntry.getValue());
				output.add("Saved unit test " + utEntry.getKey());
			} catch(Exception e) {
				String error = "Error saving unit test " + utEntry.getKey();
				output.add(error);
				logger.error(error, e);
			}
		}
		for(Map.Entry<String, HandMarking> hmEntry : convertedHandMarking.entrySet()) {
			try {
				handMarkingDAO.saveOrUpdate(hmEntry.getValue());
				output.add("Saved hand marking " + hmEntry.getKey());
			} catch(Exception e) {
				String error = "Error saving hand marking " + hmEntry.getKey();
				output.add(error);
				logger.error(error, e);
			}
		}
		for(Map.Entry<String, Assessment> assEntry : convertedAssessments.entrySet()) {
			try {
				assessmentDAO.saveOrUpdate(assEntry.getValue());
				output.add("Saved assessment " + assEntry.getKey());
			} catch(Exception e) {
				String error = "Error saving assessment " + assEntry.getKey();
				output.add(error);
				logger.error(error, e);
			}
		}
	}
	
	/**
	 * Load all unit tests.
	 * <p>
	 * Calls {@link #getUnitTestFromDisk(String)} multiple times.
	 */
	private void loadUnitTests() {
		// get unit test location
		String allTestLocation = ProjectProperties.getInstance().getProjectLocation() + "legacy/template/unitTest";
		if(!new File(allTestLocation).exists()) {
			allTestLocation = ProjectProperties.getInstance().getProjectLocation() + "legacy/unitTest";
		}
		if(!new File(allTestLocation).exists()) {
			output.add("No unit tests found under " + ProjectProperties.getInstance().getProjectLocation() + "legacy/template/unitTest" +
					" or " + ProjectProperties.getInstance().getProjectLocation() + "legacy/unitTest");
			return;
		}
		String[] allUnitTestNames = (new File(allTestLocation)).list();
		if (allUnitTestNames != null && allUnitTestNames.length > 0) {
			// load properties
			for (String name : allUnitTestNames) {
				pasta.archive.legacy.UnitTest test = getUnitTestFromDisk(allTestLocation + "/" + name);
				if (test != null) {
					allUnitTests.put(name, test);
					output.add("Loaded legacy unit test " + name);
				}
			}
		}
	}

	/**
	 * Load all assessments
	 * <p>
	 * Calls {@link #getAssessmentFromDisk(String)} multiple times.
	 */
	private void loadAssessments() {
		// get assessment location
		String allTestLocation = ProjectProperties.getInstance().getProjectLocation() + "legacy/template/assessment";
		if(!new File(allTestLocation).exists()) {
			allTestLocation = ProjectProperties.getInstance().getProjectLocation() + "legacy/assessment";
		}
		if(!new File(allTestLocation).exists()) {
			output.add("No assessments found under " + ProjectProperties.getInstance().getProjectLocation() + "legacy/template/assessment" +
					" or " + ProjectProperties.getInstance().getProjectLocation() + "legacy/assessment");
			return;
		}
		String[] allAssessmentNames = (new File(allTestLocation)).list();
		if (allAssessmentNames != null && allAssessmentNames.length > 0) {
			// load properties
			for (String name : allAssessmentNames) {
				pasta.archive.legacy.Assessment assessment = getAssessmentFromDisk(allTestLocation + "/" + name);
				if (assessment != null) {
					allAssessments.put(name, assessment);
					output.add("Loaded legacy assessment " + name);
				}
			}
		}
	}

	/**
	 * Load all handmarkings templates
	 * <p>
	 * Calls {@link #getHandMarkingFromDisk(String)} multiple times.
	 */
	private void loadHandMarking() {
		// get hand marking location
		String allTestLocation = ProjectProperties.getInstance().getProjectLocation() + "legacy/template/handMarking";
		if(!new File(allTestLocation).exists()) {
			allTestLocation = ProjectProperties.getInstance().getProjectLocation() + "legacy/handMarking";
		}
		if(!new File(allTestLocation).exists()) {
			output.add("No hand marking found under " + ProjectProperties.getInstance().getProjectLocation() + "legacy/template/handMarking" +
					" or " + ProjectProperties.getInstance().getProjectLocation() + "legacy/handMarking");
			return;
		}
		String[] allHandMarkingNames = (new File(allTestLocation)).list();
		if (allHandMarkingNames != null && allHandMarkingNames.length > 0) {
			// load properties
			for (String name : allHandMarkingNames) {
				pasta.archive.legacy.HandMarking test = getHandMarkingFromDisk(allTestLocation + "/" + name);
				if (test != null) {
					allHandMarking.put(test.getShortName(), test);
					output.add("Loaded legacy hand marking " + name);
				}
			}
		}
	}
	
	
	private pasta.archive.legacy.UnitTest getUnitTestFromDisk(String location) {
		try {
			File fXmlFile = new File(location + "/unitTestProperties.xml");
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder dBuilder;
			dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(fXmlFile);
			doc.getDocumentElement().normalize();
			String name = doc.getElementsByTagName("name").item(0)
					.getChildNodes().item(0).getNodeValue();
			boolean tested = Boolean.parseBoolean(doc
					.getElementsByTagName("tested").item(0).getChildNodes()
					.item(0).getNodeValue());
			return new pasta.archive.legacy.UnitTest(name, tested);
		} catch (Exception e) {
			String error = "Could not rebuild legacy unit test from " + location;
			output.add(error);
			logger.error(error, e);
			return null;
		}
	}
	
	/**
	 * Method to get a handmarking from a location
	 * <p>
	 * Loads the handMarkingProperties.xml from file into the cache. 
	 * Also loads the multiple .html files which are the descriptions
	 * in each box of the hand marking template.
	 * 
	 * @param location
	 *            - the location of the handmarking
	 * @return null - there is no handmarking at that location to be retrieved
	 * @return test - the handmarking at that location.
	 */
	private pasta.archive.legacy.HandMarking getHandMarkingFromDisk(String location) {
		try {

			File fXmlFile = new File(location + "/handMarkingProperties.xml");
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder dBuilder;
			dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(fXmlFile);
			doc.getDocumentElement().normalize();

			pasta.archive.legacy.HandMarking markingTemplate = new pasta.archive.legacy.HandMarking();

			// load name
			markingTemplate.setName(doc.getElementsByTagName("name").item(0)
					.getChildNodes().item(0).getNodeValue());

			// load column list
			NodeList columnList = doc.getElementsByTagName("column");
			List<Tuple> columnHeaderList = new ArrayList<Tuple>();
			if (columnList != null && columnList.getLength() > 0) {
				for (int i = 0; i < columnList.getLength(); i++) {
					Node columnNode = columnList.item(i);
					if (columnNode.getNodeType() == Node.ELEMENT_NODE) {
						Element columnElement = (Element) columnNode;

						Tuple tuple = new Tuple();
						tuple.setName(columnElement.getAttribute("name"));
						tuple.setWeight(Double.parseDouble(columnElement
								.getAttribute("weight")));

						columnHeaderList.add(tuple);
					}
				}
			}
			markingTemplate.setColumnHeader(columnHeaderList);

			// load row list
			NodeList rowList = doc.getElementsByTagName("row");
			List<Tuple> rowHeaderList = new ArrayList<Tuple>();
			if (rowList != null && rowList.getLength() > 0) {
				for (int i = 0; i < rowList.getLength(); i++) {
					Node rowNode = rowList.item(i);
					if (rowNode.getNodeType() == Node.ELEMENT_NODE) {
						Element rowElement = (Element) rowNode;

						Tuple tuple = new Tuple();
						tuple.setName(rowElement.getAttribute("name"));
						tuple.setWeight(Double.parseDouble(rowElement
								.getAttribute("weight")));

						rowHeaderList.add(tuple);
					}
				}
			}
			markingTemplate.setRowHeader(rowHeaderList);

			// load data
			Map<String, Map<String, String>> descriptionMap = new TreeMap<String, Map<String, String>>();
			for (Tuple column : markingTemplate.getColumnHeader()) {
				Map<String, String> currDescriptionMap = new TreeMap<String, String>();
				for (Tuple row : markingTemplate.getRowHeader()) {
					try {
						Scanner in = new Scanner(new File(location + "/"
								+ column.getName().replace(" ", "") + "-"
								+ row.getName().replace(" ", "") + ".txt"));
						String description = "";
						while (in.hasNextLine()) {
							description += in.nextLine()
									+ System.getProperty("line.separator");
						}
						currDescriptionMap.put(row.getName(), description);
						in.close();
					} catch (Exception e) {
						// do nothing
					}
				}
				descriptionMap.put(column.getName(), currDescriptionMap);
			}

			markingTemplate.setData(descriptionMap);

			return markingTemplate;
		} catch (Exception e) {
			String error = "Could not rebuild legacy hand marking from " + location;
			output.add(error);
			logger.error(error, e);
			return null;
		}
	}
	
	
	/**
	 * Method to get an assessment from a location
	 * <p>
	 * Loads the assessmentProperties.xml from file into the cache. 
	 * 
	 * @param location
	 *            - the location of the assessment
	 * @return null - there is no assessment at that location to be retrieved
	 * @return assessment - the assessment at that location.
	 */
	private pasta.archive.legacy.Assessment getAssessmentFromDisk(String location) {
		try {
			File fXmlFile = new File(location + "/assessmentProperties.xml");
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder dBuilder;
			dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(fXmlFile);
			doc.getDocumentElement().normalize();

			pasta.archive.legacy.Assessment currentAssessment = new pasta.archive.legacy.Assessment();

			currentAssessment.setName(doc.getElementsByTagName("name").item(0)
					.getChildNodes().item(0).getNodeValue());
			currentAssessment.setMarks(Double.parseDouble(doc
					.getElementsByTagName("marks").item(0).getChildNodes()
					.item(0).getNodeValue()));
			try {
				currentAssessment.setReleasedClasses(doc
						.getElementsByTagName("releasedClasses").item(0)
						.getChildNodes().item(0).getNodeValue());
			} catch (Exception e) {
				// not released
			}

			try {
				currentAssessment.setCategory(doc
						.getElementsByTagName("category").item(0)
						.getChildNodes().item(0).getNodeValue());
			} catch (Exception e) {
				// no category
			}
			
			try {
				currentAssessment.setCountUncompilable(Boolean.parseBoolean(doc
						.getElementsByTagName("countUncompilable").item(0)
						.getChildNodes().item(0).getNodeValue()));
			} catch (Exception e) {
				// no countUncompilable tag - defaults to true
			}

			try {
				currentAssessment.setSpecialRelease(doc
						.getElementsByTagName("specialRelease").item(0)
						.getChildNodes().item(0).getNodeValue());
			} catch (Exception e) {
				// not special released
			}
			currentAssessment.setNumSubmissionsAllowed(Integer.parseInt(doc
					.getElementsByTagName("submissionsAllowed").item(0)
					.getChildNodes().item(0).getNodeValue()));

			SimpleDateFormat sdf = new SimpleDateFormat("HH:mm dd/MM/yyyy");
			currentAssessment.setDueDate(sdf.parse(doc
					.getElementsByTagName("dueDate").item(0).getChildNodes()
					.item(0).getNodeValue()));

			// load description from file
			String description = "";
			try {
				Scanner in = new Scanner(new File(location
						+ "/description.html"));
				while (in.hasNextLine()) {
					description += in.nextLine()
							+ System.getProperty("line.separator");
				}
				in.close();
			} catch (Exception e) {
				description = "<pre>Error loading description"
						+ System.getProperty("line.separator") + e + "</pre>";
			}
			currentAssessment.setDescription(description);

			// add unit tests
			NodeList unitTestList = doc.getElementsByTagName("unitTest");
			if (unitTestList != null && unitTestList.getLength() > 0) {
				for (int i = 0; i < unitTestList.getLength(); i++) {
					Node unitTestNode = unitTestList.item(i);
					if (unitTestNode.getNodeType() == Node.ELEMENT_NODE) {
						Element unitTestElement = (Element) unitTestNode;

						pasta.archive.legacy.WeightedUnitTest weightedTest = new pasta.archive.legacy.WeightedUnitTest();
						if(!allUnitTests.containsKey(unitTestElement.getAttribute("name"))) {
							output.add("Skipping unit test " + unitTestElement.getAttribute("name") + " for assessment " + currentAssessment.getName() + " (not found)");
							continue;
						}
						weightedTest.setTest(allUnitTests.get(unitTestElement
								.getAttribute("name")));
						weightedTest.setWeight(Double
								.parseDouble(unitTestElement
										.getAttribute("weight")));
						if (unitTestElement.getAttribute("secret") != null
								&& Boolean.parseBoolean(unitTestElement
										.getAttribute("secret"))) {
							currentAssessment.addSecretUnitTest(weightedTest);
						} else {
							currentAssessment.addUnitTest(weightedTest);
						}
					}
				}
			}

			// add hand marking
			NodeList handMarkingList = doc.getElementsByTagName("handMarks");
			if (handMarkingList != null && handMarkingList.getLength() > 0) {
				for (int i = 0; i < handMarkingList.getLength(); i++) {
					Node handMarkingNode = handMarkingList.item(i);
					if (handMarkingNode.getNodeType() == Node.ELEMENT_NODE) {
						Element handMarkingElement = (Element) handMarkingNode;

						pasta.archive.legacy.WeightedHandMarking weightedHandMarking = new pasta.archive.legacy.WeightedHandMarking();
						if(!allHandMarking.containsKey(handMarkingElement.getAttribute("name"))) {
							output.add("Skipping handMarking " + handMarkingElement.getAttribute("name") + " for assessment " + currentAssessment.getName() + " (not found)");
							continue;
						}
						weightedHandMarking.setHandMarking(allHandMarking
								.get(handMarkingElement.getAttribute("name")));
						weightedHandMarking.setWeight(Double
								.parseDouble(handMarkingElement
										.getAttribute("weight")));
						currentAssessment.addHandMarking(weightedHandMarking);
					}
				}
			}

			return currentAssessment;
		} catch (Exception e) {
			String error = "Could not rebuild legacy assessment from " + location;
			output.add(error);
			logger.error(error, e);
			return null;
		}
	}
	
	
	private UnitTest convertUnitTest(pasta.archive.legacy.UnitTest old) {
		try {
			UnitTest newTest = new UnitTest();
			newTest.setName(old.getName());
			newTest.setTested(false);
			return newTest;
		} catch(Exception e) {
			String error = "Error converting legacy unit test " + old.getName();
			output.add(error);
			logger.error(error, e);
			return null;
		}
	}
	
	private HandMarking convertHandMarking(pasta.archive.legacy.HandMarking old) {
		try {
			HandMarking newHM = new HandMarking();
			newHM.setName(old.getName());
			
			Map<String, WeightedField> rows = new HashMap<>();
			Map<String, WeightedField> cols = new HashMap<>();
			for(Tuple row : old.getRowHeader()) {
				WeightedField newRow = new WeightedField(row.getName(), row.getWeight());
				newHM.addRow(newRow);
				rows.put(row.getName(), newRow);
			}
			for(Tuple column : old.getColumnHeader()) {
				WeightedField newCol = new WeightedField(column.getName(), column.getWeight());
				newHM.addColumn(newCol);
				cols.put(column.getName(), newCol);
			}
			for(String colName : old.getData().keySet()) {
				WeightedField col = cols.get(colName);
				for(String rowName : old.getData().get(colName).keySet()) {
					String data = old.getData().get(colName).get(rowName);
					WeightedField row = rows.get(rowName);
					HandMarkData newData = new HandMarkData(col, row, data);
					newHM.addData(newData);
				}
			}
			return newHM;
		} catch(Exception e) {
			String error = "Error converting legacy hand marking " + old.getName();
			output.add(error);
			logger.error(error, e);
			return null;
		}
	}

	private Assessment convertAssessment(pasta.archive.legacy.Assessment old) {
		try {
			Assessment newAss = new Assessment();
			newAss.setCategory(old.getCategory());
			newAss.setCountUncompilable(old.isCountUncompilable());
			newAss.setDescription(old.getDescription());
			newAss.setDueDate(old.getDueDate());
			newAss.setMarks(old.getMarks());
			newAss.setName(old.getName());
			newAss.setNumSubmissionsAllowed(old.getNumSubmissionsAllowed());
			
			for(pasta.archive.legacy.WeightedUnitTest oldTest : old.getUnitTests()) {
				WeightedUnitTest newTest = new WeightedUnitTest();
				newTest.setSecret(false);
				newTest.setTest(convertedUnitTests.get(oldTest.getTest().getShortName()));
				newTest.setWeight(oldTest.getWeight());
				newAss.addUnitTest(newTest);
			}
			for(pasta.archive.legacy.WeightedUnitTest oldTest : old.getSecretUnitTests()) {
				WeightedUnitTest newTest = new WeightedUnitTest();
				newTest.setSecret(true);
				newTest.setTest(convertedUnitTests.get(oldTest.getTest().getShortName()));
				newTest.setWeight(oldTest.getWeight());
				newAss.addUnitTest(newTest);
			}
			
			for(pasta.archive.legacy.WeightedHandMarking oldHM : old.getHandMarking()) {
				WeightedHandMarking newHM = new WeightedHandMarking();
				newHM.setHandMarking(convertedHandMarking.get(oldHM.getHandMarking().getShortName()));
				newHM.setWeight(oldHM.getWeight());
				newAss.addHandMarking(newHM);
			}
			
			return newAss;
		} catch(Exception e) {
			String error = "Error converting legacy assessment " + old.getName();
			output.add(error);
			logger.error(error, e);
			return null;
		}
	}

}

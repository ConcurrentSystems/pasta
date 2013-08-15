package pasta.repository;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import pasta.domain.PASTATime;
import pasta.domain.ReleaseForm;
import pasta.domain.template.Arena;
import pasta.domain.template.Assessment;
import pasta.domain.template.Competition;
import pasta.domain.template.HandMarking;
import pasta.domain.template.Tuple;
import pasta.domain.template.UnitTest;
import pasta.domain.template.WeightedCompetition;
import pasta.domain.template.WeightedHandMarking;
import pasta.domain.template.WeightedUnitTest;
import pasta.domain.upload.NewCompetition;
import pasta.domain.upload.NewHandMarking;
import pasta.util.ProjectProperties;

public class AssessmentDAO {

	// assessmentTemplates are cached
	Map<String, Assessment> allAssessments;
	Map<String, List<Assessment>> allAssessmentsByCategory;
	Map<String, UnitTest> allUnitTests;
	Map<String, HandMarking> allHandMarking;
	Map<String, Competition> allCompetitions;

	protected final Log logger = LogFactory.getLog(getClass());

	public AssessmentDAO() {
		// load up all cached objects

		// load up unit tests
		allUnitTests = new TreeMap<String, UnitTest>();
		loadUnitTests();

		// load up hand marking
		allHandMarking = new TreeMap<String, HandMarking>();
		loadHandMarking();

		// load up competitions
		allCompetitions = new TreeMap<String, Competition>();
		loadCompetitions();

		// load up all assessments
		allAssessmentsByCategory = new TreeMap<String, List<Assessment>>();
		allAssessments = new TreeMap<String, Assessment>();
		loadAssessments();

	}

	public Map<String, List<Assessment>> getAllAssessmentsByCategory() {
		return allAssessmentsByCategory;
	}

	public Map<String, UnitTest> getAllUnitTests() {
		return allUnitTests;
	}

	public Assessment getAssessment(String name) {
		return allAssessments.get(name);
	}

	public HandMarking getHandMarking(String name) {
		return allHandMarking.get(name);
	}

	public Competition getCompetition(String name) {
		return allCompetitions.get(name);
	}

	public Collection<HandMarking> getHandMarkingList() {
		return allHandMarking.values();
	}

	public Collection<Assessment> getAssessmentList() {
		return allAssessments.values();
	}

	public Collection<Competition> getCompetitionList() {
		return allCompetitions.values();
	}

	public void addUnitTest(UnitTest newUnitTest) {
		allUnitTests.put(newUnitTest.getShortName(), newUnitTest);
	}

	public void addAssessment(Assessment newAssessment) {
		// if already exists, update
		if (allAssessments.containsKey(newAssessment.getShortName())) {
			Assessment curAss = allAssessments
					.get(newAssessment.getShortName());
			// simple
			curAss.setDescription(newAssessment.getDescription());
			curAss.setDueDate(newAssessment.getDueDate());
			curAss.setMarks(newAssessment.getMarks());
			curAss.setNumSubmissionsAllowed(newAssessment
					.getNumSubmissionsAllowed());
			curAss.setReleasedClasses(newAssessment.getReleasedClasses());
			curAss.setSpecialRelease(newAssessment.getSpecialRelease());
			String oldCategory = "";
			String newCategory = "";
			if (curAss.getCategory() != null) {
				oldCategory = curAss.getCategory();
			}
			if (newAssessment.getCategory() != null) {
				newCategory = newAssessment.getCategory();
			}
			curAss.setCategory(newAssessment.getCategory());

			allAssessmentsByCategory.get(oldCategory).remove(curAss);
			if (!allAssessmentsByCategory.containsKey(newCategory)) {
				allAssessmentsByCategory.put(newCategory,
						new LinkedList<Assessment>());
			}
			allAssessmentsByCategory.get(newCategory).add(curAss);

			// tests
			curAss.setSecretUnitTests(newAssessment.getSecretUnitTests());
			curAss.setUnitTests(newAssessment.getUnitTests());
			curAss.setHandMarking(newAssessment.getHandMarking());
			curAss.setCompetitions(newAssessment.getCompetitions());
		} else {
			allAssessments.put(newAssessment.getShortName(), newAssessment);
			String category = "";
			if (newAssessment.getCategory() != null) {
				category = newAssessment.getCategory();
			}

			if (!allAssessmentsByCategory.containsKey(category)) {
				allAssessmentsByCategory.put(category,
						new LinkedList<Assessment>());
			}
			allAssessmentsByCategory.get(category).add(newAssessment);
		}
	}

	public void releaseAssessment(String assessmentName, ReleaseForm released) {
		allAssessments.get(assessmentName).setReleasedClasses(
				released.getList());
		if (released.getSpecialRelease() != null
				&& !released.getSpecialRelease().isEmpty()) {
			allAssessments.get(assessmentName).setSpecialRelease(
					released.getSpecialRelease());
		}
		try {
			// save to file
			PrintWriter out = new PrintWriter(new File(ProjectProperties
					.getInstance().getProjectLocation()
					+ "/template/assessment/"
					+ assessmentName
					+ "/assessmentProperties.xml"));
			out.print(allAssessments.get(assessmentName).toString());
			out.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void removeUnitTest(String unitTestName) {
		allUnitTests.remove(unitTestName);
		try {
			FileUtils.deleteDirectory(new File(ProjectProperties.getInstance()
					.getProjectLocation()
					+ "/template/unitTest/"
					+ unitTestName));
		} catch (Exception e) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			logger.error("Could not delete the folder for unit test "
					+ unitTestName + "\r\n" + sw.toString());
		}
	}

	public void removeAssessment(String assessmentName) {
		allAssessmentsByCategory.get(
				allAssessments.get(assessmentName).getCategory()).remove(
				allAssessments.get(assessmentName));
		allAssessments.remove(assessmentName);
		try {
			FileUtils.deleteDirectory(new File(ProjectProperties.getInstance()
					.getProjectLocation()
					+ "/template/assessment/"
					+ assessmentName));
		} catch (Exception e) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			logger.error("Could not delete the folder for assessment "
					+ assessmentName + "\r\n" + sw.toString());
		}
	}

	public void removeCompetition(String competitionName) {
		allCompetitions.remove(competitionName);
		try {
			FileUtils.deleteDirectory(new File(ProjectProperties.getInstance()
					.getProjectLocation()
					+ "/template/competition/"
					+ competitionName));
		} catch (Exception e) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			logger.error("Could not delete the folder for competition "
					+ competitionName + "\r\n" + sw.toString());
		}
	}

	/**
	 * Load all unit tests.
	 */
	private void loadUnitTests() {
		// get unit test location
		String allTestLocation = ProjectProperties.getInstance()
				.getProjectLocation() + "/template/unitTest";
		String[] allUnitTestNames = (new File(allTestLocation)).list();
		if (allUnitTestNames != null && allUnitTestNames.length > 0) {
			Arrays.sort(allUnitTestNames);

			// load properties
			for (String name : allUnitTestNames) {
				UnitTest test = getUnitTestFromDisk(allTestLocation + "/"
						+ name);
				if (test != null) {
					allUnitTests.put(name, test);
				}
			}
		}

	}

	/**
	 * Load all competitions
	 */
	private void loadCompetitions() {
		// get unit test location
		String allCompetitionLocation = ProjectProperties.getInstance()
				.getProjectLocation() + "/template/competition";
		String[] allCompetitionNames = (new File(allCompetitionLocation))
				.list();
		if (allCompetitionNames != null && allCompetitionNames.length > 0) {
			Arrays.sort(allCompetitionNames);

			// load properties
			for (String name : allCompetitionNames) {
				Competition comp = getCompetitionFromDisk(allCompetitionLocation
						+ "/" + name);
				if (comp != null) {
					allCompetitions.put(name, comp);
				}
			}
		}

	}

	/**
	 * Load all assessments.
	 */
	private void loadAssessments() {
		// get unit test location
		String allTestLocation = ProjectProperties.getInstance()
				.getProjectLocation() + "/template/assessment";
		String[] allAssessmentNames = (new File(allTestLocation)).list();
		if (allAssessmentNames != null && allAssessmentNames.length > 0) {
			Arrays.sort(allAssessmentNames);

			// load properties
			for (String name : allAssessmentNames) {
				Assessment test = getAssessmentFromDisk(allTestLocation + "/"
						+ name);
				if (test != null) {
					allAssessments.put(name, test);
					String category = test.getCategory();
					if (category == null) {
						category = "";
					}
					if (!allAssessmentsByCategory.containsKey(category)) {
						allAssessmentsByCategory.put(category,
								new LinkedList<Assessment>());
					}
					allAssessmentsByCategory.get(category).add(test);
				}
			}
		}

	}

	/**
	 * Load all handmarkings.
	 */
	private void loadHandMarking() {
		// get hand marking location
		String allTestLocation = ProjectProperties.getInstance()
				.getProjectLocation() + "/template/handMarking";
		String[] allHandMarkingNames = (new File(allTestLocation)).list();
		if (allHandMarkingNames != null && allHandMarkingNames.length > 0) {
			Arrays.sort(allHandMarkingNames);

			// load properties
			for (String name : allHandMarkingNames) {
				HandMarking test = getHandMarkingFromDisk(allTestLocation + "/"
						+ name);
				if (test != null) {
					allHandMarking.put(test.getShortName(), test);
				}
			}
		}
	}

	/**
	 * Method to get a unit test from a location
	 * 
	 * @param location
	 *            - the location of the unit test
	 * @return null - there is no unit test at that location to be retrieved
	 * @return test - the unit test at that location.
	 */
	private UnitTest getUnitTestFromDisk(String location) {
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

			return new UnitTest(name, tested);
		} catch (Exception e) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			logger.error("Could not read unit test " + location
					+ System.getProperty("line.separator") + sw.toString());
			return null;
		}
	}

	/**
	 * Method to get a competition from a location
	 * 
	 * @param location
	 *            - the location of the competition
	 * @return null - there is no competition at that location to be retrieved
	 * @return comp - the competition at that location.
	 */
	private Competition getCompetitionFromDisk(String location) {
		try {

			File fXmlFile = new File(location + "/competitionProperties.xml");
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder dBuilder;
			dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(fXmlFile);
			doc.getDocumentElement().normalize();

			Competition comp = new Competition();

			// name
			comp.setName(doc.getElementsByTagName("name").item(0)
					.getChildNodes().item(0).getNodeValue());

			// tested
			comp.setTested(Boolean.parseBoolean(doc
					.getElementsByTagName("tested").item(0).getChildNodes()
					.item(0).getNodeValue()));

			// can students create an arena
			comp.setStudentCreatableArena(Boolean.parseBoolean(doc
					.getElementsByTagName("studentCreatableArena").item(0)
					.getChildNodes().item(0).getNodeValue()));

			// can students create repeatable arenas
			comp.setStudentCreatableRepeatableArena(Boolean.parseBoolean(doc
					.getElementsByTagName("studentCreatableRepeatableArena")
					.item(0).getChildNodes().item(0).getNodeValue()));

			// can tutors create repeatableArenas
			comp.setTutorCreatableRepeatableArena(Boolean.parseBoolean(doc
					.getElementsByTagName("tutorCreatableRepeatableArena")
					.item(0).getChildNodes().item(0).getNodeValue()));

			// first start date - only for calculated comps
			if (doc.getElementsByTagName("firstStartDate") != null
					&& doc.getElementsByTagName("firstStartDate").getLength() != 0) {
				comp.setFirstStartDate(ProjectProperties.parseDate(doc
						.getElementsByTagName("firstStartDate").item(0)
						.getChildNodes().item(0).getNodeValue()));
			}

			// frequency - only for calculated comps
			if (doc.getElementsByTagName("frequency") != null
					&& doc.getElementsByTagName("frequency").getLength() != 0) {
				comp.setFrequency(new PASTATime(doc
						.getElementsByTagName("frequency").item(0)
						.getChildNodes().item(0).getNodeValue()));
			}

			// arenas
			if (doc.getElementsByTagName("arenas") != null
					&& doc.getElementsByTagName("arenas").getLength() != 0) {
				Collection<Arena> arenas = new LinkedList<Arena>();
				NodeList arenaList = doc.getElementsByTagName("arena");
				if (arenaList != null) {
					for (int i = 0; i < arenaList.getLength(); i++) {
						Node arenaNode = arenaList.item(i);
						if (arenaNode.getNodeType() == Node.ELEMENT_NODE) {
							Element arenaElement = (Element) arenaNode;

							Arena arena = new Arena();
							arena.setName(arenaElement.getAttribute("name"));
							try {
								arena.setFirstStartDate(ProjectProperties.parseDate(arenaElement
										.getAttribute("firstStartDate")));
							} catch (Exception e) {
								// couldn't parse date, it must be decades in
								// the past.
								arena.setFirstStartDate(new Date(0));
							}
							if (arenaElement.getAttribute("password").length() > 0) {
								arena.setPassword(arenaElement
										.getAttribute("password"));
							}

							if (arenaElement.getAttribute("repeats").length() > 0) {
								arena.setFrequency(new PASTATime(arenaElement
										.getAttribute("repeats")));
							}

							arenas.add(arena);
						}
					}
				}

				comp.setArenas(arenas);
			}
			return comp;
		} catch (Exception e) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			logger.error("Could not read competition " + location
					+ System.getProperty("line.separator") + sw.toString());
			return null;
		}
	}

	/**
	 * Method to get an assessment from a location
	 * 
	 * @param location
	 *            - the location of the assessment
	 * @return null - there is no assessment at that location to be retrieved
	 * @return test - the assessment at that location.
	 */
	private Assessment getAssessmentFromDisk(String location) {
		try {

			File fXmlFile = new File(location + "/assessmentProperties.xml");
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder dBuilder;
			dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(fXmlFile);
			doc.getDocumentElement().normalize();

			Assessment currentAssessment = new Assessment();

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

						WeightedUnitTest weightedTest = new WeightedUnitTest();
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

						WeightedHandMarking weightedHandMarking = new WeightedHandMarking();
						weightedHandMarking.setHandMarking(allHandMarking
								.get(handMarkingElement.getAttribute("name")));
						weightedHandMarking.setWeight(Double
								.parseDouble(handMarkingElement
										.getAttribute("weight")));
						currentAssessment.addHandMarking(weightedHandMarking);
					}
				}
			}

			// add competitions
			NodeList competitionList = doc.getElementsByTagName("competition");
			if (competitionList != null && competitionList.getLength() > 0) {
				for (int i = 0; i < competitionList.getLength(); i++) {
					Node competitionNode = competitionList.item(i);
					if (competitionNode.getNodeType() == Node.ELEMENT_NODE) {
						Element competitionElement = (Element) competitionNode;

						WeightedCompetition weightedComp = new WeightedCompetition();
						weightedComp.setTest(allCompetitions
								.get(competitionElement.getAttribute("name")));
						weightedComp.setWeight(Double
								.parseDouble(competitionElement
										.getAttribute("weight")));
						weightedComp.getTest().addAssessment(currentAssessment);
						currentAssessment.addCompetition(weightedComp);
					}
				}
			}

			return currentAssessment;
		} catch (Exception e) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			logger.error("Could not read assessment " + location
					+ System.getProperty("line.separator") + sw.toString());
			return null;
		}
	}

	/**
	 * Method to get a handmarking from a location
	 * 
	 * @param location
	 *            - the location of the handmarking
	 * @return null - there is no handmarking at that location to be retrieved
	 * @return test - the handmarking at that location.
	 */
	private HandMarking getHandMarkingFromDisk(String location) {
		try {

			File fXmlFile = new File(location + "/handMarkingProperties.xml");
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder dBuilder;
			dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(fXmlFile);
			doc.getDocumentElement().normalize();

			HandMarking markingTemplate = new HandMarking();

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
			HashMap<String, HashMap<String, String>> descriptionMap = new HashMap<String, HashMap<String, String>>();
			for (Tuple column : markingTemplate.getColumnHeader()) {
				HashMap<String, String> currDescriptionMap = new HashMap<String, String>();
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
					} catch (Exception e) {
						// do nothing
					}
				}
				descriptionMap.put(column.getName(), currDescriptionMap);
			}

			markingTemplate.setData(descriptionMap);

			return markingTemplate;
		} catch (Exception e) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			logger.error("Could not read hand marking " + location
					+ System.getProperty("line.separator") + sw.toString());
			return null;
		}
	}

	public void updateHandMarking(HandMarking newHandMarking) {
		if (allHandMarking.containsKey(newHandMarking.getShortName())) {
			HandMarking currMarking = allHandMarking.get(newHandMarking
					.getShortName());

			currMarking.setColumnHeader(newHandMarking.getColumnHeader());
			currMarking.setData(newHandMarking.getData());
			currMarking.setRowHeader(newHandMarking.getRowHeader());
		} else {
			allHandMarking.put(newHandMarking.getShortName(), newHandMarking);
		}
		// save to drive

		String location = ProjectProperties.getInstance().getProjectLocation()
				+ "/template/handMarking/" + newHandMarking.getShortName();

		try {
			FileUtils.deleteDirectory(new File(location));
		} catch (IOException e) {
			// Don't care if it doesn't exist
		}

		// make the folder
		(new File(location)).mkdirs();

		try {
			PrintWriter handMarkingProperties = new PrintWriter(new File(
					location + "/handMarkingProperties.xml"));
			handMarkingProperties.println("<handMarkingProperties>");
			// name
			handMarkingProperties.println("\t<name>" + newHandMarking.getName()
					+ "</name>");
			// columns
			if (!newHandMarking.getColumnHeader().isEmpty()) {
				handMarkingProperties.println("\t<columns>");
				for (Tuple column : newHandMarking.getColumnHeader()) {
					handMarkingProperties.println("\t\t<column name=\""
							+ column.getName() + "\" weight=\""
							+ column.getWeight() + "\"/>");
				}
				handMarkingProperties.println("\t</columns>");
			}

			// rows
			if (!newHandMarking.getRowHeader().isEmpty()) {
				handMarkingProperties.println("\t<rows>");
				for (Tuple row : newHandMarking.getRowHeader()) {
					handMarkingProperties.println("\t\t<row name=\""
							+ row.getName() + "\" weight=\"" + row.getWeight()
							+ "\"/>");
				}
				handMarkingProperties.println("\t</rows>");
			}
			handMarkingProperties.println("</handMarkingProperties>");
			handMarkingProperties.close();

			for (Entry<String, HashMap<String, String>> entry1 : newHandMarking
					.getData().entrySet()) {
				for (Entry<String, String> entry2 : entry1.getValue()
						.entrySet()) {
					PrintWriter dataOut = new PrintWriter(new File(location
							+ "/" + entry1.getKey().replace(" ", "") + "-"
							+ entry2.getKey().replace(" ", "") + ".txt"));

					dataOut.println(entry2.getValue());
					dataOut.close();
				}
			}
		} catch (FileNotFoundException e) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			logger.error("Could not update hand marking " + location
					+ System.getProperty("line.separator") + sw.toString());
		}
	}

	public void newHandMarking(NewHandMarking newHandMarking) {

		HandMarking newMarking = new HandMarking();
		newMarking.setName(newHandMarking.getName());

		ArrayList<Tuple> columns = new ArrayList<Tuple>();
		columns.add(new Tuple("Poor", 0));
		columns.add(new Tuple("Acceptable", 0.5));
		columns.add(new Tuple("Excellent", 1));
		newMarking.setColumnHeader(columns);

		ArrayList<Tuple> rows = new ArrayList<Tuple>();
		rows.add(new Tuple("Formatting", 0.2));
		rows.add(new Tuple("Code Reuse", 0.4));
		rows.add(new Tuple("Variable naming", 0.4));
		newMarking.setRowHeader(rows);

		HashMap<String, HashMap<String, String>> data = new HashMap<String, HashMap<String, String>>();
		for (Tuple column : columns) {
			HashMap<String, String> currData = new HashMap<String, String>();
			for (Tuple row : rows) {
				currData.put(row.getName(), "");
			}
			data.put(column.getName(), currData);
		}
		newMarking.setData(data);

		updateHandMarking(newMarking);
	}

	public void removeHandMarking(String handMarkingName) {
		// remove from set
		allHandMarking.remove(handMarkingName);

		// remove from assessments
		for (Entry<String, Assessment> ass : allAssessments.entrySet()) {
			List<WeightedHandMarking> marking = ass.getValue().getHandMarking();
			for (WeightedHandMarking weighted : marking) {
				if (weighted.getHandMarkingName().equals(handMarkingName)) {
					ass.getValue().getHandMarking().remove(weighted);
					break;
				}
			}
		}
	}

	public void addCompetition(Competition comp) {
		if (allCompetitions.containsKey(comp.getShortName())) {
			// update
			allCompetitions.get(comp.getShortName())
					.setArenas(comp.getArenas());
			allCompetitions.get(comp.getShortName()).setStudentCreatableArena(
					comp.isStudentCreatableArena());
			allCompetitions.get(comp.getShortName())
					.setStudentCreatableRepeatableArena(
							comp.isStudentCreatableRepeatableArena());
			allCompetitions.get(comp.getShortName()).setTested(comp.isTested());
			allCompetitions.get(comp.getShortName())
					.setTutorCreatableRepeatableArena(
							comp.isTutorCreatableRepeatableArena());
			allCompetitions.get(comp.getShortName()).setFirstStartDateStr(
					comp.getFirstStartDateStr());
			allCompetitions.get(comp.getShortName()).setFrequency(
					comp.getFrequency());
		} else {
			// add
			allCompetitions.put(comp.getShortName(), comp);
		}
	}

}
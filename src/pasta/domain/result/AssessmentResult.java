package pasta.domain.result;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.commons.collections.FactoryUtils;
import org.apache.commons.collections.list.LazyList;

import pasta.domain.template.Assessment;
import pasta.domain.template.WeightedCompetition;
import pasta.domain.template.WeightedHandMarking;
import pasta.domain.template.WeightedUnitTest;

public class AssessmentResult {
	private ArrayList<UnitTestResult> unitTests;
	private List<HandMarkingResult> handMarkingResults = LazyList.decorate(new ArrayList<HandMarkingResult>(),
			FactoryUtils.instantiateFactory(HandMarkingResult.class));
	private Assessment assessment;
	private int submissionsMade;
	private Date submissionDate;
	private Date dueDate;
	private String comments;
	
	public Collection<UnitTestResult> getUnitTests() {
		return unitTests;
	}

	public void setUnitTests(ArrayList<UnitTestResult> unitTests) {
		this.unitTests = unitTests;
		Collections.sort(this.unitTests);
	}

	public Assessment getAssessment() {
		return assessment;
	}

	public void setAssessment(Assessment assessment) {
		this.assessment = assessment;
	}
	
	public int getSubmissionsMade() {
		return submissionsMade;
	}

	public void setSubmissionsMade(int submissionsMade) {
		this.submissionsMade = submissionsMade;
	}

	public Date getSubmissionDate() {
		return submissionDate;
	}
	
	public String getFormattedSubmissionDate(){
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss");
		return sdf.format(submissionDate);
	}

	public void setSubmissionDate(Date submissionDate) {
		this.submissionDate = submissionDate;
	}
	
	public List<HandMarkingResult> getHandMarkingResults() {
		return handMarkingResults;
	}

	public void setHandMarkingResults(List<HandMarkingResult> handMarkingResults) {
		this.handMarkingResults.clear();
		this.handMarkingResults.addAll(handMarkingResults);
	}

	public void addUnitTest(UnitTestResult test){
		unitTests.add(test);
	}
	
	public void removeUnitTest(UnitTestResult test){
		unitTests.remove(test);
	}
	
	public boolean isCompileError() {
		for(UnitTestResult result : unitTests){
			if(result.getCompileErrors()!= null && !result.getCompileErrors().isEmpty()){
				return true;
			}
		}
		return false;
	}
	
	public String getCompilationError() {
		String compilationError = "";
		for(UnitTestResult result : unitTests){
			if(result.getCompileErrors()!= null && !result.getCompileErrors().isEmpty()){
				compilationError += result.getCompileErrors() 
						+ System.getProperty("line.separator")
						+ System.getProperty("line.separator")
						+ System.getProperty("line.separator");
			}
		}
		return compilationError;
	}

	public double getMarks(){
		return getPercentage() * assessment.getMarks();
	}
	
	public double getAutoMarks(){
		return getAutoPercentage() * assessment.getMarks();
	}
	
	public double getPercentage(){
		double marks = 0;
		double maxWeight = getAssessmentHandMarkingWeight() 
				+ getAssessmentUnitTestsWeight()
				+ getAssessmentCompetitionWeight();
		// unit tests
		// regular
		for(UnitTestResult result : unitTests){
			try{
				marks += result.getPercentage()*assessment.getWeighting(result.getTest());
			}
			catch(Exception e){
				// ignore anything that throws exceptions
			}
		}
		
		// hand marking
		for(HandMarkingResult result : handMarkingResults){
			try{
				marks += result.getPercentage()*assessment.getWeighting(result.getMarkingTemplate());
			}
			catch(Exception e){
				// ignore anything that throws exceptions (probably a partially marked submission)
			}
		}
				
		if(maxWeight == 0){
			return 0;
		}
		return (marks / maxWeight);
	}
	
	public double getAutoPercentage(){
		double marks = 0;
		double maxWeight = getAssessmentHandMarkingWeight() 
				+ getAssessmentUnitTestsWeight()
				+ getAssessmentCompetitionWeight();
		// unit tests
		// regular
		for(UnitTestResult result : unitTests){
			try{
				marks += result.getPercentage()*assessment.getWeighting(result.getTest());
			}
			catch(Exception e){
				// ignore anything that throws exceptions
			}
		}
		
		if(maxWeight == 0){
			return 0;
		}
		return (marks / maxWeight);
	}
	
	private double getAssessmentHandMarkingWeight(){
		double weight = 0;
		if(assessment.getHandMarking() != null){
			for(WeightedHandMarking marking: assessment.getHandMarking()){
				weight += marking.getWeight();
			}
		}
		return weight;
	}
	
	private double getAssessmentUnitTestsWeight(){
		double weight = 0;
		if(assessment.getAllUnitTests() != null){
			for(WeightedUnitTest marking: assessment.getAllUnitTests()){
				weight += marking.getWeight();
			}
		}
		return weight;
	}
	
	private double getAssessmentCompetitionWeight(){
		double weight = 0;
		if(assessment.getCompetitions() != null){
			for(WeightedCompetition marking: assessment.getCompetitions()){
				weight += marking.getWeight();
			}
		}
		return weight;
	}

	public String getComments() {
		return comments;
	}

	public void setComments(String comments) {
		this.comments = comments;
	}
	
	public boolean isFinishedHandMarking(){
		if(handMarkingResults.size() < assessment.getHandMarking().size()){
			return false;
		}
		if(handMarkingResults != null){
			for(HandMarkingResult res : handMarkingResults){
				if(res == null || !res.isFinishedMarking()){
					return false;
				}
			}
		}
		return true;
	}

}

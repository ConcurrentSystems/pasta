/*
Copyright (c) 2014, Alex Radu
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this
   list of conditions and the following disclaimer. 
2. Redistributions in binary form must reproduce the above copyright notice,
   this list of conditions and the following disclaimer in the documentation
   and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

The views and conclusions contained in the software and documentation are those
of the authors and should not be interpreted as representing official policies, 
either expressed or implied, of the PASTA Project.
 */

package pasta.domain.upload;

import java.util.ArrayList;
import java.util.List;

import org.springframework.web.multipart.commons.CommonsMultipartFile;

import pasta.domain.template.BlackBoxTest;
import pasta.domain.template.BlackBoxTestCase;
import pasta.domain.template.UnitTest;

/**
 * Form object to update a unit test assessment module.
 * 
 * @author Joshua Stretton
 * @version 1.0
 * @since 2015-04-02
 *
 */
public class UpdateUnitTestForm {
	
	private long id;
	private String name;
	
	private CommonsMultipartFile file;
	
	private String mainClassName;
	private String submissionCodeRoot;
	
	private List<BlackBoxTestCaseForm> testCases;
	
	public UpdateUnitTestForm(UnitTest base) {
		this.id = base.getId();
		this.name = base.getName();
		this.mainClassName = base.getMainClassName();
		this.submissionCodeRoot = base.getSubmissionCodeRoot();
		if(base instanceof BlackBoxTest) {
			this.testCases = createTestCaseForms(((BlackBoxTest) base).getTestCases());
		}
		
		this.file = null;
	}
	private List<BlackBoxTestCaseForm> createTestCaseForms(List<BlackBoxTestCase> testCases) {
		List<BlackBoxTestCaseForm> forms = new ArrayList<BlackBoxTestCaseForm>();
		for(BlackBoxTestCase testCase : testCases) {
			forms.add(new BlackBoxTestCaseForm(testCase));
		}
		return forms;
	}
	
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public CommonsMultipartFile getFile() {
		return file;
	}
	public void setFile(CommonsMultipartFile file) {
		this.file = file;
	}
	
	//JUnitTests
	public String getMainClassName() {
		return mainClassName;
	}
	public void setMainClassName(String mainClassName) {
		this.mainClassName = mainClassName;
	}
	public String getSubmissionCodeRoot() {
		return submissionCodeRoot;
	}
	public void setSubmissionCodeRoot(String submissionCodeRoot) {
		this.submissionCodeRoot = submissionCodeRoot;
	}
	
	//BlackBoxTests
	public List<BlackBoxTestCaseForm> getTestCases() {
		return testCases;
	}
	public void setTestCases(List<BlackBoxTestCaseForm> testCases) {
		if(this.testCases == null || testCases == null) {
			this.testCases = testCases;
		} else {
			this.testCases.clear();
			this.testCases.addAll(testCases);
		}
	}
	public List<BlackBoxTestCase> getPlainTestCases() {
		List<BlackBoxTestCase> testCases = new ArrayList<BlackBoxTestCase>();
		for(BlackBoxTestCaseForm form : this.testCases) {
			if(!form.isDeleteMe()) {
				testCases.add(form.asPlainTestCase());
			}
		}
		return testCases;
	}
}

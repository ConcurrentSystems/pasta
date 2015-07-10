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

package pasta.web.controller;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.TreeMap;
import java.util.zip.ZipOutputStream;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import pasta.domain.FileTreeNode;
import pasta.domain.template.BlackBoxTest;
import pasta.domain.template.UnitTest;
import pasta.domain.upload.NewUnitTestForm;
import pasta.domain.upload.Submission;
import pasta.domain.upload.TestUnitTestForm;
import pasta.domain.upload.UpdateUnitTestForm;
import pasta.domain.upload.validate.TestUnitTestFormValidator;
import pasta.domain.upload.validate.UpdateUnitTestFormValidator;
import pasta.domain.user.PASTAUser;
import pasta.service.UnitTestManager;
import pasta.util.PASTAUtil;
import pasta.util.ProjectProperties;

/**
 * Controller class for Unit Test functions. 
 * <p>
 * Handles mappings of $PASTAUrl$/unitTest/...
 * <p>
 * Only teaching staff can access this url.
 * 
 * @author Alex Radu
 * @version 2.0
 * @since 2013-08-15
 *
 */
@Controller
@RequestMapping("unitTest/")
public class UnitTestController {

	/**
	 * Initializes the codeStyle tag mapping of file endings to 
	 * javascript tag requirements for syntax highlighting.
	 */
	public UnitTestController() {
		codeStyle = new TreeMap<String, String>();
		codeStyle.put("c", "ccode");
		codeStyle.put("cpp", "cppcode");
		codeStyle.put("h", "cppcode");
		codeStyle.put("cs", "csharpcode");
		codeStyle.put("css", "csscode");
		codeStyle.put("html", "htmlcode");
		codeStyle.put("java", "javacode");
		codeStyle.put("js", "javascriptcode");
		codeStyle.put("pl", "perlcode");
		codeStyle.put("pm", "perlcode");
		codeStyle.put("php", "phpcode");
		codeStyle.put("py", "pythoncode");
		codeStyle.put("rb", "rubycode");
		codeStyle.put("sql", "sqlcode");
		codeStyle.put("xml", "xmlcode");

	}

	protected final Log logger = LogFactory.getLog(getClass());

	@Autowired
	private UnitTestManager unitTestManager;
	
	@Autowired
	private UpdateUnitTestFormValidator updateValidator;
	@Autowired
	private TestUnitTestFormValidator testValidator;
	
	private Map<String, String> codeStyle;

	// ///////////////////////////////////////////////////////////////////////////
	// Models //
	// ///////////////////////////////////////////////////////////////////////////
	
	@ModelAttribute("updateUnitTest")
	public UpdateUnitTestForm loadUpdateForm(@PathVariable("testId") long testId) {
		return new UpdateUnitTestForm(unitTestManager.getUnitTest(testId));
	}
	
	@ModelAttribute("testUnitTest")
	public TestUnitTestForm loadTestForm(@PathVariable("testId") long testId) {
		return new TestUnitTestForm(unitTestManager.getUnitTest(testId));
	}
	
	@ModelAttribute("unitTest")
	public UnitTest loadUnitTest(@PathVariable("testId") long testId) {
		return unitTestManager.getUnitTest(testId);
	}

	// ///////////////////////////////////////////////////////////////////////////
	// Helper Methods //
	// ///////////////////////////////////////////////////////////////////////////

	/**
	 * Get the currently logged in user.
	 * 
	 * @return the currently used user, null if nobody is logged in or user isn't registered.
	 */
	public PASTAUser getUser() {
		PASTAUser user = (PASTAUser) RequestContextHolder
				.currentRequestAttributes().getAttribute("user",
						RequestAttributes.SCOPE_SESSION);
		return user;
	}

	// ///////////////////////////////////////////////////////////////////////////
	// UNIT TEST //
	// ///////////////////////////////////////////////////////////////////////////

	/**
	 * $PASTAUrl$/unitTest/{testId}/
	 * <p>
	 * View the details of a unit test.
	 * 
	 * If the user has not authenticated: redirect to login.
	 * 
	 * If the user is not a tutor: redirect to home.
	 * 
	 * ATTRIBUTES:
	 * <table>
	 * 	<tr><td>unikey</td><td>The {@link pasta.domain.user.PASTAUser} for the logged in user</td></tr>
	 * 	<tr><td>unitTest</td><td>The {@link pasta.domain.template.UnitTest} for this test</td></tr>
	 * 	<tr><td>latestResult</td><td>The {@link pasta.domain.result.UnitTestResult} for the execution of the test run of the unit testing code.</td></tr>
	 * 	<tr><td>node</td><td>The root {@link pasta.domain.FileTreeNode} for the root of the unit test code.</td></tr>
	 * </table>
	 * 
	 * JSP: <ul><li>assessment/view/unitTest</li></ul>
	 * 
	 * @param testId the id of the test.
	 * @param model the model being used
	 * @return "redirect:/login/" or "redirect:/home/" or "assessment/view/unitTest"
	 */
	@RequestMapping(value = "{testId}/")
	public String viewUnitTest(@PathVariable("testId") long testId,
			@ModelAttribute("unitTest") UnitTest test,
			Model model) {
		PASTAUser user = getUser();
		if (user == null) {
			return "redirect:/login/";
		}
		if (!user.isTutor()) {
			return "redirect:/home/";
		}

		model.addAttribute("unikey", user);
		model.addAttribute("latestResult", test.getTestResult());
		
		FileTreeNode node = PASTAUtil.generateFileTree(test.getFileLocation() + "/code");
		model.addAttribute("node", node);
		
		if(test.hasCode()) {
			if(!(test instanceof BlackBoxTest)) {
				Map<String, String> candidateFiles = new HashMap<String, String>();
				Stack<FileTreeNode> toExpand = new Stack<FileTreeNode>();
				toExpand.push(node);
				int dirStart = node.getLocation().length();
				
				while(!toExpand.isEmpty()) {
					FileTreeNode expandNode = toExpand.pop();
					String location = expandNode.getLocation().substring(dirStart);
					if(location.endsWith(".java")) {
						candidateFiles.put(location.substring(0, location.length()-5), location);
					}
					if(!expandNode.isLeaf()) {
						for(FileTreeNode child : expandNode.getChildren()) {
							toExpand.push(child);
						}
					}
				}
				model.addAttribute("candidateMainFiles", candidateFiles);
			}
		}
		
		return "assessment/view/unitTest";
	}
	
	/**
	 * $PASTAUrl$/unitTest/{testId}/download/
	 * <p>
	 * Download the unit test code on the machine.
	 * 
	 * If the user has not authenticated or is not a tutor: do nothing
	 * 
	 * Otherwise create a zip file with the name: $testName$.zip
	 * 
	 * @param testId the id of the unit test
	 * @param model the model being used
	 * @param response the response being used to serve the zip
	 */
	@RequestMapping(value = "{testId}/download/")
	public void downloadUnitTest(@PathVariable("testId") long testId,
			Model model,HttpServletResponse response) {
		PASTAUser user = getUser();
		if (user == null) {
			return;
		}
		if (!user.isTutor()) {
			return;
		}
		
		String testName = unitTestManager.getUnitTest(testId).getFileAppropriateName();
		response.setContentType("application/zip");
		response.setHeader("Content-Disposition", "attachment;filename=\""
				+ testName + ".zip\"");
		ByteArrayOutputStream outStream = new ByteArrayOutputStream();
		ZipOutputStream zip = new ZipOutputStream(outStream);
		try {
			
			PASTAUtil.zip(zip, new File(ProjectProperties.getInstance()
					.getUnitTestsLocation()
					+ testId
					+ "/code/"), ProjectProperties.getInstance()
					.getUnitTestsLocation()
					+ testId
					+ "/code/");
			zip.closeEntry();
			zip.close();
			IOUtils.copy(new ByteArrayInputStream(outStream.toByteArray()),
					response.getOutputStream());
			response.flushBuffer();

		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	
	/**
	 * $PASTAUrl$/unitTest/{testId}/ - POST
	 * <p>
	 * Upload some code to test the unit test on the production machine or
	 * to update the unit tests on the system.
	 * 
	 * If the user has not authenticated: redirect to login.
	 * 
	 * If the user is not a tutor: redirect to home.
	 * 
	 * If the user is an instructor:
	 * <ul>
	 * 	<li><b>updating code</b> - done using {@link pasta.service.UnitTestManager#updateUnitTestCode(NewUnitTestForm)}</li>
	 * 	<li><b>testing test code</b> - done using {@link pasta.service.UnitTestManager#testUnitTest(Submission, String)}</li> 
	 * </ul>
	 * 
	 * @param testId the id of the test
	 * @param form used for updating the unit test code
	 * @param subForm used for testing the unit test code
	 * @param result binding results used for feedback.
	 * @param model the model being used.
	 * @return "redirect:/login/" or "redirect:/home/" or "redirect:/mirror/"
	 */
	@RequestMapping(value = "{testId}/", method = RequestMethod.POST)
	public String updateUnitTest(@PathVariable("testId") long testId,
			@Valid @ModelAttribute(value = "updateUnitTest") UpdateUnitTestForm updateForm, BindingResult result,
			@ModelAttribute(value = "unitTest") UnitTest test,
			RedirectAttributes attr, Model model) {
		PASTAUser user = getUser();
		if (user == null) {
			return "redirect:/login/";
		}
		if (!user.isTutor()) {
			return "redirect:/home/";
		}
		
		updateValidator.validate(updateForm, result);
		if(result.hasErrors()) {
			attr.addFlashAttribute("updateUnitTest", updateForm);
			attr.addFlashAttribute("org.springframework.validation.BindingResult.updateUnitTest", result);
			return "redirect:.";
		}
		
		if(updateForm != null && getUser().isInstructor()) {
			unitTestManager.updateUnitTest(test, updateForm);
			
			if(updateForm.getFile() != null && !updateForm.getFile().isEmpty()) {
				unitTestManager.updateUnitTestCode(test, updateForm);
			}
		}

		return "redirect:/mirror/";
	}
	
	/**
	 * $PASTAUrl$/unitTest/{testId}/test/ - POST
	 * <p>
	 * Upload some code to test the unit test on the production machine or
	 * to update the unit tests on the system.
	 * 
	 * If the user has not authenticated: redirect to login.
	 * 
	 * If the user is not a tutor: redirect to home.
	 * 
	 * If the user is an instructor:
	 * <ul>
	 * 	<li><b>testing test code</b> - done using {@link pasta.service.UnitTestManager#testUnitTest(Submission, String)}</li> 
	 * </ul>
	 * 
	 * @param testId the id of the test
	 * @param testForm used for testing the unit test code
	 * @param test the test itself
	 * @param result binding results used for feedback.
	 * @param model the model being used.
	 * @return "redirect:/login/" or "redirect:/home/" or "redirect:/mirror/"
	 */
	@RequestMapping(value = "{testId}/test/", method = RequestMethod.POST)
	public String testUnitTest(@PathVariable("testId") long testId,
			@Valid @ModelAttribute(value = "testUnitTest") TestUnitTestForm testForm, BindingResult result,
			@ModelAttribute(value = "unitTest") UnitTest test,
			RedirectAttributes attr, Model model) {
		PASTAUser user = getUser();
		if (user == null) {
			return "redirect:/login/";
		}
		if (!user.isTutor()) {
			return "redirect:/home/";
		}
		
		testValidator.validate(testForm, result);
		if(result.hasErrors()) {
			attr.addFlashAttribute("testUnitTest", testForm);
			attr.addFlashAttribute("org.springframework.validation.BindingResult.testUnitTest", result);
			return "redirect:../.";
		}
		
		if(testForm != null && testForm.getFile() != null && getUser().isInstructor()) {
			unitTestManager.testUnitTest(test, testForm);
		}
		
		return "redirect:/mirror/";
	}

	/**
	 * $PASTAUrl$/unitTest/delete/{testId}/
	 * <p>
	 * Delete a unit test from the system.
	 * 
	 * If the user has not authenticated: redirect to login.
	 * 
	 * If the user is not a tutor: redirect to home.
	 * 
	 * If the user is an instructor: delete the unit test using
	 * {@link pasta.service.UnitTestManager#removeUnitTest(long)}
	 * then redirect to $PASTAUrl$/unitTest/
	 * 
	 * @param testId the id of the unit test.
	 * @param model the model being used.
	 * @return "redirect:/login/" or "redirect:/home/" or "redirect:../../"
	 */
	@RequestMapping(value = "delete/{testId}/")
	public String deleteUnitTest(@PathVariable("testId") long testId,
			Model model) {
		PASTAUser user = getUser();
		if (user == null) {
			return "redirect:/login/";
		}
		if (!user.isTutor()) {
			return "redirect:/home/";
		}
		if (getUser().isInstructor()) {
			unitTestManager.removeUnitTest(testId);
		}
		return "redirect:../../";
	}

	/**
	 * $PASTAUrl$/unitTest/tested/{testId}/
	 * <p>
	 * Mark a unit test as tested.
	 * 
	 * If the user has not authenticated: redirect to login.
	 * 
	 * If the user is not a tutor: redirect to home.
	 * 
	 * If the user is an instructor: mark the unit test as tested.
	 * 
	 * @param testId the id of the test
	 * @param model the model being used.
	 * @return "redirect:/login/" or "redirect:/home/" or "redirect:../../$testId$/"
	 */
	@RequestMapping(value = "tested/{testId}/")
	public String testedUnitTest(@PathVariable("testId") long testId,
			Model model) {
		PASTAUser user = getUser();
		if (user == null) {
			return "redirect:/login/";
		}
		if (!user.isTutor()) {
			return "redirect:/home/";
		}
		if (getUser().isInstructor()) {
			UnitTest test = unitTestManager.getUnitTest(testId);
			test.setTested(true);
			unitTestManager.updateUnitTest(test);
		}
		return "redirect:../../" + testId + "/";
	}
}
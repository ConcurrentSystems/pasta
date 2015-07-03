<!-- 
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
-->

<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<h1 style="margin-bottom:0.5em;">${assessment.name}</h1>

<c:if test="${not assessment.completelyTested}" >
	<div class="ui-state-error ui-corner-all" style="font-size: 1.5em;">
		<span class="ui-icon ui-icon-alert" style="float: left; margin-right: .3em;"></span><b>WARNING: This assessment contains untested unit tests</b>
	</div>
</c:if>

<button style="margin-top:1em;float:left;" class="button" onclick="window.location.href=window.location.href+'run/'">Re-Run Assessment</button>

<form:form commandName="updateAssessmentForm" enctype="multipart/form-data" method="POST">
	
	<%-- <form:input type="hidden" path="id" value="${assessment.id}"/> --%>
	<input type="submit" value="Save Assessment" id="submit" style="margin-top:1em;"/>
	
	<table>
		<tr><td>Category:</td><td><form:input type="text" path="category"/></td></tr>
		<tr><td>Assessment Marks:</td><td><form:input type="text" path="marks"/></td></tr>
		<tr><td>Assessment DueDate:</td><td><form:input type="text" path="strDate" /></td></tr>
		<tr><td>Maximum Number of allowed submissions:</td><td><form:input type="text" path="numSubmissionsAllowed"/></td></tr>
		<tr><td>Count submissions that have failed to compile:</td><td><form:checkbox path="countUncompilable"/></td></tr>
		<tr><td>Solution Name:</td><td><form:input path="solutionName"/><span class='help'>The name of the main solution source code file. <strong>Only required if you use Black Box unit tests.</strong> If students are to submit <code>MyProgram.java</code> and <code>MyProgram.c</code>, then solution name should be "MyProgram"</span></td></tr>
	</table>
	
	<h2>Release Rule</h2>
	<div>
		<div class="vertical-block">
			<p><strong>Current release rule:</strong>
			<p>${assessment.releaseDescription}
		</div>
		<div class="vertical-block">
			<c:set var="buttonText" value="Set" />
			<c:if test="${assessment.released}"><c:set var="buttonText" value="Modify" /></c:if>
			<a href="../release/${assessment.id}/"><c:out value="${buttonText}" /> Release Rule</a>
		</div>		
	</div>
	
	
	<h2>Description</h2>
	<div id='descriptionHTML'>
		${assessment.description}
	</div>
	<button type="button" id="modifyDescription">Modify Description</button>
	<div style="display:none">
		<form:textarea path="description" cols="110" rows="10" /><br/>
	</div>
	
	
	<table style="margin-bottom:2em;width:90%">
		<tr>
			<td valign="top" >
				<div style="float:left; width:100%;">
					<h2> Unit Tests </h2>
					<table class='dragRows' style="width:100%;">
						<tr class="sortableDisabled"><th>Name</th><th>Weighting</th><th>Tested</th></tr>
						<tbody id="unitTest" class="sortable newUnitTests">
							<c:forEach var="unitTest" varStatus="unitTestIndex" items="${nonSecretUnitTests}">
								<tr>
									<td>
										<form:input type="hidden" path="newUnitTests[${unitTestIndex.index}].id" value="${unitTest.id}"/>
										<form:input type="hidden" path="newUnitTests[${unitTestIndex.index}].test.id" value="${unitTest.test.id}"/>
										<a href="../../unitTest/${unitTest.test.id}/">${unitTest.test.name}</a>
									</td>
									<td><form:input size="5" type="text" path="newUnitTests[${unitTestIndex.index}].weight" value="${unitTest.weight}"/></td>
									<td class="pastaTF pastaTF${unitTest.test.tested}">${unitTest.test.tested}</td>
								</tr>
							</c:forEach>
							<tr id="buffer" class="dragBuffer sortableDisabled"></tr>
						</tbody>
					</table>
				
					<h2> Secret Unit Tests </h2>
					<table class='dragRows' style="width:100%;">
						<tr class="sortableDisabled"><th>Name</th><th>Weighting</th><th>Tested</th></tr>
						<tbody id="unitTest" class="sortable newSecretUnitTests">
							<c:forEach var="unitTest" varStatus="unitTestIndex" items="${secretUnitTests}">
								<tr>
									<td>
										<form:input type="hidden" path="newSecretUnitTests[${unitTestIndex.index}].id" value="${unitTest.id}"/>
										<form:input type="hidden" path="newSecretUnitTests[${unitTestIndex.index}].test.id" value="${unitTest.test.id}"/>
										<a href="../../unitTest/${unitTest.test.id}/">${unitTest.test.name}</a>
									</td>
									<td><form:input size="5" type="text" path="newSecretUnitTests[${unitTestIndex.index}].weight" value="${unitTest.weight}"/></td>
									<td class="pastaTF pastaTF${unitTest.test.tested}">${unitTest.test.tested}</td>
								</tr>
							</c:forEach>
							<tr id="buffer" class="dragBuffer sortableDisabled"></tr>
						</tbody>
					</table>
				</div>
			</td>
			<td valign="top">
				<div style="float:left; width:100%;">
					<h2> Available Unit Tests </h2>
					<table class='dragRows' style="width:100%;">
						<tr class="sortableDisabled"><th>Name</th><th>Weighting</th><th>Tested</th></tr>
						<tbody id="unitTest" class="sortable unusedUnitTest">
							<c:forEach var="unitTest" varStatus="unitTestIndex" items="${otherUnitTests}">
								<tr>
									<td>
										<input type="hidden" id="unusedUnitTest${unitTestIndex.index}.id" value="${unitTest.id}"/>
										<input type="hidden" id="unusedUnitTest${unitTestIndex.index}.test.id" value="${unitTest.test.id}"/>
										<a href="../../unitTest/${unitTest.test.id}/">${unitTest.test.name}</a>
									</td>
									<td><input size="5" type="text" id="unusedUnitTest${unitTestIndex.index}.weight" value="${unitTest.weight}"/></td>
									<td class="pastaTF pastaTF${unitTest.test.tested}">${unitTest.test.tested}</td>
								</tr>
							</c:forEach>
							<tr id="buffer" class="dragBuffer sortableDisabled"></tr>
						</tbody>
					</table>
				</div>
			</td>
		</tr>
	</table>
	
	<table style="margin-bottom:2em;width:90%">
		<tr>
			<td valign="top" >
				<div style="float:left; width:100%;">
					<h2> Hand Marking </h2>
					<table class='dragRows' style="width:100%;">
						<tr class="sortableDisabled"><th>Name</th><th>Weighting</th></tr>
						<tbody id="handMarking" class="sortable newHandMarking">
							<c:forEach var="handMarking" varStatus="handMarkingIndex" items="${assessment.handMarking}">
								<tr>
									<td>
										<form:input type="hidden" path="newHandMarking[${handMarkingIndex.index}].id" value="${handMarking.id}"/>
										<form:input type="hidden" path="newHandMarking[${handMarkingIndex.index}].handMarking.id" value="${handMarking.handMarking.id}"/>
										<a href="../../handMarking/${handMarking.handMarking.id}/">${handMarking.handMarking.name}</a>
									</td>
									<td><form:input size="5" type="text" path="newHandMarking[${handMarkingIndex.index}].weight" value="${handMarking.weight}"/></td>
								</tr>
							</c:forEach>
							<tr id="buffer" class="dragBuffer sortableDisabled"></tr>
						</tbody>
					</table>
				</div>
			</td>
			<td valign="top">
				<div style="float:left; width:100%;">
					<h2> Available Hand Marking</h2>
					<table class='dragRows' style="width:100%;">
						<tr class="sortableDisabled"><th>Name</th><th>Weighting</th></tr>
						<tbody id="handMarking" class="sortable unusedHandMarking">
							<c:forEach var="handMarking" varStatus="handMarkingIndex" items="${otherHandMarking}">
								<tr>
									<td>
										<input type="hidden" id="unusedHandMarking${handMarkingIndex.index}.id" value="${handMarking.id}"/>
										<input type="hidden" id="unusedHandMarking${handMarkingIndex.index}.handMarking.id" value="${handMarking.handMarking.id}"/>
										<a href="../../handMarking/${handMarking.handMarking.id}/">${handMarking.handMarking.name}</a>
									</td>
									<td><input size="5" type="text" id="unusedHandMarking${handMarkingIndex.index}.weight" value="${handMarking.weight}"/></td>
								</tr>
							</c:forEach>
							<tr id="buffer" class="dragBuffer sortableDisabled"></tr>
						</tbody>
					</table>
				</div>
			</td>
		</tr>
	</table>
	
	<table style="margin-bottom:2em;width:90%">
		<tr>
			<td valign="top" >
				<div style="float:left; width:100%;">
					<h2> Competitions </h2>
					<table class='dragRows' style="width:100%;">
						<tr class="sortableDisabled"><th>Name</th><th>Weighting</th></tr>
						<tbody id="competitions" class="sortable newCompetitions">
							<c:forEach var="competition" varStatus="competitionIndex" items="${assessment.competitions}">
								<tr>
									<td>
										<form:input type="hidden" path="newCompetitions[${competitionIndex.index}].id" value="${competition.id}"/>
										<form:input type="hidden" path="newCompetitions[${competitionIndex.index}].competition.id" value="${competition.competition.id}"/>
										<a href="../../competition/${competition.competition.id}/">${competition.competition.name}</a>
									</td>
									<td><form:input size="5" type="text" path="newCompetitions[${competitionIndex.index}].weight" value="${competition.weight}"/></td>
								</tr>
							</c:forEach>
							<tr id="buffer" class="dragBuffer sortableDisabled"></tr>
						</tbody>
					</table>
				</div>
			</td>
			<td valign="top">
				<div style="float:left; width:100%;">
					<h2> Available Competitions</h2>
					<table class='dragRows' style="width:100%;">
						<tr class="sortableDisabled"><th>Name</th><th>Weighting</th></tr>
						<tbody id="competitions" class="sortable unusedCompetition">
							<c:forEach var="competition" varStatus="competitionIndex" items="${otherCompetitions}">
								<tr>
									<td>
										<input type="hidden" id="unusedCompetition${competitionIndex.index}.id" value="${competition.id}"/>
										<input type="hidden" id="unusedCompetition${competitionIndex.index}.competition.id" value="${competition.competition.id}"/>
										<a href="../../competition/${competition.competition.id}/">${competition.competition.name}</a>
									</td>
									<td><input size="5" type="text" id="unusedCompetition${competitionIndex.index}.weight" value="${competition.weight}"/></td>
								</tr>
							</c:forEach>
							<tr id="buffer" class="dragBuffer sortableDisabled"></tr>
						</tbody>
					</table>
				</div>
			</td>
		</tr>
	</table>
	
	<input type="submit" value="Save Assessment" id="submit"/>
</form:form>

<script>
    $(function() {
    	$( "#strDate" ).datetimepicker({timeformat: 'hh:mm', dateFormat: 'dd/mm/yy'});
    	
    	$( "input:text" ).each(function() {
    		$textBox = $(this);
    		$textBox.tipsy({trigger: 'focus', gravity: 'w', title: function() {
    			if($(this).is("[id^='unused']")) {
    				return "Drag this row to the table on the left to select it.";
    			}
    			return "";
    		}});
    	});
    	
        $( "#unitTest.sortable" ).sortable({
            connectWith: "tbody",
            dropOnEmpty: true,
            
            stop: function(event, ui){
            	var tables = $("#unitTest.sortable");
            	// for each table
            	for(var i=0; i<tables.length; i++){
            		// for each child
            		var prefix = $.trim(tables[i].className.replace("sortable", "").replace("ui-sortable", ""));
            		var childrenNodes = tables[i].children;
            		for(var j=0; j<childrenNodes.length; ++j){
            			
						if(childrenNodes[j].getAttribute("id") != "buffer"){
							// td -> input - id
							childrenNodes[j].children[0].children[0].setAttribute("id", prefix+j+".id");
							childrenNodes[j].children[0].children[0].setAttribute("name", prefix+"["+j+"]"+".id");
							
							// td -> input - unitTest - id
							childrenNodes[j].children[0].children[1].setAttribute("id", prefix+j+".test.id");
							childrenNodes[j].children[0].children[1].setAttribute("name", prefix+"["+j+"]"+".test.id");
							
							// td -> input - weight
							childrenNodes[j].children[1].children[0].setAttribute("id", prefix+j+".weight");
							childrenNodes[j].children[1].children[0].setAttribute("name", prefix+"["+j+"]"+".weight");
						}
            		}
            	}
            	refreshDragBuffers();
            }
        });
        
        $( "#handMarking.sortable" ).sortable({
            connectWith: "tbody",
            dropOnEmpty: true,
            
            stop: function(event, ui){
            	var tables = $("#handMarking.sortable");
            	// for each table
            	for(var i=0; i<tables.length; i++){
            		// for each child
            		var prefix = $.trim(tables[i].className.replace("sortable", "").replace("ui-sortable", ""));
            		var childrenNodes = tables[i].children;
            		for(var j=0; j<childrenNodes.length; ++j){
            			
						if(childrenNodes[j].getAttribute("id") != "buffer"){
	            			// td -> input - id
	            			childrenNodes[j].children[0].children[0].setAttribute("id", prefix+j+".id");
	            			childrenNodes[j].children[0].children[0].setAttribute("name", prefix+"["+j+"]"+".id");
	            			
	            			// td -> input - handMarking - id
	            			childrenNodes[j].children[0].children[1].setAttribute("id", prefix+j+".handMarking.id");
	            			childrenNodes[j].children[0].children[1].setAttribute("name", prefix+"["+j+"]"+".handMarking.id");
	            			
	            			// td -> input - weight
	            			childrenNodes[j].children[1].children[0].setAttribute("id", prefix+j+".weight");
	            			childrenNodes[j].children[1].children[0].setAttribute("name", prefix+"["+j+"]"+".weight");
						}
            		}
            	}
            	refreshDragBuffers();
            }
        
        });
        
        $( "#competitions.sortable" ).sortable({
            connectWith: "tbody",
            dropOnEmpty: true,
            
            stop: function(event, ui){
            	var tables = $("#competitions.sortable");
            	// for each table
            	for(var i=0; i<tables.length; i++){
            		// for each child
            		var prefix = $.trim(tables[i].className.replace("sortable", "").replace("ui-sortable", ""));
            		var childrenNodes = tables[i].children;
            		for(var j=0; j<childrenNodes.length; ++j){

            			if(childrenNodes[j].getAttribute("id") != "buffer"){
	            			// td -> input - id
	            			childrenNodes[j].children[0].children[0].setAttribute("id", prefix+j+".id");
	            			childrenNodes[j].children[0].children[0].setAttribute("name", prefix+"["+j+"]"+".id");
	            			
	            			// td -> input - competition - id
	            			childrenNodes[j].children[0].children[1].setAttribute("id", prefix+j+".competition.id");
	            			childrenNodes[j].children[0].children[1].setAttribute("name", prefix+"["+j+"]"+".competition.id");
	            			
	            			// td -> input - weight
	            			childrenNodes[j].children[1].children[0].setAttribute("id", prefix+j+".weight");
	            			childrenNodes[j].children[1].children[0].setAttribute("name", prefix+"["+j+"]"+".weight");
            			}
            		}
            	}
            	refreshDragBuffers();
            }
        });
        
        $( "tbody.sortable").disableSelection();
        $('ul.tristate').tristate();
        
        $("#modifyDescription").bind('click', function(e) {
        	$("#description").parents('div').show();
        	$("#modifyDescription").hide();
        });
        
        var timer;
        $("#description").wysiwyg({
        	initialContent: function() {
    			return "";
    		},
        	controls: {
        		html  : { visible: true }
        	},
        	events: {
        		keyup: function() {
        			clearTimeout(timer);
                	timer = setTimeout(function() {
                		$("#descriptionHTML").html($("#description").val());
                	}, 1000);
        		}
        	}
        });
        
        refreshDragBuffers();
    });
    
    function refreshDragBuffers() {
    	$(".dragBuffer").each(function() {
    		var visible = $(this).siblings().length == 0;
    		$(this).toggle(visible);
    	});
    }
</script>

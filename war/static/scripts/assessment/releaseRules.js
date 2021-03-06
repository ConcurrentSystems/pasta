$(function() {
	loadEvents();
});

function loadEvents() {
	reloadEvents($(".first"));
	checkFullConjunctions();
	$("#submitButton").on('click', function() {
		$("#releaseRuleForm").submit();
	});
}

function reloadEvents($parentDiv) {
	$parentDiv.find( ".strDate" ).datetimepicker({timeformat: 'hh:mm', dateFormat: 'dd/mm/yy'});
	
	// Adding nested form:form elements should merge the forms, but it doesn't always do so
	// This line "fixes" that issue, though the underlying issue cannot be found.
	$parentDiv.find("form").each(function() {
		$(this).children().first().unwrap();
	});
	
	$parentDiv.find(".chosen").chosen({width: "22em"});
	$parentDiv.find(".chosen-no-search").chosen({width: "22em", disable_search: true});
	$parentDiv.find('.chosen-toggle').on('click', function(){
		$(this).parent().find('.selectAll option').prop('selected', $(this).hasClass('select')).parent().trigger('chosen:updated');
		return false;
	});
	
	
	$parentDiv.find(".changeRule, .setSubrule").chosen().on('change', function() {
		var $closestParent = $(this).closest('.ruleParent');
		var ruleName = $(this).children(':selected').val();
		var pathPrefix = $closestParent.attr('pathPrefix');
		
		var confirmResult;
		if($(this).is(".changeRule")) {
			var type;
			if(ruleName.endsWith("ReleaseAndRule")) {
				type="And";
			} else if(ruleName.endsWith("ReleaseOrRule")) {
				type="Or";
			}	
			if(type){
				var confirmMessage = "This will change the current rule into a sub-rule of an " + type.toUpperCase() + " rule.\n\n";
				var currentRuleName = $closestParent.find("[id$='ruleName']").val();
				var currentConj;
				if(currentRuleName.endsWith("ReleaseAndRule")) {
					currentConj = "AND";
				} else if(currentRuleName.endsWith("ReleaseOrRule")) {
					currentConj = "OR";
				}
				if(currentConj) {
					confirmMessage += "If you would rather convert this " + currentConj + " rule into an " + type.toUpperCase() + " rule, click 'Cancel'.";
				} else {
					confirmMessage += "If you would rather replace the rule entirely, click 'Cancel'.";
				}
				
				confirmResult = confirm(confirmMessage);
				if(confirmResult || currentConj) {
					$("#releaseRuleForm").append($("<input />", {
						type: 'hidden',
						name: 'basePath',
						value: pathPrefix
					}));
					$("#releaseRuleForm").append($("<input />", {
						type: 'hidden',
						name: 'newRuleType',
						value: type
					}));
					if(!confirmResult) {
						$("#releaseRuleForm").append($("<input />", {
							type: 'hidden',
							name: 'changeConjunction',
							value: true
						}));
					}
					var $form = $("#releaseRuleForm");
					$form.attr("action", $form.attr("action") + "convertToJoin/");
					$form.submit();
				}
			}
		}
		if(!confirmResult) {
			addRule($closestParent, ruleName, pathPrefix);
		}
	});
	
	// controls for the "delete rule" button
	$parentDiv.find(".deleteRule").on('click', function() {
		var $container = $(this).closest(".ruleParent");
		
		var deleting = !$container.is(".toDelete"); 
		$container.toggleClass("toDelete");
		
		$(this).text(deleting ? "Undelete Rule" : "Delete Rule");
		
		$container.find("input").prop("disabled", deleting);
		$container.find("select").prop("disabled", deleting).trigger('chosen:updated');
		
		if(deleting) {
			$container.find(".showChangeRule").first().parent().hide();
			$container.attr("oht", $container.height())
			$container.css("overflow", "hidden");
			$container.animate({height:$(this).parent().next().css("height")}, 150);
			$container.find("#deleteMessage").first().text(" (will be deleted)");
		} else {
			$container.find("#deleteMessage").first().text("").first();
			$container.animate({height:$container.attr("oht")}, 150, function() {
				$container.removeAttr("oht");
				$container.removeAttr("style");
			});
			$container.find(".showChangeRule").first().parent().show();
		}
	});
	
	// controls for the "change" button
	$parentDiv.find('div.changeRuleDiv').hide();
	$parentDiv.find('a.showChangeRule').on('click', function() {
		$(this).next().show();
		$(this).hide();
	});
	
	$parentDiv.find("input.percentCheck").on("change", function() {
		var $cell = $(this).closest(".pf-item").children(".pf-label").first();
		$cell.text($(this).is(":checked") ? "Percent (0 - 100):" : "Mark:");
	});
	$parentDiv.find("input.setMark").on("keyup", function() {
		$(this).tipsy("hide");
		$(this).tipsy("show");
	});
	
	// Allows looping of nested colours infinitely
	$("div.first > div > div > div > div.subRule > div > div > div > div.subRule > div > div > div > div.subRule").addClass("first");
}

function checkFullConjunctions() {
	// Add the option to add a new subrule when all subrules have been set.
	$('.ruleParent').has('.subRule').each(function() {
		$parent = $(this);
		var full = true;
		$conjunction = $();
		$subRules = $parent.children().children().children().children('.subRule');
		$subRules.each(function() {
			if($(this).children().children().length <= 1 && !$(this).children().children().first().is("form")) {
				full = false;
			}
			$conjunction = $(this).siblings(".conjunction").first();
			if($conjunction.length == 0) {
				$conjunction = jQuery("<div/>", {
					class : 'conjunction',
					text : $(this).attr('conjunction')
				});
			}
		});
		if(full) {
			$lastRule = $subRules.last();
			$conjClone = $conjunction.clone();
			$lastRule.after($conjClone);
			
			var pathPrefix = $(this).attr('pathPrefix');
			var newPathPrefix = (pathPrefix ? pathPrefix : "") +".rules[" + $subRules.length + "]";
			$newParent = jQuery("<div />", {
				class: "section ruleParent subRule",
				pathPrefix: newPathPrefix
			});
			$conjClone.after($newParent);
			addRule($newParent, "", newPathPrefix);
		}
	});
}

function addRule($parentDiv, ruleName, pathPrefix) {
	$parentDiv.load("load/", {"ruleName":ruleName, "pathPrefix":(pathPrefix ? pathPrefix : "")}, function(text, status) {
		if(status !== "error") {
			reloadEvents($parentDiv);
			if(ruleName) {
				checkFullConjunctions();
			}
		}
	});
}

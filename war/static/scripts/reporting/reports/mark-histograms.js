(function() {

	displayCallbacks["displayHistograms"] = function(content, allData) {
		var form = $("<div/>").addClass("pasta-form no-width part").appendTo(content);
		var row = $("<div/>").addClass("pf-horizontal two-col").appendTo(form);
		var selectDiv = $("<div/>").addClass("pf-item").appendTo(row);
		var sliderDiv = $("<div/>").addClass("pf-item hidden").appendTo(row);
		
		$("<div/>").addClass("pf-label").text("Assessment:").appendTo(selectDiv);
		var selectInputDiv = $("<div/>").addClass("pf-input").appendTo(selectDiv);
		
		var select = createAssessmentSelect(content, allData.assessments);
		select.appendTo(selectInputDiv);
		
		var sliderLabelDiv = $("<div/>").addClass("pf-label").text("Number of buckets: ").appendTo(sliderDiv);
		var sliderInputDiv = $("<div/>").addClass("pf-input").appendTo(sliderDiv);
		
		var slider = $("<input/>", {
			type: "range",
			min: 1,
			max: 10
		}).appendTo(sliderInputDiv);
		
		var sliderLabel = $("<span/>", {
			text: "Value",
		}).appendTo(sliderLabelDiv);
		
		var graph;
		
		select.chosen({
			width: '100%'
		}).on("change", function(){
			var assessment = $(this).find("option:selected").data("assessment");
			var numBuckets = idealBuckets(assessment.maxMark);
			
			if(!graph) {
				graph = $("<div/>").addClass("graph-container part").appendTo(content);
			}
			
			sliderDiv.toggleClass("hidden", assessment.maxMark <= 1);
			slider.attr("max", Math.max(1, assessment.maxMark));
			slider.val(numBuckets);
			
			slider.off("input");
			slider.on("input", function() {
				sliderLabel.text(slider.val());
				var loading = $("<div/>").addClass("loading").loading().appendTo(content);
				plotAssessmentMarks(assessment, slider.val(), graph);
				loading.remove();
			});
			
			slider.trigger("input");
		});
	}
	
	function createAssessmentSelect(container, assessments) {
		var select = $("<select/>");
		$("<option/>").appendTo(select);
		$.each(assessments, function(i, assessment) {
			$("<option/>", {
				text: assessment.assessment.name,
				value: assessment.assessment.id
			})
			.data("assessment", assessment)
			.appendTo(select);
		});
		return select;
	}
	
	function plotAssessmentMarks(assessment, numBuckets, container) {
		var plotData = getPlotData(assessment.marks.slice(), numBuckets, assessment.maxMark);
		Highcharts.chart(container[0], {
			chart: {
				type: 'column'
			},
			title: {
		        text: 'Assessment Marks for ' + assessment.assessment.name
		    },
			xAxis: {
				title: {
					text: "Total Mark"
				},
				categories: plotData.buckets,
			},
			yAxis: {
				title: {
					text: "Count"
				}
			},
			legend: {
				enabled: false
			},
			series: [{
				name: "Submissions",
				data: plotData.counts,
				pointPadding: 0,
		        groupPadding: 0,
			}],
		});
	}
	
	function getPlotData(data, numBuckets, max) {
		data.sort(function(a, b) {
			return a - b
		});
		var step = Math.round((max / numBuckets) * 100) / 100;
		var buckets = ["N/A"];
		var counts = [0];
		var next = 0;
		
		// Count non-submissions
		while (data[0] < 0) {
			counts[0]++;
			data.shift();
		}
	
		for (var i = 0; i < numBuckets; i++) {
			var start = next;
			if (i < numBuckets - 1) {
				next = Math.round((next + step) * 100) / 100;
				buckets.push("[" + start + "-" + next + ")");
			} else {
				next = max;
				buckets.push("[" + start + "-" + next + "]");
				counts.push(data.length);
				break;
			}
			var count = 0;
			while (data[0] < next) {
				count++;
				data.shift();
			}
			counts.push(count);
		}
	
		if(max == 0 && buckets.length == 2) {
			buckets[1] = "Submitted";
		}
		
		return {
			"buckets" : buckets,
			"counts" : counts
		};
	}
	
	function idealBuckets(num) {
		if(num <= 1) {
			return 1;
		}
		if(num <= 10) {
			return num;
		}
		var best = 1;
		for(var i = 2; i <= num; i++) {
			if(num % i == 0) {
				if(i < 10) {
					best = i;
				} else {
					return i;
				}
			}
		}
		return num;
	}

})();
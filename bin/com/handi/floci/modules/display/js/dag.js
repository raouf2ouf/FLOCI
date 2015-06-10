

window.onload = function() {    
	d3.json("data.json", function(error, json) {
	var data = json;
	var nodes = data.nodes,
		links = data.links;

	window.dag = new FLOCIDAG(document.body, nodes, links, []);

	d3.json("degrees.json", function(error, degrees) {
		window.dag.drawMembershipDegree(degrees);
	});

	});
};
var width = 1200,
	height = 1200;

/* Get a reference to the chart and indicate its width and height */
/*var svg = d3.select(".chart")
    .attr("width", width)
    .attr("height", height);*/

// Create the input graph
var g = new dagreD3.graphlib.Graph()
  .setGraph({})
  .setDefaultEdgeLabel(function() { return {}; });


d3.json("data.json", function(error, json) {
	var data = json;
	var nodes = data.nodes,
		links = data.links;
	

  	nodes.forEach( function(node) { g.setNode(node.name, { label: node.name }); });
  	links.forEach( function(link) { g.setEdge(link.source, link.target);
  	});

  	// Set up an SVG group so that we can translate the final graph.
	var svg = d3.select("svg").attr("width", width),
	    svgGroup = svg.append("g");

	// Set up zoom support
	var zoom = d3.behavior.zoom().on("zoom", function() {
	      svgGroup.attr("transform", "translate(" + d3.event.translate + ")" +
	                                  "scale(" + d3.event.scale + ")");
	    });
	svg.call(zoom);

	// Create the renderer
	var render = new dagreD3.render();
	debugger;
	// Run the renderer. This is what draws the final graph.
	render(d3.select("svg g"), g);

	// Center the graph
	/*var xCenterOffset = (svg.attr("width") - g.graph().width) / 2;
	svgGroup.attr("transform", "translate(" + xCenterOffset + ", 20)");
	svg.attr("height", g.graph().height + 40);*/

	// Center the graph
	var initialScale = 0.75;
	zoom.translate([(svg.attr("width") - g.graph().width * initialScale) / 2, 20])
	  .scale(initialScale)
	  .event(svg);
	svg.attr('height', g.graph().height * initialScale + 40);
});
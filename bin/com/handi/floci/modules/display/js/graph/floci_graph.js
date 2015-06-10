// lightweight is an optional argument that will try to draw the graph as fast as possible
function FLOCIDAG(attachPoint, nodes, links, /*optional*/ params) {
    var dag = this;
    
    // Get the necessary parameters
    var lightweight = params.lightweight ? true : false;
    
    // Twiddle the attach point a little bit
    // rootSVG is the svg containing the graph svg and the minimap svg...
    var rootSVG = d3.select(attachPoint).append("svg").attr("width", attachPoint.clientWidth).attr("height", attachPoint.clientHeight); // the attachPoint must not be an svg, as that would make it redundant. it can be for eg. document.body
    
    var graphSVG = rootSVG.append("svg").attr("width", "100%").attr("height", "100%").attr("class", "graph-attach");
    //graphSVG.node().oncontextmenu = function(d) { return false; }; // graphSVG.node() it to get the svg since append returns an array of the appended svg?
    
    var minimapSVG = rootSVG.append("svg").attr("class", "minimap-attach");
    //var listSVG = rootSVG.append("svg").attr("class", "history-attach");
    
    // Create the graph and history representations
    var graph = createGraphFromData(nodes, links);
    //var history = DirectedAcyclicGraphHistory();
    
    
    // Create the chart instances
    var DAG = DirectedAcyclicGraph().animate(!lightweight); // this returns the graph function of the DirectedAcyclicGraph
    DAG.bbox(function(d) {
        return d3.select(this).select("circle").node().getBBox();
    });

    /*DAG.nodeTranslate = function(d) {
        return "rotate(" + (d.x - 90) + ")translate(" + d.y + ")";
    }*/

    /*DAG.splineGenerator = function(d) { // generates the line to draw
        /*return d3.svg.line().x(function(d) { return d.x }).y(function(d) { return d.y }).interpolate("basis")(edgepos.call(this, d));*/
        /*return d3.svg.diagonal.radial().projection(function(d) { return [d.y, d.x / 180 * Math.PI]; }).interpolate("basis")(DAG.edgepos.call(this, d))*/;
       /* return d3.svg.line()
            .x(function(d) { return d.y })
            .y(function(d) { return d.x / 180 * Math.PI })
            .interpolate("basis")(DAG.edgepos.call(this, d));
    }*/
    

    DAG.drawnode(function(d) {
        var node = d3.select(this);

        node.append("circle").attr("r", 20);
        
        node.append("text")
            .attr("text-anchor", "middle")
            .attr("x", 0)
            .attr("dy", 35)
            .text(d.id);
        if(d.hasOwnProperty("degree")) {
            node.append("text")
              .attr("class", "degree")
              .attr("x", 0)
              .attr("dy", 5)
              .attr("text-anchor", function(d) { return d.children ? "end" : "start"; })
              .text(function(d) { return (d.degree > 1.0) ? "P" : d.degree; });        
        }
    
        var prior_pos = DAG.nodepos.call(this, d);
        if (prior_pos!=null) {
            d3.select(this).attr("transform", DAG.nodeTranslate);
        }
    });
    var DAGMinimap = DirectedAcyclicGraphMinimap(DAG).width("19.5%").height("19.5%").x("80%").y("80%");
    //var DAGHistory = List().width("15%").height("99%").x("0.5%").y("0.5%");
    //var DAGTooltip = DirectedAcyclicGraphTooltip();
    //var DAGContextMenu = DirectedAcyclicGraphContextMenu(graph, graphSVG);
    var test = 5;
    // Attach the panzoom behavior
    var refreshViewport = function() {
        var t = zoom.translate();
        var scale = zoom.scale();
        graphSVG.select(".graph").attr("transform","translate("+t[0]+","+t[1]+") scale("+scale+")");
        minimapSVG.select('.viewfinder').attr("x", -t[0]/scale).attr("y", -t[1]/scale).attr("width", attachPoint.offsetWidth/scale).attr("height", attachPoint.offsetHeight/scale);
        if (!lightweight) graphSVG.selectAll(".node text").attr("opacity", 3*scale-0.3);
    }
    
    var zoom = MinimapZoom().scaleExtent([0.001, 2.0]).on("zoom", refreshViewport);
    zoom.call(this, rootSVG, minimapSVG);
    
    // A function that resets the viewport by zooming all the way out
    var resetViewport = function() {
      var curbbox = graphSVG.node().getBBox();
      var bbox = { x: curbbox.x, y: curbbox.y, width: curbbox.width+50, height: curbbox.height+50};
      scale = Math.min(attachPoint.offsetWidth/bbox.width, attachPoint.offsetHeight/bbox.height);
      w = attachPoint.offsetWidth/scale;
      h = attachPoint.offsetHeight/scale;
      tx = ((w - bbox.width)/2 - bbox.x + 25)*scale;
      ty = ((h - bbox.height)/2 - bbox.y + 25)*scale;
      zoom.translate([tx, ty]).scale(scale);
      refreshViewport();
    }

    // A function that attaches mouse-click events to nodes to enable node selection
    function setupEvents(){
        var nodes = graphSVG.selectAll(".node");
        var edges = graphSVG.selectAll(".edge");
        //var items = listSVG.selectAll(".item");
    
        // Set up node selection events
        var select = Selectable().getrange(function(a, b) {
            var path = getNodesBetween(a, b).concat(getNodesBetween(b, a));
            return nodes.data(path, DAG.nodeid());
        }).on("select", function() {
            var selected = {};
            graphSVG.selectAll(".node.selected").data().forEach(function(d) { selected[d.id]=true; });
            edges.classed("selected", function(d) {
                return selected[d.source.id] && selected[d.target.id]; 
            });
            //attachContextMenus();
            //DAGTooltip.hide();
        });
        select(nodes);
    
        
        if (!lightweight) {
            nodes.on("mouseover", function(d) {
                graphSVG.classed("hovering", true);
                highlightPath(d);
            }).on("mouseout", function(d){
                graphSVG.classed("hovering", false);
                edges.classed("hovered", false).classed("immediate", false);
                nodes.classed("hovered", false).classed("immediate", false);
            });
        }
        
        function highlightPath(center) {        
            var path = getEntirePathLinks(center);
            
            var pathnodes = {};
            var pathlinks = {};
            
            path.forEach(function(p) {
               pathnodes[p.source.id] = true;
               pathnodes[p.target.id] = true;
               pathlinks[p.source.id+p.target.id] = true;
            });
            
            edges.classed("hovered", function(d) {
                return pathlinks[d.source.id+d.target.id];            
            })
            nodes.classed("hovered", function(d) {
                return pathnodes[d.id];
            });
            
            var immediatenodes = {};
            var immediatelinks = {};
            immediatenodes[center.id] = true;
            center.getVisibleParents().forEach(function(p) {
                immediatenodes[p.id] = true;
                immediatelinks[p.id+center.id] = true;
            })
            center.getVisibleChildren().forEach(function(p) {
                immediatenodes[p.id] = true;
                immediatelinks[center.id+p.id] = true;
            })
            
            edges.classed("immediate", function(d) {
                return immediatelinks[d.source.id+d.target.id];
            })
            nodes.classed("immediate", function(d) {
                return immediatenodes[d.id];
            })
        }
    }
    
    // The main draw function
    this.draw = function() {
        //DAGTooltip.hide();                  // Hide any tooltips
        console.log("draw begin")
        var begin = (new Date()).getTime();  
        var start = (new Date()).getTime();     
        graphSVG.datum(graph).call(DAG);    // Draw a DAG at the graph attach
        console.log("draw graph", new Date().getTime() - start);
        start = (new Date()).getTime();    
        minimapSVG.datum(graphSVG.node()).call(DAGMinimap);  // Draw a Minimap at the minimap attach
        console.log("draw minimap", new Date().getTime() - start);
        //start = (new Date()).getTime();
        //graphSVG.selectAll(".node").call(DAGTooltip);        // Attach tooltips
        //console.log("draw tooltips", new Date().getTime() - start);
        start = (new Date()).getTime();
        setupEvents();                      // Set up the node selection events
        console.log("draw events", new Date().getTime() - start);
        //start = (new Date()).getTime();
        refreshViewport();                  // Update the viewport settings
        console.log("draw viewport", new Date().getTime() - start);
        //start = (new Date()).getTime();
        //attachContextMenus();
        //console.log("draw contextmenus", new Date().getTime() - start);
        console.log("draw complete, total time=", new Date().getTime() - begin);
    }
    
    //Call the draw function
    this.draw();
    
    // Start with the graph all the way zoomed out
    resetViewport();

    // Save important variables
    this.attachPoint = attachPoint;
    //this.reports = reports;
    this.DAG = DAG
    this.DAGMinimap = DAGMinimap;
    /*this.DAGHistory = DAGHistory;
    this.DAGTooltip = DAGTooltip;
    this.DAGContextMenu = DAGContextMenu;*/
    this.graph = graph;
    //this.resetViewport = resetViewport;
    //this.history = history;
    
    // Draw the degrees and change the class of each concept
    this.drawMembershipDegree = function(degrees) {
         // Transforming the degrees array to a map
        var degrees_map = {};
        degrees.forEach(function(degreeObject) {
            degrees_map[degreeObject.name] = degreeObject.degree;
        }); 

        // for each node change add the memebership degree
        var nodes = graphSVG.selectAll(".node");
        
        nodes.data().forEach(function(d) { 
            d.degree = degrees_map[d.id];
        });

        // depending on the memebership degree add class
        nodes.classed("degree00", function(d) { return (d.degree <= 0.0); });
        nodes.classed("degree01", function(d) { return (d.degree >= 0.0 && d.degree < 0.1); });
        nodes.classed("degree02", function(d) { return (d.degree >= 0.1 && d.degree < 0.2); });
        nodes.classed("degree03", function(d) { return (d.degree >= 0.2 && d.degree < 0.3); });
        nodes.classed("degree04", function(d) { return (d.degree >= 0.3 && d.degree < 0.4); });
        nodes.classed("degree05", function(d) { return (d.degree >= 0.4 && d.degree < 0.5); });
        nodes.classed("degree06", function(d) { return (d.degree >= 0.5 && d.degree < 0.6); });
        nodes.classed("degree07", function(d) { return (d.degree >= 0.6 && d.degree < 0.7); });
        nodes.classed("degree08", function(d) { return (d.degree >= 0.7 && d.degree < 0.8); });
        nodes.classed("degree09", function(d) { return (d.degree >= 0.8 && d.degree < 0.9); });
        nodes.classed("degree10", function(d) { return (d.degree >= 0.9 && d.degree <= 1.0); });
        nodes.classed("degreeP", function(d) { return (d.degree > 1.0); });

        nodes.append("text")
              .attr("class", "degree")
              .attr("x", 0)
              .attr("dy", 5)
              .attr("text-anchor", "middle")/*function(d) { return d.children ? "end" : "start"; })*/
              .text(function(d) { return (d.degree > 1.0) ? "P" : d.degree; }); 
    }

    // Add a play button
//    console.log("appending play button");
//    var playbutton = rootSVG.append("svg").attr("x", "10").attr("y", "5").append("text").attr("text-anchor", "left").append("tspan").attr("x", 0).attr("dy", "1em").text("Click To Play").on("click",
//            function(d) {
//        animate();
//    });
    
    /*var animate = function() {
        var startTime = new Date().getTime();
        
        // Find the min and max times
        var max = 0;
        var min = Infinity;
        graphSVG.selectAll(".node").each(function(d) {
            var time = parseFloat(d.report["Timestamp"]);
            if (time < min) {
                min = time;
            }
            if (time > max) {
                max = time;
            }
        })
        
        var playDuration = 10000;
        
        var update = function() {
            var elapsed = new Date().getTime() - startTime
            var threshold = (elapsed * (max - min) / playDuration) + min;
            graphSVG.selectAll(".node").attr("display", function(d) {
                d.animation_hiding = parseFloat(d.report["Timestamp"]) < threshold ? null : true;
                return d.animation_hiding ? "none" : "";
            });
            graphSVG.selectAll(".edge").attr("display", function(d) {
                return (d.source.animation_hiding || d.target.animation_hiding) ? "none" : ""; 
            })
            if (elapsed < playDuration) {
                window.setTimeout(update, 10);
            }
        }
        update();
        
    }*/
}
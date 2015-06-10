
var window = window ? window : {};
// Problems with resizing and jquery and chrome and this stuff is so dumb.
window.width = function() {
	return document.body.clientWidth;
};

window.height = function() {
	return document.body.clientHeight;
};

// http://stackoverflow.com/questions/523266/how-can-i-get-a-specific-parameter-from-location-search
var getParameter = function(name) {
    name = name.replace(/[\[]/,"\\\[").replace(/[\]]/,"\\\]");
    var regexS = "[\\?&]"+name+"=([^&#]*)";
    var regex = new RegExp( regexS );
    var results = regex.exec( window.location.href );
    if( results == null )
        return "";
    else
        return results[1];
};

var getParameters = function() {
    if (window.location.href.indexOf("?")==-1) return {};
    var param_strs = window.location.href.substr(window.location.href.indexOf("?")+1).split("&");
    var params = {};
    param_strs.forEach(function(str) {
        splits = str.split("=");
        if (splits.length==2) {
            params[splits[0]] = splits[1];
        }
    });
    return params;
};

var sanitizeReports = function(reports) {
    var i = 0;
    var erroneous = { "edges": [], "ids": []};
    while (i < reports.length) {
        var report = reports[i];
        if (!report.hasOwnProperty("Edge") || report["Edge"].length==0) {
          erroneous.edges.push(report);
        	report["Edge"] = [];
        } else if (!report.hasOwnProperty("X-Trace") || report["X-Trace"].length!=1) {
          erroneous.ids.push(report);
        	reports.splice(i, 1);
        	i--;
        }
        i++;
    }
    if (erroneous.edges.length>0 || erroneous.ids.length>0) {
      if (erroneous.edges.length>0)
        console.warn("Warning: "+erroneous.edges.length+" reports with no edges");
      if (erroneous.ids.length>0)
        console.warn("Warning: "+erroneous.ids.length+" reports with no or bad ID");
      console.warn("Erroneous reports: ", erroneous);
    }
    
    return reports;    
};


var createGraphFromData = function(nodes, edges) {
    console.log("Creating graph from data");

    /* the data here is composed of nodes and edges, so it has to be converted to a Graph object composed of nodes each one references its children and parents
    so you have to get for each node its parents/children, this implise to transform the edges array to a map to facilitate the lookup */
    
    // Transforming the edges array to a map
    var edges_map = {};
    edges.forEach(function(edge) {
        if(edges_map[edge.target]) edges_map[edge.target].push(edge.source); // if the target already exists, add the source to the array
        else edges_map[edge.target] = [edge.source];
    }); 

    // Create nodes
    console.info("Creating graph nodes");
    var nodes_map = {};
    nodes.forEach(function(node) {
        if(node.hasOwnProperty("id")) { // if nodes are identified with id property
            nodes_map[node.id] = new Node(node.id);
            // add the node datato the Node in case there are some informations other than the id
            nodes_map[node.id].node = node;
        } else if(node.hasOwnProperty("name")) { // if nodes are identified with name property
            nodes_map[node.name] = new Node(node.name);
            // add the node data to the Node in case there are some informations other than the id
            nodes_map[node.name].node = node;
        }
    });

    // Second link the nodes together
    console.info("Linking graph nodes");
    for (var nodeid in nodes_map) {
        var node = nodes_map[nodeid];
        if(edges_map[node.id]) { // if this node has a parent
            edges_map[node.id].forEach(function(source) {
                if(!nodes_map[source]) console.log(source + ":" + node.id);
                nodes_map[source].addChild(node); // add this node to the parent's children
                node.addParent(nodes_map[source]); // add the parent to this node's parents
            });
        }
    }
    
    // Create the graph and add the nodes
    var graph = new Graph();
    for (var id in nodes_map) {
        graph.addNode(nodes_map[id]);
    }
    
    console.log("Done creating graph from reports");
    return graph;
}

var createJSONFromVisibleGraph = function(graph) {
    var nodes = graph.getVisibleNodes();
    var reports = [];
    
    for (var i = 0; i < nodes.length; i++) {
        var node = nodes[i];
        var parents = node.getVisibleParents();
        var report = $.extend({}, node.report);
        report["Edge"] = [];
        for (var j = 0; j < parents.length; j++) {
            report["Edge"].push(parents[j].id);
        }
        reports.push(report);
    }
    
    return {"reports": reports};
}


//Javascript impl of java's string hashcode:
//http://werxltd.com/wp/2010/05/13/javascript-implementation-of-javas-string-hashcode-method/
String.prototype.hashCode = function(){
 var hash = 0, i, char;
 if (this.length == 0) return hash;
 for (i = 0; i < this.length; i++) {
     char = this.charCodeAt(i);
     hash = ((hash<<5)-hash)+char;
     hash = hash & hash; // Convert to 32bit integer
 }
 return hash;
};

function hash_report(report) {
 hash = 0;
 if (report["Agent"]) hash += ("Agent:"+report["Agent"]).hashCode();
 if (report["Label"]) hash += ("Label:"+report["Label"]).hashCode();
 if (report["Class"]) hash += ("Class:"+report["Class"]).hashCode();
 return hash & hash;
}

var filter_reports = function(reports, f) {    
    // Figure out which reports have to be removed
    var retained = {};
    var removed = {};
    var reportmap = {};
    for (var i = 0; i < reports.length; i++) {
        var report = reports[i];
        var id = report.EventID;
        reportmap[id] = report;
        if (f(report)) {
            removed[id]=report;
        } else {
            retained[id]=report;
        }
    }

    var remapped = {};
    var num_calculated = 0;
    var remap_parents = function(id) {
        if (remapped[id]) {
            return;
        } else {
            var report = reportmap[id];
            var parents = report["Edge"];
            var newparents = {};
            for (var i = 0; i < parents.length; i++) {
                if (removed[parents[i]]) {
                    remap_parents(parents[i]);
                    reportmap[parents[i]]["Edge"].forEach(function(grandparent) {
                        newparents[grandparent] = true;
                    })
                } else {
                    newparents[parents[i]] = true;
                }
            }
            report["Edge"] = Object.keys(newparents);
            remapped[id] = true;
        }
    }
    
    return Object.keys(retained).map(function(id) {
        remap_parents(id);
        return retained[id];
    })
}


var kernelgraph_for_trace = function(trace) {
    return KernelGraph.fromJSON(trace);
}

var report_id = function(report) {
	return report.EventID;
}

// generates numeric ids starting from 0, never reuses same number
var unique_id = function(){
	var seed = 0;
	return function() { 
		return seed++;
	};
}();

// generates random strings of default length 8 consisting of only letters
var random_string = function(/*optional*/ length)
{
	if (!length)
		length = 8;
    var text = "";
    var possible = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    for( var i=0; i < length; i++ )
        text += possible.charAt(Math.floor(Math.random() * possible.length));

    return text;
};



function group_reports_by_field(reports, field) {
  var grouping = {};
  for (var i = 0; i < reports.length; i++) {
    try {
      var value = reports[i][field];
      if (!(value in grouping))
        grouping[value] = [];
      grouping[value].push(reports[i]);
    } catch (e) {
      console.log(e);
    }
  }
  return grouping;
};
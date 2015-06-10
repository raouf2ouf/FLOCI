/*
 * This file contains the prototypes for Graph and Node
 */

var Node = function(id) {
    // Save the arguments
    this.id = id;
    
    // Default values for internal variables
    this.never_visible        = false;
    this.hidden               = false;
    this.child_nodes          = {}; // The immediate child nodes in the graph, regardless of visibility
    this.parent_nodes         = {}; // The immediate parent nodes in the graph, regardless of visibility
}

Node.prototype.visible = function(_) {
    if (arguments.length==0) return (!this.never_visible && !this.hidden)
    this.hidden = !_;
    return this;
}

Node.prototype.addChild = function(child) {
    this.child_nodes[child.id] = child;
}

Node.prototype.addParent = function(parent) {
    this.parent_nodes[parent.id] = parent;
}

Node.prototype.removeChild = function(child) {
    if (child.id in this.child_nodes) delete this.child_nodes[child.id];
}

Node.prototype.removeParent = function(parent) {
    if (parent.id in this.parent_nodes) delete this.parent_nodes[parent.id];
}

Node.prototype.getParents = function() {
    return values(this.parent_nodes);
}

Node.prototype.getChildren = function() {
    return values(this.child_nodes);
}

Node.prototype.getVisibleParents = function() {   
    var visible_parent_map = {}; // a map of a map containing for each node a map of his visible parents
    
    var explore_node = function(node) {
        if (visible_parent_map[node.id]) {
            return;
        }
        visible_parent_map[node.id] = {}; // initialize the parent map for this node
        var parents = node.parent_nodes;
        for (var pid in parents) {
            var parent = parents[pid];
            if (parent.visible()) {
                visible_parent_map[node.id][pid] = parent; // if the parent is visible, add it to the parent map
            } else {
                // if the parent is not visible, check the grandparents
                /* since "visible_parent_map" is a global variable it will contain all the visible parent of this parent
                inside a map with the key "parent.id" */
                explore_node(parent);
                // get the grandparents from the map of the parent and put them inside the map of the child  
                var grandparents = visible_parent_map[pid];
                for (var gpid in grandparents) {
                    visible_parent_map[node.id][gpid] = grandparents[gpid];
                }
            }
        }
    }
    // call the function on this node
    explore_node(this);
    
    return values(visible_parent_map[this.id]);
}

Node.prototype.getVisibleChildren = function() {
    var visible_children_map = {};
    
    var explore_node = function(node) {
        if (visible_children_map[node.id]) {
            return;
        }
        visible_children_map[node.id] = {};
        var children = node.child_nodes;
        for (var pid in children) {
            var child = children[pid];
            if (child.visible()) {
                visible_children_map[node.id][pid] = child;
            } else {
                explore_node(child);
                var grandchildren = visible_children_map[pid];
                for (var gcid in grandchildren) {
                    visible_children_map[node.id][gcid] = grandchildren[gcid];
                }
            }
        }
    }
    
    explore_node(this);
    
    return values(visible_children_map[this.id]);
}

var Graph = function() {
    // Default values for internal variables
    this.nodelist = []
    this.nodes = {};
}

Graph.prototype.addNode = function(node) {
    this.nodelist.push(node);
    this.nodes[node.id] = node;
}

Graph.prototype.getNode = function(id) {
    return this.nodes[id];
}

Graph.prototype.getNodes = function() {
    return this.nodelist;
}

Graph.prototype.getVisibleNodes = function() {
    return this.nodelist.filter(function(node) { return node.visible(); });
}

Graph.prototype.getVisibleLinks = function() {
    var visible_parent_map = {}; // <node.id, <parent.id,boolean> > a map of a map containing for each node a map of its visible parent(s) 
    /* This fonction is to get visible parents of a node, we could have used node.getVisibleParents(), 
    but this is the same implementation except that it only stores the id of the parents and not the parents themselves, 
    thus saving computing time and memory*/
    var explore_node = function(node) {
        if (visible_parent_map[node.id]) {
            return;
        }
        visible_parent_map[node.id] = {};
        var parents = node.parent_nodes;
        for (var pid in parents) {
            var parent = parents[pid];
            if (parent.visible()) {
                visible_parent_map[node.id][pid] = true;
            } else {
                explore_node(parent);
                var grandparents = visible_parent_map[pid];
                for (var gpid in grandparents) {
                    visible_parent_map[node.id][gpid] = true;
                }
            }
        }
    }
    // get the visible parent(s) of all nodes. why all nodes? why not just the visible ones since you are goining to link only those??
    for (var i = 0; i < this.nodelist.length; i++) {
        explore_node(this.nodelist[i]);
    }
    
    var nodes = this.nodes;
    var ret = [];
    var visible_nodes = this.getVisibleNodes();

    // For every visible node link it to its visible parent(s)
    for (var i = 0; i < visible_nodes.length; i++) {
        var node = visible_nodes[i];
        var parentids = visible_parent_map[node.id];
        Object.keys(parentids).forEach(function(pid) {
            ret.push({source: nodes[pid], target: node});
        })
    }

    return ret;
}

/*
 * The functions below are just simple utility functions
 */

function getNodesBetween(a, b) {
    // Returns a list containing all the nodes between a and b, including a and b
    var between = {}; // a map <node.id, boolean>
    var nodesBetween = [a, b]; // an array containing the nodes a and b, why not an empty one?? since a and b will be added eventualy if they are related?? 
    var get = function(p) { // this function returns a boolean
        if (between[p.id] == null) { // the node has not been tested yet
            if (p==b) { // we have reached the limit (node b)
                nodesBetween.push(p); // don't understand why push p since it is b and b have already been pushed??
                between[p.id] = true;
            } else if (p.getParents().map(get).indexOf(true)!=-1) { // look at the parent(s) of p and call this fuction recursivly, if there is true then one parent is b or a child of b
                nodesBetween.push(p);
                between[p.id] = true;
            } else {
                between[p.id] = false;
            }
        }
        return between[p.id];
    }
    get(a); // start from the small node a and up to b.
    return nodesBetween;
}

function getEntirePathNodes(center) { 
    // Returns a list containing all nodes from the center node to the top and to the bottom
    var visible_parent_map = {}; // <node.id, <parent.id,boolean> >
    var visible_child_map = {}; // <node.id, <child.id,boolean> >
    var nodes = {};
    // The same implementation again, we can't use the old ones because they return arrays and we need maps, why not optimise this??
    /* this function populate "visible_parent_map" with the all the visible parents from the node to the top 
    (unlike the node.getVisibleParents() that is limited to the direct visible parent(s)) */
    var explore_parents = function(node) {
        if (visible_parent_map[node.id]) {
            return;
        }
        visible_parent_map[node.id] = {};
        nodes[node.id] = node;
        var parents = node.parent_nodes;
        for (var pid in parents) {
            var parent = parents[pid];
            if (parent.visible()) {
                visible_parent_map[node.id][pid] = true;
                explore_parents(parent);
            } else {
                explore_parents(parent);
                var grandparents = visible_parent_map[pid];
                for (var gpid in grandparents) {
                    visible_parent_map[node.id][gpid] = true;
                }
            }
        }
    }
    
    var explore_children = function(node) {
        if (visible_child_map[node.id]) {
            return;
        }
        visible_child_map[node.id] = {};
        nodes[node.id] = node;
        var children = node.child_nodes;
        for (var cid in children) {
            var child = children[cid];
            if (child.visible()) {
                visible_child_map[node.id][cid] = true;
                explore_children(child);
            } else {
                explore_children(child);
                var grandchildren = visible_child_map[cid];
                for (var gcid in grandchildren) {
                    visible_child_map[node.id][gcid] = true;
                }
            }
        }
    }
    
    explore_parents(center);
    explore_children(center);
    
    return values(nodes);
}

function getEntirePathLinks(center) {
    // Returns a list containing all edges leading into or from the center node
    // The same fucking code to get the nodes from a center node, Change this code!? 
    var visible_parent_map = {};
    var visible_child_map = {};
    var nodes = {};
    
    var explore_parents = function(node) {
        if (visible_parent_map[node.id]) {
            return;
        }
        visible_parent_map[node.id] = {};
        nodes[node.id] = node;
        var parents = node.parent_nodes;
        for (var pid in parents) {
            var parent = parents[pid];
            if (parent.visible()) {
                visible_parent_map[node.id][pid] = true;
                explore_parents(parent);
            } else {
                explore_parents(parent);
                var grandparents = visible_parent_map[pid];
                for (var gpid in grandparents) {
                    visible_parent_map[node.id][gpid] = true;
                }
            }
        }
    }
    
    var explore_children = function(node) {
        if (visible_child_map[node.id]) {
            return;
        }
        visible_child_map[node.id] = {};
        nodes[node.id] = node;
        var children = node.child_nodes;
        for (var cid in children) {
            var child = children[cid];
            if (child.visible()) {
                visible_child_map[node.id][cid] = true;
                explore_children(child);
            } else {
                explore_children(child);
                var grandchildren = visible_child_map[cid];
                for (var gcid in grandchildren) {
                    visible_child_map[node.id][gcid] = true;
                }
            }
        }
    }
    
    explore_parents(center);
    explore_children(center);
    
    var path = [];

    for (var targetid in visible_parent_map) {
        var target = nodes[targetid];
        var sourceids = visible_parent_map[targetid];
        for (var sourceid in sourceids) {
            var source = nodes[sourceid];
            path.push({source: source, target: target});
        }
    }
    
    for (var sourceid in visible_child_map) {
        var source = nodes[sourceid];
        var targetids = visible_child_map[sourceid];
        for (var targetid in targetids) {
            var target = nodes[targetid];
            path.push({source: source, target: target});
        }
    }

    return path;
}

function values(obj) {
    return Object.keys(obj).map(function(key) { return obj[key]; });
}

function flatten(arrays) {
    var flattened = [];
    return flattened.concat.apply(flattened, arrays);
}
package Graph;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


import ETP.*;



public class Graph {
	
	private static Map<String, Node> adjacencyList; //exam id - node 
	private List<Edge> edges;
	
	public Graph() {
		edges = new ArrayList<>();
        adjacencyList = new TreeMap<>();
	}
	
	
	
	public void addEdge(Exam vertex1, Exam vertex2) { 
		
			if (!containsVertex(vertex1.getId()))  
	        	addVertex(vertex1);  
	    
	        if(!containsVertex(vertex2.getId()))
	        	addVertex(vertex2);  
	        
	        Node node1 = adjacencyList.get(vertex1.getId());  
	        Node node2 = adjacencyList.get(vertex2.getId());  
	        adjacencyList.remove(vertex1.getId()); 
	        adjacencyList.remove(vertex2.getId()); 
	        
	        //check if the edge already exist
	        //unidirectional graph. insert same edge into the 2 nodes 
	        Edge e;
	        if(!node1.hasEdge(node2)) {     
	        		//then i create the edges 
	        		e = new Edge(node1,node2,1); //weight starts from 1
	        		//same edge object e is added to the 2 nodes
	        		node1.addEdge(e);   
	        		node2.addEdge(e); 
	        		edges.add(e);
	        		node1.getVertex().addEdge(e);
	        		node2.getVertex().addEdge(e);
	        } else { 						
	        		//if the edge already exist
	        		//update the weight of the edge
	        		for(Edge i: edges) {
	        			if(i.isBetween(node1, node2)||i.isBetween(node2, node1)) {
	        				i.addWeight();
	        				break;
	        			}
	        		}
	        }
	        adjacencyList.put(vertex1.getId(), node1);   
	        adjacencyList.put(vertex2.getId(), node2);   
	}
	
	public List<Edge> getAllEdges(){
		return edges;
	}
	
	public int getNumNodes() {
		return adjacencyList.size();
	}
	
	public boolean containsVertex(String vertex) {
        return adjacencyList.containsKey(vertex);  
    }
	 
	public void addVertex(Exam vertex) {
		Node node1 = new Node(vertex);  
		adjacencyList.put(vertex.getId(), node1); 
    }
	
	//return the adjacency list for the Exam e
	public List<Edge> getNodeEdges(String examId){
		Node n = adjacencyList.get(examId);
		return n.getEdges();
	}
	
	
	public void print() {
		
		Iterator<Node> it = adjacencyList.values().iterator();
		while(it.hasNext()) {
			Node n = it.next(); 
			System.out.println("Exam: " + n.getVertex().getId());
			List<Edge> listEdge = n.getEdges();
			for (Edge e : listEdge) {
			System.out.println(e.fromNode().getVertex().getId() + " ---> " + e.toNode().getVertex().getId() + ", weight:" + e.getWeight());
			}
		}
		
	}
	
	public Node getNode(String nodeName) {
		return adjacencyList.get(nodeName); 
	}
	
	/**
	 * 
	 * @param exam1 the first exam
	 * @param exam2 the second exam
	 * @return true if e1 and e2 are connected
	 */
	
	public Boolean checkEdge(String exam1, String exam2) {
		Node node1 = getNode(exam1); 
		Node node2 = getNode(exam2); 
		return node1.hasEdge(node2); 
	}
	
	/**
	 * 
	 * @param exam1 the first exam
	 * @param exam2 the second exam
	 * @return the weight of the connecting edge (0 if it does not exists)
	 */
	public int getEdgeWeight(String exam1, String exam2) {
		Node node1 = getNode(exam1); 
		Node node2 = getNode(exam2); 
		return node1.getEdgeSize(node2); 
	}



	public static int getNodeSize(String id) {
		
		int num = adjacencyList.get(id).getEdges().size(); 
		return num; 
		
	}
	
	public Edge getEdge(String exam1, String exam2) {
		Node node1 = getNode(exam1);
		Node node2 = getNode(exam2);
		return node1.findEdge(node2);
	}
	
	public String toString() {
		String s="";
		for(Edge e: edges) {
			s+=e.toString()+" w: "+e.getWeight()+"\n";
		}
		return s;
	}



	public double getDensity() {
		int n = adjacencyList.size();
		return 2*(double)edges.size()/(n*(n-1));
	}

	
}
package Graph;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import ETP.Exam;

public class Node {
	

	    private Exam vertex;  

	    private List<Edge> edges;  


	    public Node(Exam vertex) {
	    	
	        this.vertex = vertex;
	        this.edges = new ArrayList<>();
	        
	    }

	    public Exam getVertex() {
	    	
	        return vertex;
	    }
	    
	    public List<Edge> getEdges() {
	    	
	        return edges;
	    }

	    public boolean addEdge(Edge e) {
	        return edges.add(e);
	    }

	    public boolean updateEdge(Node node2) { 
	    		Edge edge = this.findEdge(node2); 
	    		edges.remove(edge);
	    		int weight = edge.getWeight(); 
	    		weight++; 
	        Edge newEdge = new Edge(this, node2, weight); 
	        return edges.add(newEdge); 
	        
	    }
	    
	    public boolean hasEdge(Node node) throws NullPointerException { 
	        return edges.stream()
	        				.filter(edge -> edge.isBetween(this, node))
	        				.findFirst()
	        				.isPresent()
	        				;
	    }

	    public Edge findEdge(Node node) throws NoSuchElementException, NullPointerException { 
	        return edges.stream()
	                .filter(edge -> edge.isBetween(this, node))
	                .findFirst()
	                .get() 
	                ;
	         
	    }
	    
	    public int getEdgeCount() {
	        return edges.size();
	    }

	    public int getEdgeSize(Node node) throws NullPointerException { 
	        return edges.stream()
	        				.filter(edge -> edge.isBetween(this, node))
	        				.findFirst()
	        				.get()
	        				.getWeight()
	        				;
	    }

	
}
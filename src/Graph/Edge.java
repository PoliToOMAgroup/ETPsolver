package Graph;

public class Edge{ 
	
    private Node node1;
    private Node node2;
    private int weight;

    public Edge(Node node1, Node node2, int weight) { 
        this.node1 = node1;
        this.node2 = node2;
        this.weight = weight;
    }
    
    
    public int getWeight() {
    	
		return weight;
	}
    
    public void addWeight() {
    		weight++;
    }

	public Node fromNode() {
		
        return node1;
        
    }

    public Node toNode() {
        return node2;
    }
    
    /**
     * 
     * @param node1 the first node
     * @param node2 the second node
     * @return true if exists edge between node1 and node2, false otherwise
     */
    public boolean isBetween(Node node1, Node node2) { 
        return (this.node1 == node1 && this.node2 == node2 || this.node1 == node2 && this.node2 == node1);
    }
    
    public String toString() {
    	return fromNode().getVertex().getId() + " - " +toNode().getVertex().getId();
    }
}

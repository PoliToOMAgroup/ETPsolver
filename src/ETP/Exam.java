package ETP;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.*;
import Graph.*;

public class Exam {

	private String id;
	private List<Student> students; //used at initialization phase 
	private Map<String,Exam> conflictingExams; //list of exams clashing with this exam
	private List<Edge> conflictingArcs; //list of arcs where this exam appear
	
	//properties of the exam itself.
	public int cg; 
	public int hc; 
	public int sd;
	//note that highest cost and saturation degree are not a property of just the exam but instead it depends on which solution we are considering
	//in fact they are continuously rewritten by methods inside solution
	//but they're needed to write compareTo correctly
	
	public Exam(String id, int num) {
		this.setId(id);
		this.hc = -1;
		this.sd = -1;
		this.cg = -1;
		this.students=new ArrayList<>();
		this.conflictingExams=new HashMap<>();
		this.conflictingArcs=new ArrayList<>();
	}
	
	
	@Override
	public boolean equals(Object o) {
		
		if(o instanceof Exam) {
			if(this.id.equals(((Exam) o).getId()))
				return true;
		}
		
		return false;
	}
	
	@Override
	public int hashCode() {
		return this.id.hashCode();
	}
	
	
    public static Comparator<Exam> compareByHeuristic() {
        return new Comparator<Exam>() {
			@Override
			public int compare(Exam e1, Exam e2){
				//priorities: highest cost first, saturation degree after
				///CASE 1: EQUALITY OF SATURATION DEGREE
				if(e1.sd == e2.sd && e1.hc != e2.hc) {
					//then the exam with highest cost is scheduled first
					if(e1.hc > e2.hc)
						return -1;
					else
						return 1;
				
				///CASE 2: EQUALITY OF HIGHEST COST
				} else if(e1.hc==e2.hc && e1.sd != e2.sd) {
					//then the exam with the highest saturation degree will be scheduled first
					if(e1.sd > e2.sd)
						return 1;
					else 
						return -1;
				
				//for other cases in which either hc and sd are different each other,
				// consider first highest cost and then saturation degree
				} else if(e1.sd < e2.sd && e1.hc > e2.hc) {
					return -1; 
				} else if(e1.sd > e2.sd && e1.hc < e2.hc) {
					return 1; 
				} else if(e1.sd < e2.sd && e1.hc < e2.hc) {
					return -1;
				} else if(e1.sd > e2.sd && e1.hc > e2.hc) {
					return 1;
				} else {
					//IF this.sd==e2.sd && this.hc == e2.sd THEN consider another approach
					// the conflict grade parameter approach: first goes the exam with more conflicts
					if(e1.cg > e2.cg)
						return -1;
					else if(e1.cg < e2.cg)
						return 1;
				}
		        return 0; //either examination can be scheduled first
		    }
        };
    }
    
    public static Comparator<Exam> compareByName() { 
        return new Comparator<Exam>() {
       
			@Override
			public int compare(Exam o1, Exam o2) {
				return Integer.valueOf(o1.getId()).compareTo(Integer.valueOf(o2.getId()));
			}
            // compare using attribute 2
        };
    }
	
	public List<Exam> getConflictingExams(){
		return this.conflictingExams.values().stream().collect(Collectors.toList());
	}
	
	public void setConflictingExams(List<Exam> exams) {
		for(Exam e: exams) {
			this.conflictingExams.put(e.getId(), e);
		}
	}
	
	public String getId() {
		return id;
	}

	private void setId(String id) {
		this.id = id;	
	}
	
	public void enroll(Student std) {
		this.students.add(std);
	}
	
	public void addEdge(Edge e) {
		this.conflictingArcs.add(e);
	}
	
	/**
	 * Computes saturation degree of the exam for the current given (incomplete)solution
	 * The number of remaining periods that an exam can be allocated to without causing a clash, i.e. the
	 * number of feasible periods
	 * 
	 * @param solution the solution
	 * @param conflictGraph the graph of conflicts
	 * @param numSlots the number of slots
	 * @return the saturion degree for this exam
	 */
	
	public int computeSD(Map<Exam,Integer> solution,Graph conflictGraph, int numSlots) {
		int counter=0,t; //counts unfeasible slots
		Map<Integer,Boolean> timeslotAlreadyCounted = new HashMap<>();
		for(Exam e: this.conflictingExams.values()) {
			if(solution.containsKey(e)) { //means that the one timeslot containing e is not feasible
				//but we need to count that timeslot only one time!
				t=solution.get(e);
				if(!timeslotAlreadyCounted.containsKey(solution.get(e))){
					counter++;
					timeslotAlreadyCounted.put(t, true);
				}
			}
		}
		return numSlots - counter;
	}
	
	public void updateSD(Map<Integer,Set<Exam>> complex_solution, Exam lastAdded, int new_timeslot) {
		Set<Exam> examsInT;
		boolean notCountedYet;

		if(this.conflictingExams.containsKey(lastAdded.getId())) {
			examsInT = complex_solution.get(new_timeslot);
			//if there is a new conflict in the solution then we decrease the counter ONLY if we don't already counted that timeslot
			notCountedYet=true;
			//if we insert the new conflicting exam e in a timeslot with no other conflict with e then sd--
			for(Exam e: examsInT) {
				if(e.getId() != lastAdded.getId()) //important! e != lastAdded
					if(this.conflictingExams.containsKey(e.getId())) {
						notCountedYet=false;
						break;
					}
			}
			if(notCountedYet)
				this.sd--;
			
		}
	}
	
	
	/**
	 * Computes the soft constraint cost of scheduling an examination given 
	 * the current state of the timetable with complexity O(tmax*|E|).
	 * 
	 * @param solution the solution
	 * @param conflictGraph the graph of conflicts
	 * @param numSlots the number of slots
	 * @return the scheduling cost of that exam
	 * 
	 * 
	 */
	public int computeHC(Map<Exam,Integer> solution, Graph conflictGraph, int numSlots) {
		int highestcost=0;
		int d,t;
		
		for(Exam e2: this.conflictingExams.values()) { //for each exam(assigned index: e2) connected to e1 
			if(solution.containsKey(e2)) { //if e2 is already in the current timetable
				for(t=0; t<numSlots; t++) { //for each timeslot
					d=Math.abs(t-solution.get(e2)); //compute distance between current timeslot and timeslot of e2
					//if distance is between 0 and 5 -> apply penalty
					highestcost+=ETPHandler.getPenalty(d)*conflictGraph.getEdgeWeight(this.id, e2.getId());
				}
			}
		}
		
		return highestcost;
	}
	
	public void updateHC(Map<Exam,Integer> solution, Graph conflictGraph, int numSlots,Exam lastAdded) {
		int highestcost=0;
		int d,t;
		Exam e2=lastAdded;
		if(this.conflictingExams.containsKey(e2.getId())) { //if e2 is already in the current timetable
			for(t=0; t<numSlots; t++) { //for each timeslot
				d=Math.abs(t-solution.get(e2)); //compute distance between current timeslot and timeslot of e2
				//if distance is between 0 and 5 -> apply penalty
				highestcost+=ETPHandler.getPenalty(d)*conflictGraph.getEdgeWeight(this.id, e2.getId());
			}
		}
		
		this.hc+=highestcost;
	}

}

package Solver;

import java.util.*;
import java.util.stream.Collectors;

import ETP.*;
import Graph.*;

public class Solution implements Comparable<Solution>{
	
	/**
	 * The class provides a framework for the solution properties
	 * and methods
	 */
	
	private static ETPHandler data; //common data to all solutions
	
	//the solution itself
	private Map<Exam,Integer> mapped_sol; // examID - timeslot_num 
	private Map<Integer,Set<Exam>> complex_sol; //timeslot_num - list of exams in that timeslot
	
	//properties of the solution itself
	private Double rank; 
	private int feasible; //if 0 -> feasible, otherwise it represents the # of couples of conflicting exams
	private Map<Exam,Set<Exam>> examsClashing; //list of conflicting exams to be fixed
	
	//properties for self-adaptation
	private double saMutation; //percentage of exams to be changed
	private int saLocation; //time slot for mutation
	private double saLearningRate; //learning rate
	private LinkedList<Double> saRanks; //learning rate
	private boolean shakeStatic; //shake solution because we are stacked
	
	//data structures for sorting or ranking computations
	private boolean erasable; //used for sorting
	private Map<Exam,Double> exam_weight; //used to know which exam is less well-placed in his timeslot

	private Solution fatherSolution;

	public Solution(ETPHandler givenData) {
		
		data=givenData;
		this.erasable=false;
		//map initialization
		this.mapped_sol = new HashMap<>();
		this.exam_weight=new HashMap<>();
		this.examsClashing=new HashMap<>();

		for(Exam e: data.getExamsList()) 
			exam_weight.put(e, 0.0);
	
		//map of lists initialization
		this.complex_sol = new HashMap<>();
		for(int i=0;i<data.getNumSlots();i++) {
			Set<Exam> l = new HashSet<>();
			complex_sol.put(i, l);
		}
		
		//conflict list of this solution
		this.rank= -1.00; //initialization of rank
		this.feasible=Integer.MAX_VALUE; //initialize number of conflicts

		//self-adaptation
		selfLearningRate(true);
		selfMutation();
		this.saRanks = new LinkedList<Double>();
	}
	
	// 		TABU
	/**
	 * This is the constructor to build a new solution by copy data from another one
	 * 
	 * @param other the solution from which to copy the data
	 */
	public Solution(Solution other) {
		this.fatherSolution = other.getFatherSol();
		this.mapped_sol = new HashMap<>();
		this.complex_sol = new HashMap<>();
		for(int i=0;i<data.getNumSlots();i++) {
			complex_sol.put(i, new HashSet<>());
		}
		this.copySolutionFromMap(other.getMappedSol());
		this.examsClashing = this.copyClashingByValue(other);
		this.rank = other.getRank();
		this.feasible = other.getNumConflicts();
		this.exam_weight = other.getExamWeight();

		//self-adaptation
		selfLearningRate(true);
		selfMutation();
		this.saRanks = new LinkedList<Double>();
	}
	
	/**
	 * This is the constructor to built a new solution by changing position of 1 exam
	 * 
	 * @param previousSol the solution on which to move the exam
	 * @param e the exam to be moved
	 * @param tOld the current timeslot for that exam
	 * @param tNew the new timeslot for that exam
	 */
	public Solution(Solution previousSol, Exam e, Integer tOld, Integer tNew) {
		
		this.fatherSolution = previousSol;
		this.mapped_sol = new HashMap<>();
		this.complex_sol = new HashMap<>();
		for(int i=0;i<data.getNumSlots();i++) {
			complex_sol.put(i, new HashSet<>());
		}
		
		//initialization for updateProperties use
		this.rank = this.fatherSolution.getRank();
		this.feasible = this.fatherSolution.getNumConflicts();
		this.examsClashing = copyClashingByValue(this.fatherSolution);

		//tabu does not use gene weight structure
		this.exam_weight = this.fatherSolution.getExamWeight();
		
		this.byChange(e, tOld, tNew);
		
		//self-adaptation
		selfLearningRate(true);
		selfMutation();
		this.saRanks = new LinkedList<Double>();
	}
	
	/**
	 * Constructor to built solution made by swapping 2 exams from another one
	 * (e1 in t1 and e2 in t2)
	 * 
	 * @param previousSol the solution on which to swap two exams
	 * @param e1 the first exam to be moved
	 * @param t1 the current position for e1
	 * @param e2 the second exam to be moved
	 * @param t2 the current position for e2
	 */ 
	public Solution(Solution previousSol, Exam e1, Integer t1, Exam e2, Integer t2) {

		this.fatherSolution = previousSol;
		this.mapped_sol = new HashMap<>();
		this.complex_sol = new HashMap<>();
		for(int i=0;i<data.getNumSlots();i++) {
			complex_sol.put(i, new HashSet<>());
		}
		
		//initialization for updateProperties use
		this.rank = this.fatherSolution.getRank();
		this.feasible = this.fatherSolution.getNumConflicts();
		this.examsClashing = copyClashingByValue(this.fatherSolution);

		//tabu does not use gene weight structure
		this.exam_weight = this.fatherSolution.getExamWeight();
		
		this.bySwap(e1, t1, e2, t2);

		//self-adaptation
		selfLearningRate(true);
		selfMutation();
		this.saRanks = new LinkedList<Double>();
	}
	
	// solution built by swap
	private void bySwap(Exam e1, Integer t1, Exam e2, Integer t2) {
		this.copySolutionFromMap(this.fatherSolution.getMappedSol()); //by value!
		
		// t2 is the old pos of e1
		this.complex_sol.get(t2).remove(e1);
		this.complex_sol.get(t1).add(e1);
		this.mapped_sol.put(e1, t1);
		
		this.updateProperties(e1, t2, t1);
		
		this.complex_sol.get(t2).add(e2);
		this.complex_sol.get(t1).remove(e2);
		this.mapped_sol.put(e2, t2);
		
		this.updateProperties(e2, t1, t2);
			
	}
	
	//solution built by changing
	private void byChange(Exam e, Integer tOld, Integer tNew) {
		this.copySolutionFromMap(this.fatherSolution.getMappedSol()); //by value!
		
		this.complex_sol.get(tOld).remove(e);
		this.complex_sol.get(tNew).add(e);
		this.mapped_sol.put(e, tNew);
		
		this.updateProperties(e, tOld, tNew);
			
	}
	
	private Map<Exam,Set<Exam>> copyClashingByValue(Solution sol){
		Map<Exam,Set<Exam>> tmp = new HashMap<>();
		for(Exam ei : sol.getExamsClashing().keySet()) {
			if(!tmp.containsKey(ei))
				tmp.put(ei,  new HashSet<>());
			
			for(Exam ej : sol.getExamsClashing().get(ei))
				tmp.get(ei).add(ej);
			
		}
		return tmp;	
	}
	

	
	
//******** SELF-ADAPTATION *********************//
//***********************************************//
	
	/**
	 * Method for setting the shake parameter to true.
	 * The method is called when the solution does not improve
	 * for some iterations.
	 */
	public void setShakeStatic() {
		shakeStatic=true;
	}
	
	/**
	 * Method for the update of the learning rate
	 * @param shake boolean variable that determines if the learning rate needs to be reset
	 * @return saLearningRate the up-to-date learning rate of the solution 
	 */
	public double selfLearningRate(boolean shake) {
		if(shake) {
			this.saLearningRate = -Math.random()*5-3;
			if(shakeStatic) this.saLearningRate = -Math.random()*5-0.5;
		} else {
			this.saLearningRate -= 0.05;
		}
		return this.saLearningRate;
	}
	
	/**
	 * Method for the updating of the mutation rate
	 * @return saMutation the amount (%) of exams to move (mutate)
	 */
	public double selfMutation() {
		this.saMutation = Math.exp(this.saLearningRate);
		return this.saMutation;
	}
	
	/**
	 * Method for the copy of the rank of a parent solution
	 * @param parent the parent solution
	 */
	public void selfCopyRanks(Solution parent) {
		for(Double d: parent.saRanks) {
			this.saRanks.add(d);
		}
	}
	
	/**
	 * Method for the update of the list of the last 3 ranks that
	 * the current solution has in the previous 3 iterations 
	 */
	public void selfUpdateRanks() {
		if(this.saRanks.size()<3) {
			this.saRanks.addFirst((Double)this.rank);		
			return;
		}
		this.saRanks.pollLast();
		
		double avg = 0;
		for(int i=0; i<this.saRanks.size()-1; i++) {
			double r1 = this.saRanks.get(i).doubleValue();
			double r2 = this.saRanks.get(i+1).doubleValue();
			avg += (r2-r1)/r2;
		}
		
		avg = avg/(this.saRanks.size() - 1);
		if(avg < 0.1) {
			selfLearningRate(true);
		}
	}
	
	/**
	 * Method to retrieve the list of the last ranks of the solution
	 * @return the list of the last ranks
	 */
	public LinkedList<Double> getSaRanks() {
		return this.saRanks;
	}
	
	/**
	 * Method used for determining the mutation position (timeslot)
	 * @param saLocation position of the mutating timeslot
	 * @return saLocation the timeslot position
	 */
	public int selfLocation(int saLocation) {
		this.saLocation = saLocation;
		return this.saLocation;
	}	
	
	
	

//******** SOLUTION MANIPULATION OPERATORS ***********//
//***********************************************//
	

	public void copySolutionFromMap(Map<Exam, Integer> mapped_sol_f) {
		
		this.mapped_sol = new HashMap<>();
		this.complex_sol = new HashMap<>();
		for(int i=0;i<data.getNumSlots();i++) {
			complex_sol.put(i, new HashSet<>());
		}
		
		for(Exam e: mapped_sol_f.keySet()) {
			
			Integer slot = mapped_sol_f.get(e); 
			this.mapped_sol.put(e, slot); //copy to mapped
			Set<Exam> examSet = this.complex_sol.get(slot); 
			examSet.add(e);
			
		}
		
	}
	
	public void copySolution(Solution sol) {
		
		//copy mappedsol and complexsol
		this.mapped_sol = new HashMap<>();
		this.complex_sol = new HashMap<>();
		for(int i=0;i<data.getNumSlots();i++) 
			complex_sol.put(i, new HashSet<>());
		
	
		for(Integer slot : sol.getComplexSol().keySet()) {
			Set<Exam> tmpSet = new HashSet<>();
			
			for(Exam exam : sol.getComplexSol().get(slot)) {
				tmpSet.add(exam); 
				this.mapped_sol.put(exam, slot);
			}
			
			this.complex_sol.put(slot, tmpSet);	
		}
		
		//copy feasible and rank 
		this.feasible = sol.getNumConflicts(); 
		this.rank = sol.getRank(); 

		
		//copy examClashing
		this.examsClashing.clear();
		for(Exam examKey : sol.getExamsClashing().keySet()) {
			Set<Exam> examSet = sol.getExamsClashing().get(examKey);
			Set<Exam> copySet = new HashSet<>();
			for(Exam e : examSet) {
				copySet.add(e); 
			}
			this.examsClashing.put(examKey, copySet); 
		}
		
		//copy examWeight 
		this.exam_weight.clear();
		for(Exam examKey : sol.getExamWeight().keySet()) {
			Double num = sol.getExamWeight().get(examKey); 
			this.exam_weight.put(examKey, num); 
		}
		
	}


	public void setExam(Exam e, Integer value) {
		mapped_sol.put(e, value);
		complex_sol.get(value).add(e);
	}
	
	public void moveExam(Exam e, Integer old_t, Integer new_t) {
	
		this.mapped_sol.remove(e, old_t);
		this.mapped_sol.put(e, new_t);
		this.complex_sol.get(old_t).remove(e);
		this.complex_sol.get(new_t).add(e);
		
		this.updateProperties(e, old_t, new_t);

	} 

	public void randomize() {
		Random randomValue=new Random();
		for(Exam e: data.getExamsList()) {
			this.setExam(e, randomValue.nextInt(data.getNumSlots()));
		}
		this.initializeProperties();
	}
	

//******** QUALITY CHECKERS AND PREDICTORS *********************//
//***********************************************//

	
	//RANK METHODS

	/**
	 * When a new solution is created call this method to initialize
	 * the 4 properties of the solution:
	 * feasible, rank, examsClashing, structured_rank
	 */
		
	public void initializeProperties() {
		int t1,t2,d;
		Exam e1,e2;
		double rank=0.0;
		int temp;
		Set<Exam> tempE;
		
		this.feasible=0;
		this.examsClashing.clear();
		
		for(Edge a: data.getConflictsList()) {
			e1=a.fromNode().getVertex();
			e2=a.toNode().getVertex();
			t1=mapped_sol.get(e1);
			t2=mapped_sol.get(e2);
			d=Math.abs(t1-t2);
			
			//compute rank and fill structured_rank
			if(d<=5) { //if distance between the 2 conflicting exams is less or equal to 5 then we apply penalization
				temp=ETPHandler.getPenalty(d)*a.getWeight();
				rank+=temp;
				exam_weight.put(e1, exam_weight.get(e1)+temp);
				exam_weight.put(e2, exam_weight.get(e2)+temp);
			}
			
			//compute feasible and fill examsClashing
			if(d==0) {
				this.feasible++;
				//update conflicts for exam1
				if(examsClashing.containsKey(e1))
					tempE=examsClashing.get(e1);
				else
					tempE = new HashSet<>();
				
				tempE.add(e2);
				this.examsClashing.put(e1, tempE);
				
				//update conflicts for exam2
				if(examsClashing.containsKey(e2))
					tempE=examsClashing.get(e2);
				else 
					tempE = new HashSet<>();
				
				tempE.add(e1);
				this.examsClashing.put(e2, tempE);
			}
											
		}
		
		this.rank=(double)rank/data.getStudents().size();
	}

     /**
	 * After changing the position of one exam call this method to update
	 * the 4 properties of the solution:
	 * feasible, rank, examsClashing, structured_rank
	 * @param examMoved the moved exam
	 * @param old_t the previous timeslot of the moved exam
	 * @param new_t the current timeslot of the moved exam
	 */
	
	public void updateProperties(Exam examMoved, int old_t, int new_t) {
		
		int t2,old_d,new_d,edge_weight,temp;
		Exam e1,e2;
		Set<Exam> clashSet1, clashSet2;
		
		e1=examMoved;
		
		long temp_rank = Math.round(this.rank * data.getStudents().size());
		
		for(Edge changeEdge: data.getGraph().getNodeEdges(examMoved.getId())) {
			
			if(e1==changeEdge.toNode().getVertex()) {
				e2=changeEdge.fromNode().getVertex();
			}else {
				e2=changeEdge.toNode().getVertex();
			}
			
			t2=mapped_sol.get(e2);
			old_d=Math.abs(old_t-t2);
			new_d=Math.abs(new_t-t2);
			
			//update rank and exam_weight
			if(new_d!=old_d) {
				edge_weight=changeEdge.getWeight();
				temp = (ETPHandler.getPenalty(new_d)-ETPHandler.getPenalty(old_d))*edge_weight;
				temp_rank += temp;
				exam_weight.put(e1, exam_weight.get(e1)+temp);
				exam_weight.put(e2, exam_weight.get(e2)+temp);
			}
			
			//update of feasible and examsClashing
			if(new_d==0 && old_d!=0) {//add e1 and e2 in examsClashing
				this.feasible++;
				
				if(!this.examsClashing.containsKey(e1)) {				
					clashSet1 = new HashSet<>();
				}else {
					clashSet1 = this.examsClashing.get(e1);
				}
				clashSet1.add(e2);
				this.examsClashing.put(e1, clashSet1);
				
				if(!this.examsClashing.containsKey(e2)) {			
					clashSet2 = new HashSet<>();
				}else {
					clashSet2 = this.examsClashing.get(e2);
				}
				clashSet2.add(e1);
				this.examsClashing.put(e2, clashSet2);					
			}		

			if(new_d!=0 && old_d==0) { //remove e1 and e2 from examsClashing
				this.feasible--;
				
				clashSet1 = this.examsClashing.get(e1);
				clashSet1.remove(e2);
				if(clashSet1.isEmpty()) this.examsClashing.remove(e1);
				
				clashSet2 = this.examsClashing.get(e2);
				clashSet2.remove(e1);
				if(clashSet2.isEmpty()) this.examsClashing.remove(e2);
			}			
						
		}
		
		this.rank=(double)temp_rank/((double)data.getStudents().size());
	}
	
	
	/**
	 * Given one exam and a feasible time-slot, this method computes 
	 * the penalty given by inserting the exam in that time-slot
	 * @param e1 exam to be inserted
	 * @param t feasible time-slot
	 * @return associated cost
	 */
	
	private double predictExamCost(Exam e1,int t) {
		double cost=0.0;
		int d;
		
		for(Exam e2: e1.getConflictingExams()) {
			if(mapped_sol.containsKey(e2)) {
				d=Math.abs(t-mapped_sol.get(e2));
				cost+=ETPHandler.getPenalty(d)*data.getGraph().getEdgeWeight(e1.getId(), e2.getId());
			}
		}
		return cost;
	}
	
	/**
	 * Given one exam and one time-slot, this method checks if the insertion
	 * of the exam in the time-slot generates a feasible solution
	 * @param id1 ID of the exam
	 * @param new_timeslot possible time-slot
	 * @return feasibility
	 */
	
	public boolean isSlotFeasible(String id1, int new_timeslot) {
		Set<Exam> examsInTimeslot=complex_sol.get(new_timeslot);
		
		Exam e1 = data.getExam(id1);
		for(Exam e2 : e1.getConflictingExams()) {
			if(examsInTimeslot.contains(e2)) {
				return false;
			}
		}
		return true;
	}
	
	
//******** SOLUTION FIXER *********************//
//***********************************************//

	/**
	 *	remove all the conflicting exams
	 *	sort the exams
	 *	while (list is not empty)
	 *		pop the first exam
	 *		sort timeslots 
	 *		schedule to the first timeslot 
	 *		update cg, hc, sd
	 *		sort the remaining exams			
	 */
	public void fix() {
		List<Exam> clashingExams=this.examsClashing.keySet().stream().collect(Collectors.toList());
		
		Map<Integer,Double> timeslot_ranking = new HashMap<>();
		Map<Exam, Integer> backup_sol = new HashMap<>();
		int temp_t;
		Exam temp_id;
		
		for(Exam e: clashingExams) { 
			temp_t=this.mapped_sol.get(e);
			temp_id = e;
			backup_sol.put(temp_id, temp_t);
			this.mapped_sol.remove(temp_id);
			this.complex_sol.get(temp_t).remove(temp_id);
		}
		
		for(Exam e: clashingExams) {
			e.hc=e.computeHC(this.mapped_sol, data.getGraph(),data.getNumSlots());
			e.sd=e.computeSD(this.mapped_sol, data.getGraph(),data.getNumSlots());
		}
		
		Collections.sort(clashingExams, Exam.compareByHeuristic()); //key-point of this algorithm

		Exam temp;
		while(!clashingExams.isEmpty()) {
			temp=clashingExams.get(0);
			
			for(int t=0; t<data.getNumSlots(); t++) {
				if(!this.isSlotFeasible(temp.getId(), t)) {
					timeslot_ranking.put(t, Double.MAX_VALUE);
				} else {
					timeslot_ranking.put(t,this.predictExamCost(temp, t));
				}
			}
			
			timeslot_ranking=sortByValue(timeslot_ranking);
			Map.Entry<Integer,Double> entry = timeslot_ranking.entrySet().iterator().next();
			Integer key = entry.getKey();

			this.setExam(temp, key);
			//this.moveExam(temp, backup_sol.get(temp), key);
	
			//order the remaining exams
			clashingExams.remove(0);
			for(Exam e: clashingExams) {
				e.updateHC(this.mapped_sol, data.getGraph(),data.getNumSlots(), temp);
				e.updateSD(complex_sol, temp, key);
			}
			
			Collections.sort(clashingExams, Exam.compareByHeuristic()); //key-point of this algorithm

		}
		this.initializeProperties();
	}

	
	

	private static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
	    return map.entrySet()
	              .stream()
	              .sorted(Map.Entry.comparingByValue(/*Collections.reverseOrder()*/))
	              .collect(Collectors.toMap(
	                Map.Entry::getKey, 
	                Map.Entry::getValue, 
	                (e1, e2) -> e1, 
	                LinkedHashMap::new
	              ));
	}
//************* COMPARATOR *****************//
	
	
	@Override
	public int compareTo(Solution i2) {
		double thisrank = Math.round( this.rank * 100000.0 ) / 100000.0;
		double i2rank = Math.round( i2.getRank() * 100000.0 ) / 100000.0;
		
		if(this.erasable != i2.isErasable()) {
			if(this.erasable==true && i2.isErasable()==false) {
				return 1; //scale down
			} else if(this.erasable==false && i2.isErasable()==true) {
				return -1; //scale up
			}
		} else {
			if(this.feasible==i2.getNumConflicts() && thisrank == i2rank) {
				return 0;
			} else {
				if(this.feasible!=i2.getNumConflicts() && thisrank==i2rank) {
					if(this.feasible > i2.getNumConflicts()) {
						return 1;
					} else {
						return -1;
					}
				} else if(this.feasible==i2.getNumConflicts() && thisrank!=i2rank) {
					if(thisrank > i2rank) {
						return 1;
					} else {
						return -1;
					}
				} else if(this.feasible > i2.getNumConflicts() && thisrank > i2rank) {
					return 1;
				} else if(this.feasible < i2.getNumConflicts() && thisrank > i2rank) {
					return -1;
				} else if(this.feasible > i2.getNumConflicts() && thisrank < i2rank) {
					return 1;
				} else if(this.feasible < i2.getNumConflicts() && thisrank < i2rank) {
					return -1;
				}
			} 
		}
		return 0;
		
	}

	//******** GETTERS ***********//
	//***********************************************//
		
	public int getNumConflicts() {
		return feasible;
	}
	
	public Double getRank() {
		return rank;
	}
	
	public boolean isErasable() {
		return this.erasable;
	}
	
	public boolean isFeasible() {
		if(feasible==0)
			return true;
		return false;
	}
	
	public int getSlot(Exam e) {
		return mapped_sol.get(e);
	}
	
	public int getNumSlots() {
		return data.getNumSlots();
	}

	public Map<Integer, Set<Exam>> getComplexSol() {
		return this.complex_sol;
	}
	
	public Map<Exam, Integer> getMappedSol() {
		return this.mapped_sol;
	}
	
	public Map<Exam, Set<Exam>> getExamsClashing(){
		return this.examsClashing;
	}
	
	public void setErasable(boolean value) {
		this.erasable=value;
	}
		
	public Map<Exam, Double> getExamWeight() {
		return exam_weight;
	}
	
		//	TABU
	public Exam getRandomExam(Integer t) {
		Set<Exam> exSet = this.complex_sol.get(t);
		if(exSet.isEmpty()) //no exam in this timeslot
			return null;
	
		List<Exam> exams = exSet.stream().collect(Collectors.toList());
		Random num=new Random();
		return exams.get(num.nextInt(exams.size()));

	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof Solution) { //by using mapped sol instead of solution we can avoid sorting problem (Ex. [e1, e2] != [e2, e1])
			if(this.getMappedSol().equals(((Solution) obj).getMappedSol())) {
				return true;
			}		
		}
		return false;
	}
	
	public Solution getFatherSol() {
		return this.fatherSolution;
	}

	@Override
	public int hashCode() {
		return this.getRank().hashCode();
	}
	
}

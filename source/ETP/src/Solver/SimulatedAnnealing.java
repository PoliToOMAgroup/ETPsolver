package Solver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import ETP.*;


public class SimulatedAnnealing {
	
	/**
	 * This class is intended to generate a feasible solution running Simulated Annealing Algorithm 
	 * 
	 */
 
	private Configurator cf;	
	private Solution best; 
	private Solution current;
	private Solution perturbed; 
	private double bestRank;
	private double temperature;
	private ETPHandler timeTabling;
	private int time;
	
	
	/**
	 * Constructor 
	 * @param timeTabling an instance of the current timetable
	 * @param cf the configurator object
	 */
	public SimulatedAnnealing(ETPHandler timeTabling, Configurator cf) {
		this.cf = cf;
		this.temperature = cf.INITIAL_TEMPERATURE; 
		this.timeTabling = timeTabling;
		this.perturbed = new Solution(timeTabling); 
		this.current = new Solution(timeTabling); 
		this.best = new Solution(timeTabling); 
		
	}
	
	
	/**
	 * This method return the best solution found by the algorithm
	 * @return the best solution
	 */
	private Solution getBestSolution() {
		
		evaluateSolution(this.best);
		return this.best; 
		
	}

	
	/**
	 * This method generate a neighbor from a solution
	 * It selects random two timeslots.
	 * It selects the first exam in the first time slot that has no conflict in the second time slot. Then moves that exam
	 * 
	 * @param sol from which to create the neighbor
	 * @return the generated neighbor
	 */
	private Solution generateNeighborByExamMove(Solution sol) {
	
		Solution neighbor = new Solution(this.timeTabling);
		neighbor.copySolution(sol);

		Random r = new Random(); 
		int firstSlot = r.nextInt(neighbor.getComplexSol().size()); 
		int secondSlot = 0; 
		while ((secondSlot = r.nextInt(neighbor.getComplexSol().size())) == firstSlot);
		Set<Exam> examListFirst = neighbor.getComplexSol().get(firstSlot);
		Set<Exam> examListSecond = neighbor.getComplexSol().get(secondSlot); 
		Exam examFirst = null;

		if(!examListFirst.isEmpty() && !examListSecond.isEmpty()) {
			Boolean conflict = true; 
			Boolean exit = false;
			Iterator<Exam> examFirstIterator = examListFirst.iterator();
			//search the first exam in the first slot that has no conflict with all the exams in the second slot 
			while(!exit && examFirstIterator.hasNext()) {
				Exam e1 = examFirstIterator.next();
				conflict = false; 
				Iterator<Exam> examSecondIterator = examListSecond.iterator();
				while(examSecondIterator.hasNext()) {
					Exam e2 = examSecondIterator.next();
					if(this.timeTabling.getGraph().checkEdge(e1.getId(), e2.getId())) {
						conflict = true;
					}
				}
				if (!conflict) {
					examFirst = e1;
					exit = true;
				}		
			}
			if(examFirst != null) {
				neighbor.moveExam(examFirst, firstSlot, secondSlot);
				return neighbor; 
			}
			
		} else if (examListFirst.size() > 0 && examListSecond.size() < 1) {

			//select random an exam and move to second
			int randomExamNum = r.nextInt(examListFirst.size()); 
			int i = 0; 
			Iterator<Exam> examFirstIterator = examListFirst.iterator();
			Boolean exit = false; 
			while(!exit && examFirstIterator.hasNext()) {
				Exam e = examFirstIterator.next(); 
				if(i == randomExamNum) {
					examFirst = e; 
					exit = true;
				}
				i++; 
			}
			
			if(examFirst != null) {
				neighbor.moveExam(examFirst, firstSlot, secondSlot);
				return neighbor; 
			}

		} else if (examListSecond.size() > 0 && examListFirst.size() < 1) {

			//select random an exam and move to first
			int randomExamNum = r.nextInt(examListSecond.size()); 
			int i = 0; 
			Iterator<Exam> examSecondIterator = examListSecond.iterator();
			Boolean exit = false; 
			while(!exit && examSecondIterator.hasNext()) {
				Exam e = examSecondIterator.next();
				if(i==randomExamNum) {
					examFirst = e; 
					exit = true;
				}
				i++; 
			}
			
			if(examFirst!=null) {
				neighbor.moveExam(examFirst, secondSlot, firstSlot);
				return neighbor;
			}
		}

		return neighbor; 
	}
	
	
	/**
	 * This method generate a neighbor from a solution
	 * It selects random two timeslots.
	 * Find all the n-exam in the first timeslot that have no conflict in the second timeslot
	 * It moves that list of exams from the first timeslot to the second 
	 * It does the viceversa 
	 * 
	 * @param sol from which to create the neighbor
	 * @param n number of exams to be swapped
	 * @return the generated neighbor
	 */
	private Solution generateNeighborByExamListSwap(Solution sol, int n) {
		
		Solution neighbor = new Solution(this.timeTabling);
		neighbor.copySolution(sol);
		
		Random r1 = new Random(); 
		int firstSlot = r1.nextInt(neighbor.getComplexSol().size()); 
		int secondSlot=0; 
		while ((secondSlot = r1.nextInt(neighbor.getComplexSol().size())) == firstSlot);  
		Set<Exam> examListFirst = neighbor.getComplexSol().get(firstSlot); 
		Set<Exam> examListSecond = neighbor.getComplexSol().get(secondSlot); 
		List<Exam> examListToMoveFirst = new ArrayList<>(); 
		List<Exam> examListToMoveSecond = new ArrayList<>(); 
		Boolean conflict = true; 
		
		//create subset of exams to move from first timeslot to second timeslot 
		int counter = 0; 
		if(!examListFirst.isEmpty() && !examListSecond.isEmpty()) {
			counter = 0;
			for(Exam e1 : examListFirst) {
				conflict = false; 
				for(Exam e2 : examListSecond) {
					if(this.timeTabling.getGraph().checkEdge(e1.getId(), e2.getId()))
						conflict = true; 
				}
				if (!conflict && counter < n) {
					examListToMoveFirst.add(e1);
					counter ++;
				}
			}

			//create subset of exams to move from second timeslot to first timeslot
			counter = 0; 
			for(Exam e1 : examListSecond) {
				conflict = false; 
				for(Exam e2 : examListFirst) {
					if(this.timeTabling.getGraph().checkEdge(e1.getId(), e2.getId()))
						conflict = true; 
				}
				if (!conflict && counter < n) {
					examListToMoveSecond.add(e1); 
					counter++; 
				}
			}

			for(Exam e : examListToMoveFirst){
				neighbor.moveExam(e, firstSlot, secondSlot);
			}
			
			for(Exam e : examListToMoveSecond){
				neighbor.moveExam(e, secondSlot, firstSlot);
			}

		}
		
		return neighbor; 
	}
	
	
	/**
	 * This method generate a neighbor from a solution
	 * It selects randomly two timeslots
	 * It select the first exam in the first timeslot with the higher cost, that no have conflict in the second timeslot 
	 * Move that exam from the first timeslot to the second one
	 * It does the viceversa
	 *  
	 * @param sol from which to create the neighbor
	 * @param exam_weight the weight of each exam
	 * @return the neighbor
	 */
	private Solution generateNeighborByExamWeight(Solution sol, Map<Exam, Double> exam_weight) {
		
		Solution neighbor = new Solution(this.timeTabling);
		neighbor.copySolution(sol);

		Random r1 = new Random(); 
		int firstSlot = r1.nextInt(neighbor.getComplexSol().size()); 
		int secondSlot = 0; 
		while ((secondSlot = r1.nextInt(neighbor.getComplexSol().size())) == firstSlot); 
		Set<Exam> examListFirst = neighbor.getComplexSol().get(firstSlot);
		Set<Exam> examListSecond = neighbor.getComplexSol().get(secondSlot); 
		Set<Exam> examFirstToMove = new HashSet<>(); 
		Set<Exam> examSecondToMove = new HashSet<>();		
	
		if(!examListFirst.isEmpty() && !examListSecond.isEmpty()) {
		
			Boolean conflict = true;
			//search all the exams in the first slot that has no conflict with all the exams in the second slot 
			for(Exam e1 : examListFirst) {
				conflict = false; 
				for(Exam e2 : examListSecond) {
					if(this.timeTabling.getGraph().checkEdge(e1.getId(), e2.getId()))
						conflict = true; 
				}
				if (!conflict) {
					examFirstToMove.add(e1); 
				}
			}

			//search the first exam in the second slot that has no conflict with all the exams in the first slot 
			for(Exam e2 : examListSecond) {
				conflict = false; 
				for(Exam e1 : examListFirst) {
					if(this.timeTabling.getGraph().checkEdge(e2.getId(), e1.getId()))
						conflict = true; 
				}
				if (!conflict) {
					examSecondToMove.add(e2); 
				}
			}
			
			if(!examFirstToMove.isEmpty()){
				Exam maxE = null; 
				double maxValue = Double.MIN_VALUE; 
				
				for(Exam e : examFirstToMove) {
					if(exam_weight.get(e) > maxValue) {
						maxValue = exam_weight.get(e); 
						maxE = e;
					}
				}
				if(maxE != null)
					neighbor.moveExam(maxE, firstSlot, secondSlot);
			}
		
			if(!examSecondToMove.isEmpty()){
				Exam maxE = null; 
				double maxValue = Double.MIN_VALUE; 
				
				for(Exam e : examSecondToMove) {
					if(exam_weight.get(e) > maxValue) {
						maxValue = exam_weight.get(e); 
						maxE = e;
					}
				}
				if(maxE!=null)
					neighbor.moveExam(maxE, secondSlot, firstSlot);
			}
			
		} else if(!examListFirst.isEmpty() && examListSecond.isEmpty()) {
			
			Exam maxE = null; 
			double maxValue = Double.MIN_VALUE; 
			
			for(Exam e : examListFirst) {
				if(exam_weight.get(e) > maxValue) {
					maxValue = exam_weight.get(e); 
					maxE = e;
				}
			}
			if(maxE != null)
				neighbor.moveExam(maxE, firstSlot, secondSlot);
			
		} else if(examListFirst.isEmpty() && !examListSecond.isEmpty()) {
			
			Exam maxE = null; 
			double maxValue = Double.MIN_VALUE; 
			
			for(Exam e : examListSecond) {
				if(exam_weight.get(e) > maxValue) {
					maxValue = exam_weight.get(e); 
					maxE = e;
				}
			}
			if(maxE != null)
				neighbor.moveExam(maxE, secondSlot, firstSlot);
			
		}

		return neighbor; 
	}

	
	/**
	 * This method generate a neighbor from a solution
	 * It selects the timeslot with the highest rank
	 * It selects the exam in that timeslot with the highest conflict rank
	 * Move that exam in the first timeslot where it has no conflict
	 * 
	 * @param sol from which to create the neighbor
	 * @return the neighbor
	 */
	private Solution generateNeighborByExamAndSlotWeight(Solution sol) {
		
		Solution neighbor = new Solution(this.timeTabling);
		neighbor.copySolution(sol);

		List<Double> rankSlots = new ArrayList<>(); 
		List<Exam> examSlots = new ArrayList<>(); 
		for (int i = 0 ; i < this.timeTabling.getNumSlots(); i++) {
			Set<Exam> se1 = neighbor.getComplexSol().get(i);
			Double rank = 0.0; 
			int weightExam = 0;
			int maxWeight = Integer.MIN_VALUE; 
			Exam maxExam = null;
			for(Exam e1 : se1) {
				for(int j = i+1; j<timeTabling.getNumSlots() & j<i+1+5; j++) {
					Set<Exam> se2 = neighbor.getComplexSol().get(j); 
					for(Exam e2 : se2) {
						if(this.timeTabling.getGraph().checkEdge(e1.getId(), e2.getId())) {
							rank += this.timeTabling.getGraph().getEdgeWeight(e1.getId(), e2.getId()); 
							weightExam++; 
						}
					}
				}
				if(weightExam > maxWeight) {
					maxWeight = weightExam; 
					maxExam = e1; 
				}
			}
			rankSlots.add(i, rank);
			examSlots.add(i, maxExam);
		}
		
		//select the highest timeslot 
		Double maxSlotValue = Collections.max(rankSlots); 
		//return the index of that slot
		int index = rankSlots.indexOf(maxSlotValue);
		//select the exam with the highest conflict rank
		Exam examToMove = examSlots.get(index); 
		
		int slotToMove = 0; 
		Boolean exit = false;
		//select a slot where to move that exam, with no conflicts
		if(examToMove != null) {
			for(int i = 0; i<this.timeTabling.getNumSlots() && i != index && !exit; i++) {
				Set<Exam> se = neighbor.getComplexSol().get(i); 
				Boolean conflict = false;
				for(Exam e : se) {
					if(this.timeTabling.getGraph().checkEdge(e.getId(), examToMove.getId()))
						conflict = true;  
				}

				if(!conflict) {
					slotToMove = i;
					neighbor.moveExam(examToMove, index, slotToMove); 
					exit = true;
				}
			}	
		}	
		
		return neighbor; 
	}
		
	
	/**
	 * This method evaluate the given solution
	 * assign penalty given by the objective function
	 * 
	 * @param sol the solution
	 * @return the rank of the solution
	 */
	private Double evaluateSolution(Solution sol) {
	
		return sol.getRank();
	
	}

	
	/**
	 * This method run Simulated Annealing with a given feasible solution 
	 * 
	 * @param givenSolution the starting solution
	 * @return the best solution found
	 */
	private Solution runWithFeasibleSolution(Solution givenSolution) {
		
		long start = System.currentTimeMillis();
	
		int iterations = 0; 
		int plateau = this.timeTabling.getExamsMap().size() * this.timeTabling.getNumSlots() * cf.GAMMA; 
		
		if(givenSolution.isFeasible()) {
			this.best.copySolution(givenSolution);
			this.bestRank = givenSolution.getRank();
			this.current.copySolution(givenSolution);
		} else {
			return null; 
		}

		while(this.temperature > 1) {
			
			Random r = new Random();
			Double randomNum = r.nextDouble(); 
			
			if(randomNum <= cf.pELS) {
				randomNum = randomNum*10;  
				this.perturbed = generateNeighborByExamListSwap(this.current, randomNum.intValue());
			}else if (randomNum > cf.pELS && randomNum <= cf.pESW) {
				this.perturbed = generateNeighborByExamWeight(this.current, this.current.getExamWeight());
			}else if (randomNum > cf.pESW && randomNum <= cf.pEM) {
				this.perturbed = generateNeighborByExamMove(this.current);
			}else {
				this.perturbed = generateNeighborByExamAndSlotWeight(this.current);
			}
							
			Double changeRate = evaluateSolution(this.perturbed) - evaluateSolution(this.current);
			if( changeRate < 0 ){
				this.current.copySolution(this.perturbed);	
			} else if ( r.nextDouble() < Math.pow(Math.E, - (changeRate)/this.temperature)) {
				this.current.copySolution(this.perturbed);
			}
			
			long end = System.currentTimeMillis();
			if(end-start >= this.time) {
				return null; 
			}
			
			evaluateSolution(this.current); 
			if(this.current.isFeasible()) {
				Double currentRank = this.current.getRank();
				if(currentRank < this.bestRank){
					this.best.copySolution(this.current);
					this.bestRank = currentRank;
					return this.getBestSolution(); 
				}
			}
			 
			//I do this to avoid a large calculation
			if (iterations / cf.iterationParameter == 0) { 
				this.current.copySolution(this.best);
			} 
			
			int result = iterations % plateau;
			if( result == 0) {
				this.temperature = Math.pow(cf.ALPHA, iterations) * cf.INITIAL_TEMPERATURE; 
			}
			
			iterations++; 
		}
			
		return this.getBestSolution();
	}
	
	
	/**
	 * This method runs Simulated Annealing with a solution passed by the Genetic Algorithm in the fixed time
	 * 
	 * @param solutionFromGa solution passed from the Genetic algorithm
	 * @param time the max time for the run
	 * @return the best solution found
	 */
	public Solution runSAForGA(Solution solutionFromGa, int time) {
		
		this.time = time; 
		
		Solution solutionToGa = new Solution(this.timeTabling);
		
		if(solutionFromGa.isFeasible()) {
			solutionToGa = runWithFeasibleSolution(solutionFromGa); 
		}else {
			return null;
		}
		
		if(solutionToGa == null) {
			return null; 
		}
		
		return solutionToGa;
		
	}
	
	
	
	

}
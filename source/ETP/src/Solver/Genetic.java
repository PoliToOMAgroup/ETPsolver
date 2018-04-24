package Solver;

import java.util.*;
import java.util.stream.Collectors;
import ETP.*;

public class Genetic {
	private ETPHandler data;
	private List<Solution> population;
	private static Random randomValue=new Random();
	private Solution generationBest;
	private Solution globalBest; 
	private Double previousBestRank=Double.MAX_VALUE;
	private Configurator cf;
	
	private long lastTimeBest;
	private long lastTimeSinceBest;
	private int stuckTime = 30*1000;
	private boolean TSEra = false;
	
	public Genetic(ETPHandler data, Configurator cf) {
		this.data = data;
		this.population=new ArrayList<>();
		this.generationBest=null;
		this.globalBest = null; 
		this.cf = cf;
	}
	
	
	/**
	* 	population initialization
	*	fitness computation
	*	while (termination condition)
	*		parent selection
	*		crossover with probability pC
	*		mutation with probability pM
	*		Infeasible Individuals FIX
	* 		Dynamic Local Search (Simulated Annealing / Tabu Search)
	*		natural selection
	*		update best solution
	*	return best
	*
	* @return best found solution
	**/
	public Solution beginSelfEvolution() {
		
		/* SELF SETTING UP */
		int complexity;
		if(data.getExamsList().size() < data.getNumSlots())
			complexity = 10;
		else
			complexity =  (int)((Math.random()*20+100)/(data.getExamsList().size()/data.getNumSlots()));
	
		if(complexity<10) 
			complexity = 10;
		else
			if(complexity > 60)
				complexity = 60;
		
		cf.maxPopulation = 2*(int) (Math.floor((Math.random()*complexity + complexity)/2));
		cf.maxParents = 4*(int)(cf.maxPopulation/8);
		cf.maxTournamentContenders = 2*(int)(cf.maxPopulation/4);
		cf.SAthreshold = Math.random()*0.2 + 0.05;
		cf.SAcounterActivation = (int) (Math.floor(Math.random()*20 + 20));
		int selfFlag = 0; // init self-adaptive parameters
		/* end SETTING UP */
		
		/* Local search constructors */
		SimulatedAnnealing SA=new SimulatedAnnealing(data,cf);
		Tabu TS = new Tabu(cf);
		
		/* internal genetic structures & parameteres */
		List<Solution> parents, children=new ArrayList<>();

	
		System.out.println("\t Searching for solution . . . ");
		
		long timerStart=System.currentTimeMillis(),timer=0;
		int iterationCounter=1; 
		double coin;
		int counterSA = 0; //used for SA activation
		this.lastTimeBest = timerStart;
		this.lastTimeSinceBest = 0;
				
		initPopulation(); //creates random initial solutions
		updateBest();

		try {
			while(timer <= cf.maxTime) {		
				
				
				if(this.lastTimeSinceBest > this.stuckTime || this.TSEra) {
					this.changeEra();
				}


				parents=parentSelection(cf.maxParents,cf.maxTournamentContenders);
				
				/* GENETIC OPERATORS */
				coin = randomValue.nextDouble();
				if(coin<=cf.pC) { //CROSSOVER
					children.addAll(applyCrossover(parents));
				}
				
		
				coin = randomValue.nextDouble();
				if(coin<=cf.pM) { //MUTATION
					children.addAll(applySelfMutation(parents));
				}
				/* end GENETIC OPERATORS */
				
				fixPopulation(); //tries to fix infeasible solutions
//				computeFitness(); //already done by fix 

				/* update of SA counter if needed */
				if(this.generationBest != null) {
					if(Math.abs(this.generationBest.getRank()-previousBestRank)<=cf.SAthreshold) {
						counterSA++;
						selfFlag++;
					} else {
						previousBestRank = this.generationBest.getRank();
						counterSA = 0;
					}
				}
				
				population = deleteDuplicates(population);
				
				/* DYNAMIC LOCAL SEARCH APPROACH SELECTION */
				if(this.TSEra && System.currentTimeMillis() - timerStart < cf.maxTime - 55*1000)
				{
					
					population.sort((p1,p2) -> p1.compareTo(p2));
					this.localSearchTS(iterationCounter, TS, cf.TSTime); //activates Tabu only if remaining time is enough
				
				}else {
					if(counterSA>cf.SAcounterActivation) {
						population.sort((p1,p2) -> p1.compareTo(p2));
						counterSA = 0;
						localSearchSA(iterationCounter,SA);
					}
				}
					

				naturalSelection(children, children.size());
				
				if(population.size()>cf.maxPopulation) //population size mismatch
					return this.globalBest; 
				
				updateBest();	
				
				iterationCounter++;
				children.clear();
				parents.clear(); 
				
				
				// adaptive-shake
				if(selfFlag > 3) {
					selfFlag = 0;
					
					if(data.getExamsList().size() < data.getNumSlots())
						complexity = 10;
					else
						complexity =  (int)((Math.random()*20+100)/(data.getExamsList().size()/data.getNumSlots()));
					
					if(complexity < 10) 
						complexity = 10;
					else
						if(complexity > 60)
							complexity = 60;
					
					cf.pC = Math.random()*0.3 + 0.6;
					cf.pM = Math.random()*0.3 + 0.2;
					cf.maxPopulation = 2*((int) Math.floor((Math.random()*complexity + complexity)/2));
					cf.maxParents = 2*(int)(cf.maxPopulation/4);
					cf.maxTournamentContenders = 2*(int)(cf.maxPopulation/4);
					addPopulation();
					population.sort((p1,p2) -> p1.compareTo(p2));
					population = population.stream().limit(cf.maxPopulation).collect(Collectors.toList());
					updateBest();
					cf.SAthreshold = Math.random()*0.2 + 0.05;
					cf.SAcounterActivation = (int) Math.floor(Math.random()*20 + 20);
				}
				
				
				timer=System.currentTimeMillis() - timerStart;
				this.lastTimeSinceBest = System.currentTimeMillis() - this.lastTimeBest;
			}
		}
		catch(Exception e1) {
			e1.printStackTrace();
			try {
				if(this.globalBest == null) {
					this.globalBest = new Solution(data);
					this.globalBest.randomize();
					this.globalBest.fix();
				}
					
				ETPHandler.writeFinalSolution(cf.instanceName, this.globalBest.getMappedSol());
			}
			catch(Exception e2) {
				e2.printStackTrace();
			}
		}
		
		
		return this.globalBest;
	}


	private void fixPopulation() {
		for(Solution i: population) {
			if(!i.isFeasible()) {
				i.fix();
			}
		}
	}
	
	
	/**
	 * Removes all the solutions with the same rank 
	 * (2 solutions with the same rank have an high 
	 * 	probability to have the same structure)
	 * @param population population list
	 * @return modified population
	 */
	public List<Solution> deleteDuplicates(List<Solution> population){
		List<Solution> unique=new ArrayList<>();
		Solution temp;
		boolean flag_no_insert=false;
		
		for(Solution i1: population) {
			flag_no_insert=false;
			for(Solution i2: unique) {
				if(Math.abs(i1.getRank()-i2.getRank())<0.000001 && i1.getNumConflicts()==i2.getNumConflicts()) {
					flag_no_insert=true;
					break;
				}
			}
			if(!flag_no_insert) {
				unique.add(i1);
			} else {
				temp=new Solution(data);
				temp.randomize(); 
				unique.add(temp);
			}
		}
		
		return unique;
	}
	
	
	/**
	 * Initializes population by inserting maxPopulation Solutions generated randomly
	 * and initializes also the best Solution with the first feasible Solution found.
	 * 
	 * COST: O(maxPopulation*numExams) 
	 */
	private void initPopulation() { 
		Solution ind;

		for(int i=0; i < cf.maxPopulation; i++) {
			ind = new Solution(data);
			ind.randomize(); //creates random Solution AND computes rank and feasibility of him
			ind.fix(); //fix & compute fitness
			
			population.add(ind); 
		}
	}
	
	
	/**
	 * Inserts individuals into the 
	 */
	private void addPopulation() { 
		Solution ind;
		int initSize = this.population.size();
		
		for(int i=initSize; i < cf.maxPopulation; i++) {
			ind = new Solution(data);
			ind.randomize(); //creates random Solution AND computes rank and feasibility of him
			ind.fix();
			
			population.add(ind); 
			
		}
	}
	
	
	
	/**
	 * implements Tournament selection
	 * @param maxParents max number of parents
	 * @param numContenders contender's number
	 * @return list of parents selected to create new springs
	 */
	private List<Solution> parentSelection(int maxParents, int numContenders){
		List<Solution> parents = new ArrayList<>();
		List<Solution> contenders=new ArrayList<>();
		HashSet<Integer> alreadyTaken=new HashSet<>();
		Solution temp;
		int tempIndex;
		int i;
		
		for(int parentsCounter=0; parentsCounter < maxParents; parentsCounter+=2) {
			contenders.clear();
			alreadyTaken.clear();
			i=0;
			while(i < numContenders) {
				tempIndex=randomValue.nextInt(population.size());
				while(alreadyTaken.contains(tempIndex))
					tempIndex=randomValue.nextInt(population.size());
				
				alreadyTaken.add(tempIndex);
				
				temp=population.get(tempIndex);
				contenders.add(temp);
				i++;
			}
			
			//take best contender
			parents.add(contenders.stream()
					.max((c1,c2)->Double.compare(c1.getRank(),c2.getRank()))
					.get());
			
			//take worst contender
			parents.add(contenders.stream()
					.min((c1,c2)->Double.compare(c1.getRank(),c2.getRank()))
					.get());
		}
		
		return parents;
	}
	
	
	/**
	 * implements uniform crossover
	 * 		for each exam to assign into the new spring
	 * 			flip coin
	 * 			if(gene of parent1 is feasible and gene of parent2 is feasible) then
	 * 				if head
	 * 					take gene from parent1
	 * 				else
	 * 					take gene from parent2
	 * 			else
	 * 				take the feasible gene 
	 * 
	 * 
	 * @param parent1 first parent
	 * @param parent2 second parent
	 * @return new spring for the population
	 */
	private Solution crossover(Solution parent1, Solution parent2) {
		double p1=0.5; //probability to get gene from parent1
		double coin;
		int gene=randomValue.nextInt(data.getNumSlots());
		
		Solution i = new Solution(data);
		for(Exam e: parent1.getMappedSol().keySet()) {
			coin = randomValue.nextDouble(); //flip coin
			
			if(!parent1.getExamsClashing().containsKey(e)&&!parent2.getExamsClashing().containsKey(e)) { 
				if(coin<=p1) {
					gene=parent1.getSlot(e);
				} else {
					gene=parent2.getSlot(e);
				}
			} else if(parent1.getExamsClashing().containsKey(e) && !parent2.getExamsClashing().containsKey(e)) {
				gene=parent2.getSlot(e);
			} else if(!parent1.getExamsClashing().containsKey(e) && parent2.getExamsClashing().containsKey(e)) {
				gene=parent1.getSlot(e);
			}
				
			i.setExam(e, gene);
		}
		i.initializeProperties();
		return i;
	}
	
	
	
	private List<Solution> applyCrossover(List<Solution> parents){
		List<Solution> children=new ArrayList<>();
		int k;
		
		for(k=1; k<parents.size();k+=2) {
			//each couple of parents generates 2 children
			children.add(crossover(parents.get(k-1),parents.get(k))); //first child
			children.add(crossover(parents.get(k-1),parents.get(k))); //second child
		}
		
		return children;
	}
	
	
	/**
	 * Method used for the solution self-mutation
	 * @param parent the solution that is going to be mutated
	 * @return ind the self-mutated solution
	 */
	private Solution selfMutation(Solution parent) {
		Exam mutedGene;
		Solution ind = parent;
		
		parent.selfLearningRate(false);
		double mutPercentage = parent.selfMutation();
		int nMutations = (int)Math.floor(mutPercentage*data.getExamsList().size());

		if(nMutations==0) return parent;
		while(nMutations>0) {
			int new_t = randomValue.nextInt(data.getNumSlots());
			if(!parent.isFeasible()) {
				mutedGene = parent.getExamsClashing().keySet().stream().collect(Collectors.toList())
								.get(randomValue.nextInt(parent.getExamsClashing().size()));
			} else {
				mutedGene = parent.getMappedSol().keySet().stream().collect(Collectors.toList())
								.get(randomValue.nextInt(data.getExamsList().size()));
			}
			ind = new Solution(parent, mutedGene, parent.getSlot(mutedGene), new_t);
			ind.selfCopyRanks(parent);
			parent = ind;
			nMutations--;
		}
		ind.initializeProperties();
		ind.selfUpdateRanks();
		return ind;
	}
	
	
	private List<Solution> applySelfMutation(List<Solution> parents) {
		List<Solution> tempChildren=new ArrayList<>();
		
		for(Solution ind: parents) {
			tempChildren.add(selfMutation(ind));
		}
		
		return tempChildren;
	}
	
	
	/**
	 * Deletes n Solutions from population and adds n children to the population.
	 * kill criterion: kill n worst solutions
	 * @param children the list of children
	 * @param n number of solutions to kill from the population
	 */
	private void naturalSelection(List<Solution> children, int n) {
		population.sort((p1,p2) -> p1.compareTo(p2));
		population = population.stream().limit(population.size()-n).collect(Collectors.toList());
		children.forEach((c)->population.add(c));
	}
	
	/**
	 * Search for best solution
	 * the new king must be: FEASIBLE and HAVE BEST RANK SO FAR
	 */
	private void updateBest() {
		Solution tempInd=null;
		double temp=Double.POSITIVE_INFINITY;
		for(Solution i: population)
		{
			if(i.getRank() < temp && i.isFeasible())
			{
				temp=i.getRank();
				tempInd=i;
			}
		}
		
		if(this.generationBest != null) {
			if(!this.generationBest.isFeasible() || !this.population.contains(this.generationBest)) {
				this.generationBest = tempInd; 
			} else {//compare with best
				if(temp < generationBest.getRank()) 
					generationBest=tempInd; //saves the best among all generations (with requirement to be feasible)
			}
		} else {
			generationBest=tempInd;
		}
	
		
		//in order to avoid lose of best rank we create a COPY of gen best if gen best is better than global best
		if(this.generationBest != null && this.generationBest.isFeasible() ) {
			if( globalBest == null || (this.generationBest.getRank() < globalBest.getRank())) {
				if(globalBest == null)
					globalBest = new Solution(data);
		    
				globalBest.copySolutionFromMap(generationBest.getMappedSol());
				globalBest.initializeProperties();
				
				this.lastTimeBest = System.currentTimeMillis();
		   }
		}
	}
	
	private void localSearchSA(int iterationCounter, SimulatedAnnealing SA) {
		Solution fromSA;
		int counter = 0; 
		for(Solution i1 : population) {
			if(i1.isFeasible() && (counter < 6) ) {
				counter++; 
				fromSA=SA.runSAForGA(i1, cf.SATime);
				if(fromSA == null) {
					i1.setErasable(true);
				} else {
					i1.copySolution(fromSA);
				}
			}
		}
		
	}
	
	private void localSearchTS(int iterationCounter, Tabu TS, int sec) {

		Solution fromTS;

	    List<Solution> toBeDeleted = new ArrayList<>();
	    List<Solution> toBeAdded = new ArrayList<>();
	    
	    //send to the TS 3 individuals

	    int counter = 0; 
	    for(Solution i1 : population) {
	    	if(i1.isFeasible() && (counter < 3) ) {
	    		counter++; 
	    		fromTS = TS.run(i1, sec*1000);
	    		if(fromTS!=null) {
	    			toBeDeleted.add(i1);
	    			toBeAdded.add(fromTS);
	    		}
	    	}
	    }  
	    
	    for(int i=0; i<toBeDeleted.size();i++) {
	    	population.remove(toBeDeleted.get(i));
	    	population.add(toBeAdded.get(i));
	    }
	  
	    if(generationBest!=null) {
	    	fromTS = TS.run(generationBest, sec);
	    	if(fromTS!=null && fromTS.getRank() < generationBest.getRank() && fromTS.isFeasible()) {
	    		generationBest = fromTS;
	    	}
	    }

	}
	
	public String toString() {
		return population.stream().map(i -> i.toString()).collect(Collectors.joining("\n"));
	}
	
	private void changeEra() {
		this.TSEra = !this.TSEra;
		this.lastTimeBest = System.currentTimeMillis(); //clear this value for the new era
	}
}

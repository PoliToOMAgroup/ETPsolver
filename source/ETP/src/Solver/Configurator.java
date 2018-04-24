package Solver;

import ETP.ETPHandler;

public class Configurator{
	
	/*
	 * PARAMETERS TO BE CALIBRATED
	 */
	
	/**
	 * Class used for the general set up of the algorithm parameters
	 */
	
	public String instanceName = "";
	
	//Self Adaptation 
	public final boolean SelfAdaptOn 		= 	true;
	public final boolean SelfParametersOn   =   true;
	
	//Genetic Parameters
	public long maxTime; //given by the cmd
	public int maxPopulation				=	40;
	public int maxParents					=	20; //numero pari
	public int maxTournamentContenders		= 	5; //must be less than maxpopulation
	public double pC						= 	0.8; //probability to apply crossover
	public double pM						=	0.3; //probability to apply mutation

	//Hybrid parameters
	public double SAthreshold				=	0.2; 
	public int SAcounterActivation			= 	12;
	public final int SATime = 3000;//ms
	public final int TSTime = 20;//s
	
	//SA parameters
	public final double INITIAL_TEMPERATURE = 	100;
	public final double ALPHA 				= 	0.99; 
	public final int GAMMA 					= 	19; 
	public final double pELS 				= 	0.5; 
	public final double pESW 				= 	0.7; 
	public final double pEM 				= 	0.95;
	public final int iterationParameter 	= 	250; 
	
	//Tabu parameters
	public final Integer neighSize			=	100;
	public final Integer listSize			=	2*neighSize;
	public final Double p				    =	0.2; //penalty for unfeasible solution
	public final Double aspirationThreshold =   0.025;		
	
	public Configurator(ETPHandler data, int seconds) {
		this.maxTime = seconds*1000;
		if(this.SelfParametersOn) {
			this.SAcounterActivation = data.getNumSlots();
			double param = data.getGraph().getDensity()/data.getNumSlots();
			if(param>0.02) {
				this.maxPopulation = 100;
			}else if(param>0.01){
				this.maxPopulation = 40;
			}else {
				this.maxPopulation = 20;
			}
			this.maxParents = 2*(int)(this.maxPopulation/8);
			this.maxTournamentContenders = (int)(this.maxPopulation/8);
		}
	}
}

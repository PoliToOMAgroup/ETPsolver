package Solver;


import ETP.*;

public class Solver {
	private static ETPHandler data;
	
	public Solver(ETPHandler tt) {
		data = tt;
	}
	
	public Solution solve(int seconds, String instanceName) {
		Configurator co = new Configurator(data, seconds);
		co.instanceName = instanceName;
		Genetic G = new Genetic(data,co);
		Solution s = G.beginSelfEvolution();

		return s;
	}
	
}

package main;
import ETP.*;
import Solver.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.util.Scanner;

public class Main {

	public static void main(String[] args) throws FileNotFoundException {
		
		ETPHandler tt = new ETPHandler();
		String instanceName=args[0];
		int timeLim = Integer.valueOf(args[2]);	
		

		try {
			readSlo(instanceName+".slo", tt);
			readExm(instanceName+".exm", tt);
			readStu(instanceName+".stu", tt);
		} catch (FileNotFoundException e) {
			System.out.println("File not found");
			e.printStackTrace();
		}
		
		tt.initialize();
		
		Solver so = new Solver(tt);
		Solution s = so.solve(timeLim, instanceName);
		
		try {
			ETPHandler.writeFinalSolution(instanceName,s.getMappedSol());
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		System.out.println("Done!");
	}
	
	
	
	
	private static void readSlo(String fileName, ETPHandler etp) throws FileNotFoundException {
		
		
		File file = new File(fileName);
		Scanner sc = new Scanner(file);
		
		
		try {
			
		if(sc.hasNext()) {
			
			etp.setNumSlots(Integer.parseInt(sc.next()));
		}
		
		} catch(NumberFormatException e)  {
			System.out.println("The string does not cointains a parsable integer");
		}
		
		sc.close();
	}
	
	private static void readExm(String fileName, ETPHandler etp) throws FileNotFoundException {
		
		
		File file = new File(fileName);
		Scanner sc = new Scanner(file);
		
		while(sc.hasNext()) {
			try {
				etp.addExam(sc.next(), Integer.parseInt(sc.next()));	
			} catch (NumberFormatException e)  {
				System.out.println("The string does not cointains a parsable integer");
			}
		}
		
		sc.close();
	}
	
	private static void readStu(String fileName, ETPHandler etp) throws FileNotFoundException {
		
		File file = new File(fileName);
		Scanner sc = new Scanner(file);
		while(sc.hasNext()) {
			
			etp.addStudent(sc.next(), sc.next());	
			
		}
		
		sc.close();
	}
	
}

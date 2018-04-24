package ETP;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import Graph.*;

public class ETPHandler {
	
	
	private int numSlots;
	private Map<String, Exam> exams = new LinkedHashMap<>();
	private Map<String, Student> students = new LinkedHashMap<>();
	private Graph confGraph;
	
	public ETPHandler() {
		//void constructor
		this.confGraph = new Graph();
	}
	
	public void initialize() {
		this.createGraph(students, exams);
		this.computeConflictingGrades();
		
		List<Edge> temp;
		List<Exam> tempconf;
		
		//each exam has a list of his conflicting exams
		for(Exam e: exams.values()) {
			temp=confGraph.getNodeEdges(e.getId());
			tempconf=new ArrayList<>();
			for(Edge a:temp) {
				if(a.fromNode().getVertex().getId() != e.getId()) {
					tempconf.add(a.fromNode().getVertex());
				} else {
					tempconf.add(a.toNode().getVertex());
				}
			}
			e.setConflictingExams(tempconf);
		}
	}
	
	
//***********************
//******GETTERS**********
//***********************
	public Graph getGraph() {
		return this.confGraph;
	}
	
	public int getNumSlots() {
		return numSlots;
	}
	
	public Map<String, Student> getStudents(){
		return this.students;
	}
	
	public Map<String, Exam> getExamsMap(){
		return this.exams;
	}
	
	public Exam getExam(String examName) {
		return exams.get(examName);
	}

	public List<Exam> getConflictingExams(Exam e){
		return e.getConflictingExams();
	}
	
	public List<Edge> getConflictsList(){
		return confGraph.getAllEdges();
	}
	
	public List<Exam> getExamsList(){
		return exams.values().stream().collect(Collectors.toList());
	}
	
	public static int getPenalty(int distance) {
		switch(distance) {
		case 1:
			return 16;
		case 2:
			return 8;
		case 3:
			return 4;
		case 4:
			return 2;
		case 5:
			return 1;
		default:
			return 0;
		}
	}


	public void setNumSlots(int numSlots) {
		this.numSlots = numSlots;
	}
	
	public void addExam(String examId, int numEnrolled) {
		
		Exam e = new Exam(examId, numEnrolled);
		this.exams.put(examId, e);
	
	}
	
	public void addStudent(String studentId, String examId) {
		Student s;
		Exam e;
		
		if(!students.containsKey(studentId)) {
			s = new Student(studentId);
			this.students.put(studentId, s);
		}
		else
			s = students.get(studentId);
		
		//insertion in the Student class
		e = exams.get(examId);
		s.insertExam(e);
		
		//insertion in the Exam class
		e.enroll(s);
		
	}
		


	public void computeConflictingGrades() {
		int degree=0;
		for(Exam e: exams.values()) {
			degree=e.getConflictingExams().size();
			e.cg=degree;
		}
	}
	
	/**
	 * This method creates the conflict graph used for our feasibility check
	 * @param students the list of students encoded as a map
	 * @param exams the list of exams encoded as a map
	 */
	public void createGraph(Map<String, Student> students, Map<String, Exam> exams ) {		
		for(Exam e: exams.values()) {
			confGraph.addVertex(e);
		}
		
		for(Student s: students.values()) {
			List<Exam> studentExams = s.getExamList();
			for(int i = 0; i < studentExams.size(); i++) {    
				for (int j = i+1; j < studentExams.size(); j++ ) {
					confGraph.addEdge(studentExams.get(i), studentExams.get(j)); 
				} 
			}
		}
	}
	
	public static void writeFinalSolution(String instanceName, Map<Exam,Integer> mapped_solution) throws FileNotFoundException, UnsupportedEncodingException {
		PrintWriter writer = new PrintWriter(instanceName +"_OMAMZ_group05.sol", "UTF-8");
		int t;
		
		String exam_id;
			
		for(Exam e: mapped_solution.keySet()) {
			t=mapped_solution.get(e)+1;
			exam_id=e.getId();
			writer.println(exam_id+" "+t);
		}
		
		writer.close();
	}
}
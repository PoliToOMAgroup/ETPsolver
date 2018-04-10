package ETP;
import java.util.ArrayList;
import java.util.List;

public class Student {
	
	private String id;
	private List<Exam> exams = new ArrayList<>();
	
	public Student(String id) {
		this.setId(id);
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	public String getId() {
		return this.id;
	}
	public void insertExam(Exam exam) {
		this.exams.add(exam);
	}
	
	public List<Exam> getExamList() {
		return exams; 
	}

}

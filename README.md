# ETPsolver
An Hybrid Tandem approach to solve examination timetabling problem. 

## Authors:
          Pietro Barbiero,
          Manuel Scurti,
          Matteo Senese,
          Chiara Sopegno,
          Antonio Tavera
          
        
## How to run :
    
    $ ETPsolver_OMAMZ_group05.exe instancename -t timelimit
 
## Input files :
      
      -instancename.exm : defines the total number of students enrolled per exam
                          Format: 1 line per exam, each line has the format
                                      INT1  INT2
                          Where INT1 is the exam ID and INT2 is the # of enrolled students in INT1
      
      
      -instancename.slo : defines the length of the examination period      
                          Format: a single value in the format
                                      INT
                          where INT is the number of available time-slots
                          
                          
      -instancename.stu :   defines the exams in which each student is enrolled
                          Format: 1 line for each enrollment. Each line has the format
                                      sINT1  INT2
                          where INT1 is the student ID and INT2 is the ID of the exam in which student
                          INT1 is enrolled

## Output file: 
	a text file named instancename OMAXX groupYY.sol containing, for each exam, the assigned time-slot
	format: 1 line per exam. Each line has the format
	INT1 INT2
	where INT1 is the exam ID and INT2 is the ID of the assigned time-slot
	(IDs must correspond to those read in the instance files)
	
	it is guaranteed that the solution provided is feasible.
      

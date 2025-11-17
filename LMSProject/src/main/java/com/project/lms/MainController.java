package com.project.lms;

import java.security.Principal;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.project.lms.apply.ApplyService;
import com.project.lms.attendance.AttendanceService;
import com.project.lms.course.Course;
import com.project.lms.member.Member;
import com.project.lms.member.MemberService;

import lombok.RequiredArgsConstructor; 


@RequiredArgsConstructor 
@Controller
public class MainController {
    private final MemberService memberService; 
	private final ApplyService applyService;
	private final AttendanceService attendanceService;
	
	@GetMapping("/")
	public String dashboard(Model model, Principal principal) {	    
	    if (principal != null) {
	        // 로그인 된 경우: 회원 정보 및 수강 강좌 리스트 조회
	        String memberId = principal.getName();
	        Member member = memberService.getMember(memberId);
	        
	        // 해당 사용자의 수강 신청된 강좌 리스트 조회
	        List<Course> enrolledCourses = applyService.getEnrolledCourses(member); 

	        // Model에 저장 (HTML의 ${enrolledCourses}로 전달됨)
	        model.addAttribute("enrolledCourses", enrolledCourses); 
            // 필요한 경우 member 객체도 추가
            model.addAttribute("member", member);
            
            //메인 페이지 출석일 출력
            long attendanceCount = attendanceService.getAttendanceCount(member);            
            model.addAttribute("attendanceCount", attendanceCount); 

	    }
	    
	    // 비회원 또는 회원 모두 동일한 뷰 반환 (단, 회원에게는 추가 데이터가 전달됨)
	    return "index"; 
	}
}
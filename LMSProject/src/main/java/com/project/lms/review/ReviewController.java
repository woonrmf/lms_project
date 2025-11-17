package com.project.lms.review;

import java.security.Principal;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import com.project.lms.apply.ApplyService;
import com.project.lms.course.Course;
import com.project.lms.course.CourseService;
import com.project.lms.lesson.Lesson;
import com.project.lms.lesson.LessonService;
import com.project.lms.member.Member;
import com.project.lms.member.MemberService;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/review")
public class ReviewController {

	private final ReviewService reviewService;
	private final CourseService courseService;
	private final LessonService lessonService;
	private final MemberService memberService;
	private final ApplyService applyService;

	@GetMapping("/list")
	public String list(Model model, @RequestParam(value="page", defaultValue = "0") int page) {
		Page<Review> paging = reviewService.getList(page);
		model.addAttribute("paging", paging);
		return "review/reviewListTest";
	}
	
	@GetMapping("/detail/{reviewno}")
	public String detail(Model model, @PathVariable("reviewno") Integer reviewno) {
		Review review = reviewService.getReview(reviewno);
		model.addAttribute("review", review);
		 Course course;
		    if (review.getLesson() != null) {
		        course = review.getLesson().getCourse();
		    } else {
		        course = review.getCourse(); // review 자체에 course 필드가 있음
		    }
		
	    model.addAttribute("course", course);
		return "review/reviewDetailTest";
	}
	
	//등록폼 
	@GetMapping("/create/{courseno}")
	public String reviewCreate(ReviewForm reviewForm, Model model, Principal principal, 
			@PathVariable("courseno") Integer courseno) { //강좌에 속한 강의를 불러오기 위해 courseno 추가
		
		// 로그인한 사용자
		Member member = memberService.getMember(principal.getName());
		Course course = courseService.getCourse(courseno); //강좌no 불러오기
		
		// 수강신청 여부 확인
	    boolean isEnrolled = applyService.isEnrolled(member, course);
	    if (!isEnrolled) {
	        model.addAttribute("errorMessage", "수강신청하지 않은 강좌에는 리뷰를 작성할 수 없습니다.");
	        return "redirect:/course/detail/" + courseno;
	    }
	    
		List<Lesson> lessonList = lessonService.getLessonByCourse(course); //강좌 속 강의 리스트 가져오기
		model.addAttribute("course", course);
		model.addAttribute("lessonList", lessonList);
		return "review/reviewCreateTest";
	}
	
	//등록
	@PostMapping("/create/{courseno}")
	public String reviewCreate(@Valid ReviewForm reviewForm, BindingResult bindingResult, 
			Principal principal, Model model, @PathVariable("courseno") Integer courseno) {
		
		if(bindingResult.hasErrors()) {
			Course course = courseService.getCourse(reviewForm.getCourseno());
			model.addAttribute("course", course);
			model.addAttribute("lessonList", lessonService.getLessonByCourse(course));
			return "review/reviewCreateTest";
		}
		
		Member member = memberService.getMember(principal.getName());
		Course course = courseService.getCourse(courseno);
		//Lesson lesson = lessonService.getLesson(questionForm.getLessonno()); 강의 선택 필수 일때 이 코드 사용
		
		// 수강 여부 다시 한 번 확인 (보안상)
	    boolean isEnrolled = applyService.isEnrolled(member, course);
	    if (!isEnrolled) {
	        model.addAttribute("errorMessage", "수강신청하지 않은 강좌에는 리뷰를 작성할 수 없습니다.");
	        return "redirect:/course/detail/" + courseno;
	    }
	    
		//강의 선택 안해도 질문 가능하게 하기
		Lesson lesson = null;
		if(reviewForm.getLessonno() != null) {
			lesson = lessonService.getLesson(reviewForm.getLessonno());
		}
			
		reviewService.create(reviewForm.getContent(), member, lesson, course, reviewForm.getRating());
			
		if(lesson != null) {
			Integer courseNo = lesson.getCourse().getCourseno(); //등록 후 courseno 디테일로 보내기
			return "redirect:/course/detail/"+courseNo;
		} else {
			return "redirect:/course/detail/"+reviewForm.getCourseno(); 
		}
	}
	
	//수정
	@GetMapping("/modify/{reviewno}")
	public String reviewModify(ReviewForm reviewForm, @PathVariable("reviewno") Integer reviewno, Principal principal) {
		Review review = reviewService.getReview(reviewno);
			
		String loginMember = principal.getName();
		Member member = memberService.getMember(loginMember);
		boolean isAdmin = member.getRole().name().equals("ADMIN");
		boolean isMember = review.getMember().getName().equals(loginMember);
			
		if(!isAdmin && !isMember) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "수정 권한이 없습니다.");
		}
		reviewForm.setRating(review.getRating());
		reviewForm.setContent(review.getContent());
		return "review/reviewModifyTest";
	}
		
	@PostMapping("/modify/{reviewno}")
	public String reviewModify(@Valid ReviewForm reviewForm, BindingResult bindingResult, Principal principal, @PathVariable("reviewno") Integer reviewno) {
		if(bindingResult.hasErrors()) {
			return "reviewModifyTest";
		}
		Review review = reviewService.getReview(reviewno);
			
		String loginMember = principal.getName();
		Member member = memberService.getMember(loginMember);
		boolean isAdmin = member.getRole().name().equals("ADMIN");
		boolean isMember = review.getMember().getName().equals(loginMember);
			
		if(!isAdmin && !isMember) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "수정 권한이 없습니다.");
		}
		reviewService.modify(review, reviewForm.getRating(), reviewForm.getContent());
		return String.format("redirect:/review/detail/%s", reviewno);
	}
		
	//삭제
	@PostMapping("/delete/{reviewno}")
	public String delete(Principal principal, @PathVariable("reviewno") Integer reviewno) {
		Review review = reviewService.getReview(reviewno);
			
		String loginMember = principal.getName();
		Member member = memberService.getMember(loginMember);
		boolean isAdmin = member.getRole().name().equals("ADMIN");
		boolean isMember = review.getMember().getName().equals(loginMember);
			
		if(!isAdmin && !isMember) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "삭제 권한이 없습니다.");
		}
		reviewService.delete(review);
		
		 Integer courseno;
		    if (review.getLesson() != null) {
		        courseno = review.getLesson().getCourse().getCourseno();
		    } else {
		        courseno = review.getCourse().getCourseno();
		    }
			
		return "redirect:/course/detail/"+courseno;
	}
}

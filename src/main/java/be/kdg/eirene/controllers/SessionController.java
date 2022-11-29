package be.kdg.eirene.controllers;

import be.kdg.eirene.service.CookieService;
import be.kdg.eirene.service.SessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpSession;

@Controller
@RequestMapping ("/profile/sessions")
public class SessionController {
	private final Logger logger;
	private final SessionService sessionService;
	private final CookieService cookieService;

	@Autowired
	public SessionController(SessionService sessionService, CookieService cookieService) {
		this.logger = LoggerFactory.getLogger(this.getClass());
		this.sessionService = sessionService;
		this.cookieService = cookieService;
	}

	@GetMapping
	public ModelAndView loadSessions(HttpSession session) {
		if (cookieService.cookieInvalid(session)) {
			return new ModelAndView("redirect:/");
		}
		return new ModelAndView("sessions")
				.addObject("sessions", sessionService.getSessions(cookieService.getAttribute(session)));
	}

	@GetMapping ("session-overview/{sessionID}")
	public ModelAndView showSessionOverview(HttpSession httpSession, @PathVariable Long sessionID) {
		if (cookieService.cookieInvalid(httpSession)) {
			return new ModelAndView("redirect:/");
		}
		return new ModelAndView("session-overview")
				.addObject("eireneSession", sessionService.getSession(sessionID, cookieService.getAttribute(httpSession)));
	}
}

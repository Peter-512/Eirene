package be.kdg.eirene.controllers;

import be.kdg.eirene.model.*;
import be.kdg.eirene.presenter.viewmodel.SessionFeedbackViewModel;
import be.kdg.eirene.service.CookieService;
import be.kdg.eirene.service.SessionService;
import be.kdg.eirene.service.UserService;
import be.kdg.eirene.service.evaluator.ReportGeneratorService;
import be.kdg.eirene.util.ReadingValidator;
import be.kdg.eirene.util.RequestDecryptor;
import com.github.javafaker.Faker;
import com.github.kevinsawicki.http.HttpRequest;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.sql.Timestamp;

@Profile ("dev")
@RestController
@RequestMapping ("/newsession")
public class ActiveSessionMockController {

	private final ReportGeneratorService reportGenerator;
	private final ReadingValidator validator;
	private final CookieService cookieService;
	private final SessionService sessionService;
	private final UserService userService;
	private final Logger logger;
	private final RequestDecryptor decryptor;
	private final Faker faker = new Faker();
	private Session session;

	@Autowired
	public ActiveSessionMockController(RequestDecryptor decryptor, CookieService cookieService, UserService userService, SessionService sessionService, ReportGeneratorService reportGenerator, ReadingValidator validator) {
		this.decryptor = decryptor;
		this.cookieService = cookieService;
		this.userService = userService;
		this.sessionService = sessionService;
		this.reportGenerator = reportGenerator;
		this.validator = validator;
		logger = LoggerFactory.getLogger(this.getClass());
	}

	@GetMapping
	public ModelAndView showNewSession(@RequestParam SessionType type, HttpSession httpSession) {
		if (cookieService.cookieInvalid(httpSession)) {
			return new ModelAndView("redirect:/");
		}
		User user = userService.getUser(cookieService.getAttribute(httpSession));
		session = new Session(type, user);
		logger.info(" report: " + reportGenerator.formulateReport(session.getReadings(), type));
		return new ModelAndView("active-session").addObject("type", type.getCapitalizedName())
		                                         .addObject("session", session)
		                                         .addObject("report", reportGenerator.formulateReport(session.getReadings(), type));
	}

	@GetMapping ("/api")
	public String getChartData() {
		Reading currentReading = new Reading(
				new Timestamp(System.currentTimeMillis()),
				new BrainWaveReading(
						faker.number().numberBetween(0, 200),
						faker.number().numberBetween(0, 100),
						faker.number().numberBetween(0, 100)),
				new SensorData(
						faker.number().numberBetween(40, 180),
						faker.number().numberBetween(18, 25),
						faker.number().numberBetween(52, 53),
						faker.number().numberBetween(30, 60),
						faker.number().numberBetween(0, 1)));

		Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").setPrettyPrinting().create();

		final String readingJSON = gson.toJson(currentReading);
		HttpRequest request = HttpRequest.post("http://localhost:8081/newsession")
		                                 .contentType("application/json")
		                                 .send(readingJSON);
		logger.info(String.valueOf(request.code()));

		EvaluatedData data = reportGenerator.formulateReport(session.getReadings(), session.getType());
		return gson.toJson(data);
	}

	@PostMapping
	public void getData(@RequestBody Reading data) {
		if (session != null && validator.validate(data)) {
			session.addReading(data);
			logger.info("Added reading: " + data);
		}
	}


	@GetMapping ("/stopsession")
	public ModelAndView stopSession() {
		if (session == null) {
			return new ModelAndView("redirect:/profile");
		}
		session.stop();
		return new ModelAndView("feedback").addObject("sessionFeedback", new SessionFeedbackViewModel());
	}

	@PostMapping ("/submit-feedback")
	public ModelAndView submit(@ModelAttribute ("sessionFeedback") @Valid SessionFeedbackViewModel sessionFeedbackViewModel, BindingResult errors, HttpSession httpSession) {
		if (cookieService.cookieInvalid(httpSession)) {
			return new ModelAndView("redirect:/");
		}
		if (errors.hasErrors()) {
			return new ModelAndView("feedback");
		}
		logger.info("request radio " + sessionFeedbackViewModel.getSatisfactionLevel());
		logger.info(sessionFeedbackViewModel.getSessionName());
		session.setSatisfaction(sessionFeedbackViewModel.getSatisfactionLevel().getValue());
		session.setName(sessionFeedbackViewModel.getSessionName());
		sessionService.save(session);
		Long sessionId = session.getId();
		return new ModelAndView("redirect:/profile/sessions/session-overview/" + sessionId);
	}

	@DeleteMapping ("/discard-session")
	public ModelAndView discardSession(HttpSession httpSession) {
		if (cookieService.cookieInvalid(httpSession)) {
			return new ModelAndView("redirect:/");
		}
		return new ModelAndView("redirect:/profile/sessions/1");
	}
}

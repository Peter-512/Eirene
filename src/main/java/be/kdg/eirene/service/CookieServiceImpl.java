package be.kdg.eirene.service;

import lombok.Getter;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;

@Service
public class CookieServiceImpl implements CookieService {

	@Getter
	public final String COOKIE_NAME = "user_id";
	public final String SESSION_ACTIVE = "isActive";

	@Override
	public boolean cookieInvalid(HttpSession session) {
		return session.getAttribute(COOKIE_NAME) == null;
	}

	@Override
	public boolean cookieInvalid(HttpSession session, Long userId) {
		return cookieInvalid(session) || !session.getAttribute(COOKIE_NAME).equals(userId);
	}

	@Override
	public void setCookie(HttpSession session, Long userId) {
		session.setAttribute(COOKIE_NAME, userId);
	}

	@Override
	public Long getAttribute(HttpSession session) {
		return session.getAttribute(COOKIE_NAME) == null ? null : (Long) session.getAttribute(COOKIE_NAME);
	}

	@Override
	public void setSessionCookie(HttpSession session, Boolean isActiveSession) {
		session.setAttribute(SESSION_ACTIVE, isActiveSession);
	}

	@Override
	public Boolean isSessionActive(HttpSession session) {
		return session.getAttribute(SESSION_ACTIVE) == null ? null : (Boolean) session.getAttribute(SESSION_ACTIVE);
	}
}
package idv.tia201.g1.authentication.filter;

import idv.tia201.g1.authentication.service.TokenService;
import idv.tia201.g1.authentication.service.UserAuth;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.HandlerInterceptor;

import static idv.tia201.g1.utils.Constants.ROLE_USER;

public class UserLoginInterceptor implements HandlerInterceptor {
    @Autowired
    private TokenService tokenService;
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String token = authorizationHeader.substring(7);

            UserAuth userAuth = tokenService.validateToken(token);
            if (userAuth != null && ROLE_USER.equals(userAuth.getRole())) {
                return true;
            }
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        return false;
    }
}

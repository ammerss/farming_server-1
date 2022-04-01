package tp.farming_springboot.config.jwt;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import io.jsonwebtoken.ExpiredJwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import tp.farming_springboot.api.ResultCode;
import java.util.HashMap;

@RequiredArgsConstructor
@Component
@Slf4j
public class AuthTokenFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final JwtAuthEntryPoint jwtAuthEntryPoint;

    private static final List<String> EXCLUDE_URL =
            Collections.unmodifiableList(
                    Arrays.asList(
                            "/auth/request-otp", //인증번호 받을때
                            "/auth/validate", //인증번호 입력할때
                            "/user/sudo", //임시
                            "/auth/tokens", //임시
                            "/init" //디비 초기화용
                    ));


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws IOException {
        try {
            HashMap<String, String> map = parseJwt(request);
            if(map.containsKey("access")){
                String jwt = map.get("access");
                jwtUtils.validateJwtToken(jwt);
                String username = jwtUtils.getUserNameFromJwtToken(jwt);
                jwtUtils.createAuthentication(username);
            }
            else if(map.containsKey("refresh")){
                String jwt = map.get("refresh");
                jwtUtils.validateJwtRefresh(jwt);
                String username = jwtUtils.getUserNameFromJwtRefreshToken(jwt);
                jwtUtils.createAuthentication(username);
            }
            filterChain.doFilter(request, response);
        }
        catch(BadCredentialsException e) {
            request.setAttribute("exception", ResultCode.TOKEN_NOT_VALID);
            SecurityContextHolder.clearContext();
            jwtAuthEntryPoint.commence(request, response, e);

        }
        catch (ExpiredJwtException e) {
            request.setAttribute("exception", ResultCode.TOKEN_EXPIRED);
            SecurityContextHolder.clearContext();
            jwtAuthEntryPoint.commence(request, response, new BadCredentialsException("토큰 유효시간이 만료되었습니다."));

        } catch (Exception e) {
            request.setAttribute("exception", ResultCode.INTERNAL_SERVER_ERROR);
            SecurityContextHolder.clearContext();
            jwtAuthEntryPoint.commence(request, response, new BadCredentialsException("토큰 검증 중 알 수 없는 에러가 발생했습니다."));
        }

    }

    private void allowForRefreshToken(ExpiredJwtException ex, HttpServletRequest request) {
        System.out.println("allowing for refresh tokens");

        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(
                null, null, null);
        SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
        request.setAttribute("claims", ex.getClaims());

    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return EXCLUDE_URL.stream().anyMatch(exclude -> exclude.equalsIgnoreCase(request.getServletPath()));
    }

    private HashMap<String, String> parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        HashMap<String, String> map = new HashMap<>();
        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            map.put("access",headerAuth.substring(7));
        }
        else if(StringUtils.hasText(headerAuth) && headerAuth.startsWith("Refresh ")){
            map.put("refresh",headerAuth.substring(8));
        }
        else {
            throw new BadCredentialsException("토큰 정보가 헤더에 없습니다.");
        }
        return map;
    }

}
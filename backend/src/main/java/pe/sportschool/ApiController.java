package pe.sportschool;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.security.Principal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ApiController {
    private final BookingService service;
    private final AuthenticationManager auth;
    private final HttpSessionSecurityContextRepository contexts;
    public ApiController(BookingService service, AuthenticationManager auth, HttpSessionSecurityContextRepository contexts) { this.service=service; this.auth=auth; this.contexts=contexts; }
    public record Login(@NotBlank String username,@NotBlank String password) {}
    public record NewRequest(@NotBlank @Size(max=120) String name,@NotBlank @Size(max=20) String phone,@Email @Size(max=180) String email,@Positive long spaceId,@NotNull LocalDate date,@Min(0) @Max(23) int startHour,@Min(1) @Max(24) int endHour) {}
    public record UpdateRequest(@Positive long spaceId,@NotNull LocalDate date,@Min(0) @Max(23) int startHour,@Min(1) @Max(24) int endHour,@NotBlank String status,@DecimalMin("0") @DecimalMax("99999999") BigDecimal agreedPrice,@Size(max=500) String reason,@NotNull @Size(max=2000) String notes) {}
    public record ConfirmRequest(@NotNull @DecimalMin("0") BigDecimal amount,@NotBlank String method,@Size(max=120) String reference) {}
    @GetMapping("/csrf") Object csrf(CsrfToken token) { return Map.of("token",token.getToken(),"header",token.getHeaderName()); }
    @PostMapping("/login") Object login(@Valid @RequestBody Login login,HttpServletRequest request,HttpServletResponse response) {
        var authenticated=auth.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(login.username(),login.password()));
        if(request.getSession(false)!=null) request.changeSessionId();
        var context=SecurityContextHolder.createEmptyContext(); context.setAuthentication(authenticated); SecurityContextHolder.setContext(context); contexts.saveContext(context,request,response);
        return service.user(login.username());
    }
    @GetMapping("/me") Object me(Principal principal) { return Map.of("user",service.user(principal.getName()),"sites",service.sitesFor(principal.getName())); }
    @GetMapping("/public/catalog") Object catalog() { return service.catalog(); }
    @GetMapping("/public/availability") Object availability(@RequestParam long siteId,@RequestParam LocalDate date,@RequestParam(required=false) Integer modality) { return service.availability(siteId,date,modality); }
    @PostMapping("/public/requests") Object create(@Valid @RequestBody NewRequest form) { return service.create(form,null); }
    @GetMapping("/requests") Object requests(Principal principal) { return service.requests(principal.getName()); }
    @PostMapping("/requests") Object manual(@Valid @RequestBody NewRequest form,Principal principal) { return service.create(form,principal.getName()); }
    @GetMapping("/requests/{id}") Object detail(@PathVariable long id,Principal principal) { return service.detail(id,principal.getName()); }
    @PutMapping("/requests/{id}") Object update(@PathVariable long id,@Valid @RequestBody UpdateRequest form,Principal principal) { return service.update(id,form,principal.getName()); }
    @PostMapping("/requests/{id}/confirm") Object confirm(@PathVariable long id,@Valid @RequestBody ConfirmRequest form,Principal principal) { return service.confirm(id,form,principal.getName()); }
}

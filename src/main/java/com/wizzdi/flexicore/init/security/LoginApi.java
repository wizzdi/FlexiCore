package com.wizzdi.flexicore.init.security;

import com.flexicore.model.SecurityUser;
import com.flexicore.model.User;
import com.wizzdi.flexicore.init.response.LoginResponse;
import com.wizzdi.flexicore.security.service.SecurityUserService;
import com.wizzdi.security.adapter.FlexicoreUserDetails;
import com.wizzdi.security.bearer.jwt.FlexicoreJwtTokenUtil;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "LoginApi")
@RestController
public class LoginApi {

  private final AuthenticationManager authenticationManager;
  private final FlexicoreJwtTokenUtil flexicoreJwtTokenUtil;
  private final SecurityUserService securityUserService;

  public LoginApi(
          AuthenticationManager authenticationManager, FlexicoreJwtTokenUtil flexicoreJwtTokenUtil, SecurityUserService securityUserService) {
    this.authenticationManager = authenticationManager;
    this.flexicoreJwtTokenUtil = flexicoreJwtTokenUtil;
      this.securityUserService = securityUserService;
  }

  @SecurityRequirements
  @PostMapping("login")
  public ResponseEntity<LoginResponse> login(@RequestBody @Valid LoginRequest loginRequest) {
    try {
      Authentication authenticate =
          authenticationManager.authenticate(
              new UsernamePasswordAuthenticationToken(
                  loginRequest.getUsername(), loginRequest.getPassword()));
      FlexicoreUserDetails flexicoreUserDetails =
          (FlexicoreUserDetails) authenticate.getPrincipal();
      SecurityUser securityUser = securityUserService.findByIdOrNull(SecurityUser.class, flexicoreUserDetails.getId());
      LoginResponse response = securityUser instanceof User user ? LoginResponse.ofUser(user) : new LoginResponse(securityUser.getId(), flexicoreUserDetails.getUsername(), securityUser.getName(), null);
      return ResponseEntity.ok()
          .header(
              HttpHeaders.AUTHORIZATION,
              flexicoreJwtTokenUtil.generateAccessToken(flexicoreUserDetails))
          .body(response);
    } catch (BadCredentialsException ex) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
  }
}
